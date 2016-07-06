package com.example.kolmike.multilapse;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.content.Context;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.view.ViewGroup;
import android.view.SurfaceHolder;


public class CameraHelper {
    private static final String TAG = "CameraHelper";
    private int CAMERA_ID = 0;
    private Camera camera_;
    private final Context context_;
    private final Activity activity_;
    private CameraPreview preview_;

    CameraErrorCallback errorCallback_ = new CameraErrorCallback();

    private final class CameraErrorCallback implements android.hardware.Camera.ErrorCallback {
        public void onError(int error, android.hardware.Camera camera) {
            //Assert.fail(String.format("Camera error, code: %d", error));
            Log.d("TEST", "Error code: " + error);
        }
    }

    private final class JpegCallback implements PictureCallback {

        private final Context context;

        public JpegCallback(Context context) {
            this.context = context;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
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
            Log.d("TEST", "Photo filename = " + filename);
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
            return new File(sdDir, "CameraAPIDemo");
        }
    }


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
            ((ViewGroup)preview_.getParent()).removeView(preview_);
            preview_ = null;
        }
    }

    public void takePicture() {
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


        camera_.setPreviewCallback(null);
        camera_.takePicture(shutterCallback, rawCallback, new JpegCallback(context_));
    }
}
