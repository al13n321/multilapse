package com.example.kolmike.multilapse;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CameraHelper {
    private static final String TAG = "CameraHelper";
    private final Context context_;
    private final Activity activity_;
    CameraErrorCallback errorCallback_ = new CameraErrorCallback();
    private CameraPreview preview_;

    public CameraHelper(Activity activity) {
        this.activity_ = activity;
        this.context_ = activity.getApplicationContext();
        this.preview_ = null;
        Log.d(TAG, "init CameraHelper()");
    }

    public void showPreview() {
        if (preview_ == null) {
            preview_ = new CameraPreview(activity_, 0, CameraPreview.LayoutMode.FitToParent);
            FrameLayout.LayoutParams previewLayoutParams = new FrameLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

            activity_.addContentView(preview_, previewLayoutParams);
        }
    }

    public void turnOffPreview() {
        if (preview_ != null) {
            preview_.stop();
            ((ViewGroup) preview_.getParent()).removeView(preview_);
            preview_ = null;
        }
    }

    ;

    public void takePicture(PhotoCallback callback) {
        boolean stopPreview = false;
        if (preview_ == null) {
            showPreview();
            stopPreview = true;
        }
        Camera camera = preview_.getCamera();
        camera.startPreview();
        camera.lock();

        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                Log.v(TAG, "Shutter");
            }
        };

        PictureCallback rawCallback = new PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.v(TAG, "Raw picture taken");
            }
        };


        //camera.setPreviewCallback(null);
        camera.takePicture(shutterCallback, rawCallback, new JpegCallback(this, stopPreview, context_, callback));
    }


    public interface PhotoCallback {
        public abstract void onPictureTaken(Context context, byte[] data);
    }

    public static final class SaveToFileCallback implements PhotoCallback {
        @Override
        public void onPictureTaken(Context context, byte[] data) {
            Log.d(TAG, "onPictureTaken()");
            File pictureFileDir = getDir();

            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                Toast.makeText(context, "Can't create directory to save image.",
                        Toast.LENGTH_LONG).show();
                return;

            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
            String date = dateFormat.format(new Date());
            String photoFile = "Multilapse_" + date + ".jpg";

            String filename = pictureFileDir.getPath() + File.separator + photoFile;

            File pictureFile = new File(filename);
            Log.d(TAG, "Photo filename = " + filename);
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Toast.makeText(context, "New Image saved:" + photoFile,
                        Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                Toast.makeText(context, "Image could not be saved.",
                        Toast.LENGTH_LONG).show();
            }
        }

        private File getDir() {
            File sdDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            return new File(sdDir, "Multilapse");
        }

    }

    private final class CameraErrorCallback implements android.hardware.Camera.ErrorCallback {
        public void onError(int error, android.hardware.Camera camera) {
            Log.d(TAG, "Error code: " + error);
        }
    }

    private final class JpegCallback implements PictureCallback {
        private final Context context;
        private final CameraHelper cameraHelper;
        private final boolean stopPreview;
        private final PhotoCallback callback;

        public JpegCallback(CameraHelper cameraHelper, boolean stopPreview, Context context, PhotoCallback callback) {
            this.context = context;
            this.cameraHelper = cameraHelper;
            this.stopPreview = stopPreview;
            this.callback = callback;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            callback.onPictureTaken(context, data);

            if (stopPreview) {
                cameraHelper.turnOffPreview();
            } else {
                camera.startPreview();
            }
        }
    }
}
