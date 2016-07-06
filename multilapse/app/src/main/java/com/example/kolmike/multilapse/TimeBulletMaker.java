package com.example.kolmike.multilapse;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class TimeBulletMaker {
    private static final String TAG = "TimeBulletMaker";
    private AnimatedGifEncoder gifEncoder;
    public final int DELAY_MS = 100;
    public final boolean ADD_BACKWARD = true;
    private ArrayList<Bitmap> bitmaps_;
    private byte[] gifData_;
    AnimatedGifEncoder encoder_;


    TimeBulletMaker() {
        bitmaps_ = new ArrayList<Bitmap>();
        encoder_ = new AnimatedGifEncoder();
        encoder_.setDelay(DELAY_MS);
    }

    public void add(Bitmap bitmap) {
        bitmaps_.add(bitmap);
    }

    public void add(byte[] bitmapData) {
        try {
            Log.d(TAG, "get photo with size = " + bitmapData.length);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
            bitmaps_.add(bitmap);
        } catch (Exception error) {
            Log.e(TAG, "Error decoding bitmap: " + error);
        }
    }

    // preprocess (align, crap and so on)
    private void preprocess() {

    }

    public byte[] build() {
        Log.d(TAG, "building gif...");
        try {
            preprocess();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            encoder_.start(out);
            int cnt = 0;
            for (Bitmap bitmap : bitmaps_) {
                encoder_.addFrame(bitmap);
                ++cnt;
            }

            if (ADD_BACKWARD) {
                for (int idx = (int) bitmaps_.size() - 2; idx >= 1; --idx) {
                    encoder_.addFrame(bitmaps_.get(idx));
                    ++cnt;
                }
            }

            Log.d(TAG, "added " + cnt + " photos to gif");

            encoder_.finish();
            gifData_ = out.toByteArray();
            Log.d(TAG, "size = " + gifData_.length);
        } catch (Exception error) {
            Log.e(TAG, "Couldn't encode bitmaps to gif: " + error);
        }
        return gifData_;
    }

    public void saveToFile(String filename) {
        if (gifData_ == null) {
            build();
        }

        try {
            String fullpath = Utility.getFilepath(filename);
            FileOutputStream fos = new FileOutputStream(fullpath);
            fos.write(gifData_);
            fos.close();
            Log.d(TAG, "Image saved to " + filename);
        } catch (Exception error) {
            Log.e(TAG, "Image could not be saved: " + error);
        }
    }
}
