package com.google.ar.core.examples.java.helloar.util;

import android.content.Context;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;

import com.google.ar.core.Camera;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Trackable;
import com.google.ar.core.examples.java.helloar.HelloArActivity;

import java.util.List;
import java.util.Locale;

public class Checker {

  private float width, height;

  private float[] pointsX = {0.50f, 0.50f};
  private float[] pointsY = {0.50f, 0.75f};
  private String[] dataString = new String[pointsX.length];
  private float avgHeight = 0, threshold = 0.2f;
  private final int averageCalculationFrameNum = 30;

  private HelloArActivity context;
  private String[] saveData;

  // tts feedback
  private TextToSpeech tts;

  // set this flag in order to start recording.
  public boolean START_RECORDING = false;

  public Checker(HelloArActivity context) {
    this.context = context;
    this.saveData = new String[pointsX.length];
    tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        if (status != TextToSpeech.ERROR) {
          tts.setLanguage(Locale.KOREAN);
        }
      }
    });
    tts.setPitch(1.0f);
    tts.setSpeechRate(1.0f);
  }

  public String[] getSaveData() {
    String[] record = dataString.clone();
//    saveData = new String[pointsY.length];
    return record;
  }

  public void checkWallOrHole(Frame frame, Camera camera, float width, float height) {
    for(int num = 0; num < pointsX.length; ++num) {
      float coorX = pointsX[num], coorY = pointsY[num];
      List<HitResult> hitResultList = frame.hitTest(coorX * width, coorY * height);
      boolean isHit = false;

      for(HitResult hit : hitResultList) {
        Trackable trackable = hit.getTrackable();

        if(trackable instanceof DepthPoint)
        {
          float res = hit.getHitPose().ty() - camera.getPose().ty();
          int frameNum = context.frame_count - context.DISCARD_FRAME_NUM;

          if(frameNum > 0 && frameNum < averageCalculationFrameNum) { // frameCount 수만큼 평균 높이 구하기
            avgHeight = (avgHeight * frameNum + res) / (frameNum + 1);
            context.avgHeightTextView.setText("Average height : " + avgHeight + "m");
          }
          else {
            if(num == 0) context.textView.setText("Height difference : " + res + "m"); // 중점의 경우를 화면에 출력

            // TODO: Implement feedback of floor detection
            Vibrator vi = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if(res < avgHeight - threshold) { // Downstair
              vi.vibrate(500);
              tts.speak("내려가는 계단이 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
            else if(res > avgHeight + threshold) { // Wall
              // Wall
              tts.speak("벽이 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
              vi.vibrate(100);
            }
            else if(true) { // Upstair
              // feedback
              tts.speak("올라가는 계단이 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
            else if(true) { // Obstacle
              // feedback
              tts.speak("장애물 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }

            if(START_RECORDING) {
              if(dataString[num] == null) {
                dataString[num] = Float.toString(res);
              } else {
                dataString[num] += "\n" + res;
              }
            }
          }

          isHit = true;
          break;
        }
      }

      // Surface가 탐지되지 않아 hit한 점이 없을 때
      if(!isHit) {
        if(num == 0) context.textView.setText("can't find proper surface");

        if(START_RECORDING) {
          if(dataString[num] == null) dataString[num] = "0.00";
          else dataString[num] += "\n" + "0.00";
        }
      }

    }
  }
}
