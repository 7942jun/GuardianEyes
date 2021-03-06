//import android.graphics.Bitmap;
//import android.graphics.Rect;
//import android.graphics.RectF;
//import android.text.PrecomputedText;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.core.util.Pair;
//
//import com.google.ar.core.Camera;
//import com.google.ar.core.Frame;
//import com.google.ar.core.HitResult;
//import com.google.ar.core.TrackingState;
//import com.google.mlkit.common.model.LocalModel;
//import com.google.mlkit.vision.common.InputImage;
//import com.google.mlkit.vision.objects.DetectedObject;
//import com.google.mlkit.vision.objects.ObjectDetection;
//import com.google.mlkit.vision.objects.ObjectDetector;
//import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class MyObjectdetector {
//
//    private LocalModel localModel;
//    private CustomObjectDetectorOptions customObjectDetectorOptions;
//    private ObjectDetector objectDetector;
//
//    MyObjectdetector () {
//        localModel = new LocalModel.Builder()
//                .setAssetFilePath("lite-model_object_detection_mobile_object_labeler_v1_1.tflite")
//                .build();
//
//        // Live detection and tracking
//        customObjectDetectorOptions = new CustomObjectDetectorOptions.Builder(localModel)
//                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
//                        .enableClassification()
//                        .setClassificationConfidenceThreshold(0.5f)
//                        .setMaxPerObjectLabelCount(3)
//                        .build();
//        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
//    }
//
//     public void getResults (InputImage image) {
//        List<Recognition> recList = new ArrayList<Recognition>() {};
//       //Log.d("asdf", "before processing");
//       objectDetector
//                 .process(image)
//                 .addOnFailureListener(e -> {
//                     e.printStackTrace();
//                 })
//                 .addOnSuccessListener(results -> {
//                     for (DetectedObject detectedObject : results) {
//                         Rect boundingBox = detectedObject.getBoundingBox();
////                         Integer trackingId = detectedObject.getTrackingId();
//                         Log.d("ggg", "jeff list - " + detectedObject.getLabels().size());
//                         for (DetectedObject.Label label : detectedObject.getLabels()) {
//                             String text = label.getText();
//                             Integer index = label.getIndex();
//                             Float confidence = label.getConfidence();
//                             RectF rectFBoundingBox = new RectF(boundingBox);
//                             Recognition recognition = new Recognition(index.toString(), text, confidence, rectFBoundingBox);
//                             Log.d("asdf", rectFBoundingBox.top +" "+ rectFBoundingBox.bottom+" "+ rectFBoundingBox.left+" "+ rectFBoundingBox.right);
//                             Log.d("asdf", recognition.toString());
//                             Log.d("ggg", "jeff coord:"+recognition.getCenterCoordinate() + " rect:" + rectFBoundingBox + " title:" + recognition.getTitle());
//                             synchronized (HelloArActivity.obj) {
//                                 HelloArActivity.objRect = rectFBoundingBox;
//                                 HelloArActivity.coor = recognition.getCenterCoordinate();
//                             }
//                         }
//                     }
//                 });
//     }
//}
