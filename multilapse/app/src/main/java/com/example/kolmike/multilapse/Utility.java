package com.example.kolmike.multilapse;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by vchalyshev on 06/07/2016.
 */
public class Utility {
    private static final String TAG = "Utility";

    public static File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appDir = new File(sdDir, "Multilapse");
        if (!appDir.exists() && !appDir.mkdirs()) {
            Log.e(TAG, "Can't create directory to save image.");
            return null;
        }

        return appDir;
    }

    public static String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        return date;
    }

    public static String getFilepath(String filename) throws Exception {
        File dir = Utility.getDir();
        if (dir == null) {
            Log.e(TAG, "No directory.");
            throw new Exception("No directory.");
        }

        return dir.getPath() + File.separator + filename;
    }
}
