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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ConfirmationFragment extends Fragment {
  private static final String ARG_NORMALIZE_ORIENTATION=
    "normalizeOrientation";
  private Float quality;

  public interface Contract {
    void completeRequest(ImageContext imageContext, boolean isOK);
    void retakePicture();
  }
    private View root;
  private ImageView iv;
  private ImageContext imageContext;

  public static ConfirmationFragment newInstance(boolean normalizeOrientation) {
    ConfirmationFragment result=new ConfirmationFragment();
    Bundle args=new Bundle();

    args.putBoolean(ARG_NORMALIZE_ORIENTATION, normalizeOrientation);
    result.setArguments(args);

    return(result);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
      Log.i(VideoRecorderActivity.class.getName(), "onCreate");
    setRetainInstance(true);
//    setHasOptionsMenu(true);
  }

  @Override
  public void onAttach(Activity activity) {
    if (!(activity instanceof Contract)) {
      throw new IllegalStateException("Hosting activity must implement Contract interface");
    }

    super.onAttach(activity);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      Log.i(VideoRecorderActivity.class.getName(), "onCreateView");
      root = inflater.inflate(R.layout.cwac_cam2_confirmation_fragment, container, false);
    iv= (ImageView) root.findViewById(R.id.cwac_cam2_thumbnail);
      root.findViewById(R.id.cwac_cam2_confirm).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              getContract().completeRequest(imageContext, true);
          }
      });

      root.findViewById(R.id.cwac_cam2_cancel).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              getContract().retakePicture();
          }
      });
      if (getActivity() instanceof VideoRecorderActivity) {
          root.findViewById(R.id.cwac_cam2_result_timer).setVisibility(View.VISIBLE);
          root.findViewById(R.id.cwac_cam2_play_thumbnail).setVisibility(View.VISIBLE);
          root.findViewById(R.id.cwac_cam2_play_thumbnail).setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  Uri contentUri = ((AbstractCameraActivity) getActivity()).getOutputUri();
                  if (contentUri != null) {
                      File file = new File(contentUri.getPath());
                      MimeTypeMap myMime = MimeTypeMap.getSingleton();
                      String extensoin = fileExt(file.toString());
                      if (extensoin != null) {
                          String mimeType = myMime.getMimeTypeFromExtension(extensoin.substring(1));
                          Intent intent = new Intent();
                          intent.setAction(android.content.Intent.ACTION_VIEW);
                          if (contentUri.getPath().startsWith("file://")) {
                              intent.setDataAndType(contentUri, mimeType);
                          } else {
                              intent.setDataAndType(Uri.fromFile(file), mimeType);
                          }
                          startActivity(intent);
                      }

                  }

              }
          });
      } else {
          root.findViewById(R.id.cwac_cam2_play_thumbnail).setVisibility(View.GONE);
          root.findViewById(R.id.cwac_cam2_result_timer).setVisibility(View.GONE);

      }



    if (imageContext!=null) {
      loadImage(quality);
    }

    return(root);
  }

    private String fileExt(String uri) {
        if (uri.contains("?")) {
            uri = uri.substring(0,uri.indexOf("?"));
        }
        if (uri.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = uri.substring(uri.lastIndexOf(".") );
            if (ext.contains("%")) {
                ext = ext.substring(0,ext.indexOf("%"));
            }
            if (ext.contains("/")) {
                ext = ext.substring(0,ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }

  @Override
  public void onHiddenChanged(boolean isHidden) {
    super.onHiddenChanged(isHidden);

    if (!isHidden) {
      ActionBar ab=getActivity().getActionBar();
        TextView resultTimer = (TextView) getActivity().findViewById(R.id.cwac_cam2_result_timer);
        if (resultTimer != null) {
            resultTimer.setText(VideoRecorderActivity.timeText);
        }
      if (ab==null) {
        throw new IllegalStateException("CameraActivity confirmation requires an action bar!");
      }
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//    inflater.inflate(R.menu.cwac_cam2_confirm, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId()==android.R.id.home) {
      getContract().completeRequest(imageContext, false);
    }
    else if (item.getItemId()==R.id.cwac_cam2_ok) {
      getContract().completeRequest(imageContext, true);
    }
    else if (item.getItemId()==R.id.cwac_cam2_retry) {
      getContract().retakePicture();
    }
    else {
      return(super.onOptionsItemSelected(item));
    }

    return(true);
  }

  public void setImage(ImageContext imageContext, Float quality) {
    this.imageContext=imageContext;
    this.quality=quality;


    if (iv!=null) {
      loadImage(quality);
    }
  }

  private Contract getContract() {
    return((Contract)getActivity());
  }

    private void loadImage(Float quality) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        View buttonsContainer = root.findViewById(R.id.cwac_cam2_confirmation_buttons);
        float diffPx = getResources().getDimension(R.dimen.cwac_cam2_conf_fragment_buttons_size) +
                getResources().getDimension(R.dimen.cwac_cam2_conf_fragment_buttons_bottom_margin);
        int maxHeight = screenHeight - (int) diffPx;
        buttonsContainer.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        Log.i(VideoRecorderActivity.class.getName(), "screen width " + screenWidth + " screenHeight " + screenHeight +
                " maxHeight " + maxHeight + " view height " + buttonsContainer.getHeight() + " diffPx " + diffPx);
        if (iv!=null) {
            loadImage(quality, maxHeight, screenWidth);
        }
    }

  private void loadImage(Float quality, int maxHeight, int screenWidth) {
    Bitmap b = imageContext.buildPreviewThumbnail(getActivity(),
            quality, getArguments().getBoolean(ARG_NORMALIZE_ORIENTATION));
      BitmapDrawable bd = new BitmapDrawable(getResources(), b);
      int imageHeight = bd.getIntrinsicHeight();
      int imageWidth = bd.getIntrinsicWidth();
      double heightScale = 1.0* maxHeight  / imageHeight;
      double widthScale = 1.0 * screenWidth / imageWidth;
      Log.i(VideoRecorderActivity.class.getName(), "image width " + imageWidth + " imageHeight " + imageHeight +
              " heightScale " + heightScale + " widthScale " + widthScale);
      double scale = Math.min(heightScale, widthScale);
      Bitmap b2 = Bitmap.createScaledBitmap(b, (int) (imageWidth * scale), (int) (imageHeight * scale), false);
      iv.setImageBitmap(b2);
  }
}
