/***
 Copyright (c) 2015 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.cam2.util;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.commonsware.cwac.cam2.CameraActivity;
import com.commonsware.cwac.cam2.CameraDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Home of static utility methods used by the library and
 * offered to developers in the spirit of togetherness.
 */
public class Utils {

    public static double WIDE_SCREEN_RATIO = 16.0/9;
    public static double NARROW_SCREEN_RATIO = 4.0/3;

    public static int WIDE_SCREEN = 0;
    public static int NARROW_SCREEN = 1;


    static int getTypicalRatio(int width, int height) {
        if (height > width) {
            return getTypicalRatio(height, width);
        }
        double ratio = 1.0 * width / height;
        double wideScreenDiff = Math.abs(WIDE_SCREEN_RATIO - ratio);
        double narrowScreenDiff = Math.abs(NARROW_SCREEN_RATIO - ratio);
        return wideScreenDiff < narrowScreenDiff ? WIDE_SCREEN : NARROW_SCREEN;
    }


  /**
   * Tests the app and the device to confirm that the code
   * in this library should work. This is called automatically
   * by other classes (e.g., CameraActivity), and so you probably
   * do not need to call it yourself. But, hey, it's a public
   * method, so call it if you feel like it.
   *
   * The method will throw an IllegalStateException if the
   * environment is unsatisfactory, where the exception message
   * will tell you what is wrong.
   *
   * @param ctxt any Context will do
   */
  public static void validateEnvironment(Context ctxt) {
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      throw new IllegalStateException("App is running on device older than API Level 14");
    }

    PackageManager pm=ctxt.getPackageManager();

    if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
        !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
      throw new IllegalStateException("App is running on device that lacks a camera");
    }

    if (ctxt instanceof CameraActivity) {
      try {
        ActivityInfo info=pm.getActivityInfo(((CameraActivity)ctxt).getComponentName(), 0);

        if (info.exported) {
          throw new IllegalStateException("A CameraActivity cannot be exported!");
        }
      }
      catch (PackageManager.NameNotFoundException e) {
        throw new IllegalStateException("Cannot find this activity!", e);
      }
    }
  }

  /**
   * Algorithm for determining if the system bar is on the
   * bottom or right. Based on implementation of PhoneWindowManager.
   * Pray that it holds up.
   *
   * @param ctxt any Context will do
   * @return true if the system bar should be on the bottom in
   * the current configuration, false otherwise
   */
  public static boolean isSystemBarOnBottom(Context ctxt) {
    Resources res=ctxt.getResources();
    Configuration cfg=res.getConfiguration();
    DisplayMetrics dm=res.getDisplayMetrics();
    boolean canMove=(dm.widthPixels != dm.heightPixels &&
        cfg.smallestScreenWidthDp < 600);

    return(!canMove || dm.widthPixels < dm.heightPixels);
  }

    public static Size getLargestPictureSize(CameraDescriptor descriptor, Size displaySize) {
        int displayRatio = getTypicalRatio(displaySize.getWidth(), displaySize.getHeight());
        Log.i(Utils.class.getName(), "getLargestPictureSize displaySize " + displaySize +
                " displayRatio " + displayRatio + " exact ratio " + (1.0 * displaySize.getWidth() / displaySize.getHeight()));
        List<Size> correctRatio = new ArrayList<>();

        for (Size size : descriptor.getPictureSizes()) {
            Log.i(Utils.class.getName(), "getLargestPictureSize checking optional " + size + "with ratio " +
                    (1.0 * size.getWidth() / size.getHeight()));
            if (getTypicalRatio(size.getWidth(), size.getHeight()) == displayRatio) {
                correctRatio.add(size);

            }
        }
        if (correctRatio.size() > 0) {
            Size maxSize = Collections.max(correctRatio, new CompareSizesByArea());
            Log.i(Utils.class.getName(), "getLargestPictureSize found a matching ratio resolution returning the largest one: " + maxSize);
            return maxSize;
        } else {
//        Log.e(TAG, "Couldn't find any suitable preview size");
            Log.i(Utils.class.getName(), "getLargestPictureSize didn't find a matching ratio resolution returning the largest one ");
            return Collections.max(descriptor.getPictureSizes(), new CompareSizesByArea());
        }
    }

  public static Size getSmallestPictureSize(CameraDescriptor descriptor) {
    Size result=null;

    for (Size size : descriptor.getPictureSizes()) {
      if (result == null) {
        result=size;
      }
      else {
        int resultArea=result.getWidth() * result.getHeight();
        int newArea=size.getWidth() * size.getHeight();

        if (newArea < resultArea) {
          result=size;
        }
      }
    }

    return(result);
  }

  // based on https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java

  /**
   * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
   * width and height are at least as large as the respective requested values, and whose aspect
   * ratio matches with the specified value.
   *
   * @param choices     The list of sizes that the camera supports for the intended output class
   * @param aspectRatio The aspect ratio
   * @return The optimal {@code Size}, or an arbitrary one if none were big enough
   */
  public static Size chooseOptimalSize(List<Size> choices, Size aspectRatio) {
      // Collect the supported resolutions that are at least as big as the preview Surface
      double exactAspectRatio = 1.0 * aspectRatio.getWidth() / aspectRatio.getHeight();
      Log.i(Utils.class.getName(), "chooseOptimalSize aspectRatio: " + aspectRatio + " exact ratio is " +
              exactAspectRatio);
      int aspectRatioScreenType = getTypicalRatio(aspectRatio.getWidth(), aspectRatio.getHeight());
      List<Size> similarRatio = new ArrayList<>();
      List<Size> exactRatio = new ArrayList<>();
      for (Size option : choices) {
          Log.i(Utils.class.getName(), "chooseOptimalSize checking optional size: " + option + " ratio is " +
                  (1.0 * option.getWidth() / option.getHeight()));
          if (Math.abs(1.0 * option.getWidth() / option.getHeight() - exactAspectRatio ) < 1e-2) {
              exactRatio.add(option);
          }
          else if (getTypicalRatio(option.getWidth(), option.getHeight()) == aspectRatioScreenType) {

              similarRatio.add(option);

          }
      }

      // Pick the smallest of those, assuming we found any
      if(exactRatio.size() > 0) {
          Size maxSize = Collections.max(exactRatio, new CompareSizesByArea());
          Log.i(Utils.class.getName(), "chooseOptimalSize found an exact ratio resolution returning the smallest one " + maxSize);
          return maxSize;
      }
      if (similarRatio.size() > 0) {
          Size maxSize = Collections.max(similarRatio, new CompareSizesByArea());
          Log.i(Utils.class.getName(), "chooseOptimalSize found a similar ratio resolution returning the smallest one " + maxSize);
          return maxSize;
      } else {
//      Log.e(TAG, "Couldn't find any suitable preview size");
          Log.i(Utils.class.getName(), "chooseOptimalSize didn't find a big enough resolution returning the largest one ");
          return Collections.max(choices, new CompareSizesByArea());
      }
  }

  static class CompareSizesByArea implements Comparator<Size> {

    @Override
    public int compare(Size lhs, Size rhs) {
      // We cast here to ensure the multiplications won't overflow
      return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
          (long) rhs.getWidth() * rhs.getHeight());
    }

  }
}
