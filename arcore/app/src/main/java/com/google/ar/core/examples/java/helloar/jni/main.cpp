#include "com_google_ar_core_examples_java_helloar_Process.h"
#include <opencv2/opencv.hpp>
#include <jni.h>

#include "Hungarian.h"
#include "KalmanTracker.h"

using namespace cv;

typedef struct TrackingBox
{
    int frame;
    int id;
    Rect_<float> box;
} TrackingBox;

double GetIOU(Rect_<float> bb_test, Rect_<float> bb_gt)
{
    float in = (bb_test & bb_gt).area();
    float un = bb_test.area() + bb_gt.area() - in;

    if (un < DBL_EPSILON)
        return 0;

    return (double) (in / un);
}

vector<KalmanTracker> trackers;
vector<Rect_<float>> predictedBoxes;
vector<vector<double>> iouMatrix;
vector<int> assignment;
vector<cv::Point> matchedPairs;
vector<TrackingBox> frameTrackingResult;
vector<TrackingBox> detFrameData;

set<int> unmatchedDetections;
set<int> unmatchedTrajectories;
set<int> allItems;
set<int> matchedItems;

int frame_count = 0;
int max_age = 1;
int min_hits = 3;
int trkNum = 0;
int detNum = 0;
double iouThreshold = 0.3;

extern "C"
JNIEXPORT void JNICALL
Java_com_google_ar_core_examples_java_helloar_Process_initializeData(JNIEnv *env, jclass clazz) {
    detFrameData.clear();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_google_ar_core_examples_java_helloar_Process_setData(JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat width, jfloat height) {
    Rect_<float> tmpRect(x, y, width, height);
    TrackingBox input;
    input.box = tmpRect;
    input.id = 0;
    input.frame = 0;
    detFrameData.push_back(input);
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_google_ar_core_examples_java_helloar_Process_getData(JNIEnv *env, jclass clazz, jint index) {
    if (index >= frameTrackingResult.size())
        return NULL;

    TrackingBox trbox = frameTrackingResult[index];
    jfloatArray res;
    res = env->NewFloatArray(6);
    if (res == NULL) return NULL;

    jfloat arr[6];
    arr[0] = trbox.box.x;
    arr[1] = trbox.box.y;
    arr[2] = trbox.box.width;
    arr[3] = trbox.box.height;
    arr[4] = (float) trbox.id;
    arr[5] = (float) trbox.frame;

    env->SetFloatArrayRegion(res, 0, 6, arr);
    return res;
}

extern "C" {
    JNIEXPORT int JNICALL Java_com_google_ar_core_examples_java_helloar_Process_Sort(JNIEnv *env, jclass clazz) {
        // count frame
        frame_count++;

        // initialization when met first frame
        if(trackers.size() == 0) {
            for(int i = 0; i < detFrameData.size(); ++i) {
                KalmanTracker trk = KalmanTracker(detFrameData[i].box);
                trackers.push_back(trk);
            }
            return 1;
        }

        // get predicted locations from existing trackers
        predictedBoxes.clear();
        for (auto it = trackers.begin(); it != trackers.end();) {
            Rect_<float> pBox = (*it).predict();
            if (pBox.x >= 0 && pBox.y >= 0) {
                predictedBoxes.push_back(pBox);
                it++;
            } else {
                it = trackers.erase(it);
            }
        }

        // associate detections to tracked object
        trkNum = predictedBoxes.size(); // predicted boxes
        detNum = detFrameData.size(); // current frame

        // trkNum * detNum matrix. Saving IOU scores. High score means high probability
        iouMatrix.clear();
        iouMatrix.resize(trkNum, vector<double>(detNum, 0));

        for (int i = 0; i < trkNum; ++i) { // compute iou matrix as a distance matrix
            for (int j = 0; j < detNum; ++j) {
                // use 1-iou because the hungarian algorithm computes a minimum-cost assignment.
                iouMatrix[i][j] = 1 - GetIOU(predictedBoxes[i], detFrameData[j].box);
            }
        }

        // solve the assignment problem using hungarian algorithm.
        // the resulting assignment is [track(prediction) : detection], with len=preNum
        HungarianAlgorithm HungAlgo;
        assignment.clear();
        HungAlgo.Solve(iouMatrix, assignment);

        // find matches, unmatched_detections and unmatched_predictions
        unmatchedTrajectories.clear();
        unmatchedDetections.clear();
        allItems.clear();
        matchedItems.clear();

        if (detNum > trkNum) { // there are unmatched detections
            for (int n = 0; n < detNum; ++n)
                allItems.insert(n);

            for (int i = 0; i < trkNum; ++i)
                matchedItems.insert(assignment[i]);

            set_difference(allItems.begin(), allItems.end(),
                           matchedItems.begin(), matchedItems.end(),
                           insert_iterator<set<int>>(unmatchedDetections, unmatchedDetections.begin()));
        } else if (detNum < trkNum) { // there are unmatched trajectory/predictions
            for (int i = 0; i < trkNum; ++i)
                if (assignment[i] == -1) // unassigned label will be set as -1 in the assignment algorithm
                    unmatchedTrajectories.insert(i);
        }

        // filter out matched with low IOU
        matchedPairs.clear();
        for (int i = 0; i < trkNum; ++i) {
            if (assignment[i] == -1) // pass over invalid values
                continue;
            if (1 - iouMatrix[i][assignment[i]] < iouThreshold) {
                unmatchedTrajectories.insert(i);
                unmatchedDetections.insert(assignment[i]);
            } else {
                matchedPairs.push_back(cv::Point(i, assignment[i]));
            }
        }

        // update matched trackers with assigned detections.
        // each prediction is corresponding to a tracker
        int trkIdx, detIdx;
        for (int i = 0; i < matchedPairs.size(); ++i) {
            trkIdx = matchedPairs[i].x;
            detIdx = matchedPairs[i].y;
            trackers[trkIdx].update(detFrameData[detIdx].box);
        }
        // create and initialise new trackers for unmatched detections
        for (auto umd : unmatchedDetections) {
            KalmanTracker tracker = KalmanTracker(detFrameData[umd].box);
            trackers.push_back(tracker);
        }

        // get trackers' output
        frameTrackingResult.clear();
        for (auto it = trackers.begin(); it != trackers.end();)
        {
            if (((*it).m_time_since_update < 1) && ((*it).m_hit_streak >= min_hits || frame_count <= min_hits)) {
                TrackingBox res;
                res.box = (*it).get_state();
                res.id = (*it).m_id + 1;
                res.frame = frame_count;
                frameTrackingResult.push_back(res);
                it++;
            } else {
                it++;
            }

            // remove dead trackers
            if (it != trackers.end() && (*it).m_time_since_update > max_age)
                it = trackers.erase(it);
        }

        return 0;
    }
}

