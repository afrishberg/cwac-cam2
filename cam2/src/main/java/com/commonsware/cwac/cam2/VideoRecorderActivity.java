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

package com.commonsware.cwac.cam2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Activity for recording video. Analogue of CameraActivity.
 * Supports the same protocol, in terms of extras and return data,\
 * as does ACTION_VIDEO_CAPTURE.
 */
public class VideoRecorderActivity extends AbstractCameraActivity {
  private static final String[] PERMS={
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.RECORD_AUDIO};
    private static final String TAG = VideoRecorderActivity.class.getName();
    public static String timeText;


    @Override
  protected String[] getNeededPermissions() {
    return(PERMS);
  }

  @Override
  protected boolean needsOverlay() {
    return(false);
  }

  @Override
  protected boolean needsActionBar() {
    return(false);
  }

  @Override
  protected boolean isVideo() {
    return(true);
  }

  @Override
  protected void configEngine(CameraEngine engine) {
    // no-op
  }

  @Override
  protected CameraFragment buildFragment() {
    return(CameraFragment.newVideoInstance(getOutputUri(),
        getIntent().getBooleanExtra(EXTRA_UPDATE_MEDIA_STORE, false),
            getIntent().getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1),
        getIntent().getIntExtra(MediaStore.EXTRA_SIZE_LIMIT, 0),
        getIntent().getIntExtra(MediaStore.EXTRA_DURATION_LIMIT, 0),
            getIntent().getBooleanExtra(EXTRA_FACING_EXACT_MATCH, false)));
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraEngine.VideoTakenEvent event) {
      File outFile = event.getVideoTransaction().getOutputPath();
      int width = cameraFrag.screenShot.getWidth();
      int height = cameraFrag.screenShot.getHeight();

      Log.i(TAG, "decoding width is " + width + " height is " + height);
      Bitmap thumbnail = cameraFrag.screenShot;
      Log.i(TAG, "thumbnail before rotation width is " + thumbnail.getWidth() + " height is " + thumbnail.getHeight());
      if (cameraFrag.orientation == Configuration.ORIENTATION_LANDSCAPE) {
          Display display = ((WindowManager)
                  getSystemService(Activity.WINDOW_SERVICE)).getDefaultDisplay();
          Matrix matrix = new Matrix();
          int rotation = display.getRotation();
          if (display.getRotation() == Surface.ROTATION_270) {
            matrix.postRotate(90);

          } else {
              matrix.postRotate(270);
          }
          Bitmap temp = Bitmap.createBitmap(cameraFrag.screenShot, 0, 0, width,
                  height, matrix, true);

          Log.i(TAG, "rotated screenShot width is " + temp.getWidth() + " height is " + temp.getHeight());
          thumbnail = Bitmap.createScaledBitmap(temp, width, height, false);
          Log.i(TAG, "rotated rescaled thumbnail width is " + thumbnail.getWidth() +
                  " height is " + thumbnail.getHeight());

      }
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, stream);
      byte[] byteArray = stream.toByteArray();

      Log.i(TAG, "done decoding ");
      confirmFrag.setImage(new ImageContext(this, byteArray),
              getIntent().getExtras().getFloat(EXTRA_CONFIRMATION_QUALITY));
      Log.i(TAG, "image is set, doing transaction ");
      findViewById(R.id.cwac_cam2_progress).setVisibility(View.GONE);
      setCurrentFragment(confirmFrag, cameraFrag);
      findViewById(R.id.cwac_cam2_preview_stack).setVisibility(View.VISIBLE);


  }

    @Override
    public void completeRequest(ImageContext imageContext, boolean isOK) {
        Log.i(TAG, "completeRequest");
        if (!isOK) {
            setResult(RESULT_CANCELED);
            finish();
            // TODO: something with the exception
        }
        else {
            final Intent result=new Intent();
            result.putExtra("data", imageContext.buildResultThumbnail(normalizeOrientation()));
            findViewById(android.R.id.content).post(new Runnable() {
                @Override
                public void run() {
                    setResult(RESULT_OK, result.setData(getOutputUri()));
                    getFragmentManager()
                            .beginTransaction()
                            .remove(confirmFrag)
                            .remove(cameraFrag)
                            .commit();
                    //         finish();
                }
            });
        }
    }

    @Override
    public void retakePicture() {
        Log.i(TAG, "retakePicture");
        setCurrentFragment(cameraFrag, confirmFrag);
    }

    /**
   * Class to build an Intent used to start the VideoRecorderActivity.
   * Call setComponent() on the Intent if you are using your
   * own subclass of VideoRecorderActivity.
   */
  public static class IntentBuilder
    extends AbstractCameraActivity.IntentBuilder<IntentBuilder> {
    /**
     * Standard constructor. May throw a runtime exception
     * if the environment is not set up properly (see
     * validateEnvironment() on Utils).
     *
     * @param ctxt any Context will do
     */
    public IntentBuilder(Context ctxt) {
      super(ctxt, VideoRecorderActivity.class);
    }

    @Override
    Intent buildChooserBaseIntent() {
      return(new Intent(MediaStore.ACTION_VIDEO_CAPTURE));
    }

    @Override
    public Intent build() {
      forceEngine(CameraEngine.ID.CLASSIC);

      return(super.build());
    }

    @Override
    public IntentBuilder to(Uri output) {
      if (!"file".equals(output.getScheme())) {
        throw new IllegalArgumentException("must be a file:/// Uri");
      }

      return(super.to(output));
    }

    /**
     * Sets the maximum size of the video file in bytes. Maps
     * to EXTRA_SIZE_LIMIT.
     *
     * @param limit maximum video size in bytes
     * @return
     */
    public IntentBuilder sizeLimit(int limit) {
      result.putExtra(MediaStore.EXTRA_SIZE_LIMIT, limit);

      return(this);
    }

    /**
     * Sets the maximum duration of the video file in milliseconds. Maps
     * to EXTRA_DURATION_LIMIT.
     *
     * @param limit maximum video length in milliseconds
     * @return
     */
    public IntentBuilder durationLimit(int limit) {
      result.putExtra(MediaStore.EXTRA_DURATION_LIMIT, limit);

      return(this);
    }
  }

}
