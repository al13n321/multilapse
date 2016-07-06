package com.example.kolmike.multilapse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class TimeBulletMaker {
    private static final String TAG = "TimeBulletMaker";
    public final int DELAY_MS = 100;
    public final boolean ADD_BACKWARD = true;
    private final int REQUIRED_SIZE = 640;
    private AnimatedGifEncoder gifEncoder;
    private ArrayList<Bitmap> bitmaps_;
    private byte[] gifData_;
    private AnimatedGifEncoder encoder_;

    TimeBulletMaker() {
        bitmaps_ = new ArrayList<Bitmap>();
        encoder_ = new AnimatedGifEncoder();
        encoder_.setDelay(DELAY_MS);
    }

    // http://android-solution-sample.blogspot.co.uk/2011/10/bitmap-out-of-memory.html
    BitmapFactory.Options getScaleOptions(byte[] data) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        BitmapFactory.decodeByteArray(data, 0, data.length, o);

        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = scale;
        return op;
    }

    public void add(Bitmap bitmap) {
        bitmaps_.add(bitmap);
    }

    public void add(byte[] bitmapData) {
        try {
            Log.d(TAG, "get photo with size = " + bitmapData.length);
            BitmapFactory.Options opts = getScaleOptions(bitmapData);
            Log.d(TAG, "scale = " + opts.inSampleSize);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, opts);
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
