package com.vrdigitals.technotv;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * The Techno TV start-up screen, drawn in code: the arc reactor spins up, "TECHNO TV" appears,
 * NOVA says the welcome line, then it fades into the home screen. Any button skips it.
 *
 * It is usually opened while the TV is turning off, so that when the TV wakes the first thing
 * on screen is this rather than whatever was showing before. While a Bluetooth speaker
 * reconnects (many TVs switch Bluetooth off in standby) it pulses with "Initialising…".
 */
public class IntroActivity extends Activity {
    /** True while the intro is up, so the Home redirect brings it back instead of the home screen. */
    static volatile boolean showing;
    static volatile long shownAt;
    /** Longest we hold the intro for the Bluetooth speaker to reconnect after the TV wakes. */
    private static final long SPEAKER_WAIT_MS = 5_000;
    /** How long the start-up animation runs before handing over to the home screen. */
    private static final long SHOW_MS = 4_200;
    private static final String WELCOME_FALLBACK = "Welcome back. All systems online.";

    private boolean done;
    private boolean begun;
    private boolean ready;     // speaker connected or wait over
    private boolean started;   // start-up animation running
    private boolean resumed;
    private View core, glow;
    private TextView title, subtitle;
    private ValueAnimator waitPulse;
    private AudioManager audio;
    private PowerManager power;
    private SharedPreferences prefs;
    private TextToSpeech tts;
    private boolean ttsReady;
    private MediaPlayer welcome;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AudioDeviceCallback speakerWatch = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] added) {
            // Let Android finish moving the sound over before the voice starts
            if (speakerConnected()) handler.postDelayed(IntroActivity.this::markReady, 500);
        }
    };
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                begin();
            } else if (started) {
                // Turned off mid-intro: drop this one; a fresh one is prepared for next time
                quit();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showing = true;
        shownAt = SystemClock.uptimeMillis();
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        power = (PowerManager) getSystemService(Context.POWER_SERVICE);
        prefs = getSharedPreferences("intro", MODE_PRIVATE);
        setContentView(buildUi());

        tts = new TextToSpeech(this, s -> {
            if (s != TextToSpeech.SUCCESS) return;
            tts.setLanguage(Locale.UK);
            tts.setPitch(0.9f);
            tts.setSpeechRate(0.95f);
            ttsReady = true;
        });

        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screen, f);
        if (power.isInteractive()) begin();
    }

    /** Screen is on: wait for the Bluetooth speaker if it's normally used, then play. */
    private void begin() {
        if (begun || done) return;
        begun = true;
        shownAt = SystemClock.uptimeMillis();
        if (prefs.getBoolean("btSpeaker", true) && !speakerConnected()) {
            pulseWhileWaiting();
            audio.registerAudioDeviceCallback(speakerWatch, handler);
            handler.postDelayed(this::markReady, SPEAKER_WAIT_MS);
        } else {
            markReady();
        }
    }

    private void markReady() {
        if (ready || done) return;
        ready = true;
        handler.removeCallbacksAndMessages(null);
        audio.unregisterAudioDeviceCallback(speakerWatch);
        play();
    }

    /** Runs the start-up animation, but only while this is actually in front. */
    private void play() {
        if (!ready || started || done || !resumed) return;
        started = true;
        if (waitPulse != null) waitPulse.cancel();
        subtitle.animate().alpha(0f).setDuration(200).start();

        // Reactor spins up and brightens
        core.setScaleX(0.7f);
        core.setScaleY(0.7f);
        core.animate().rotationBy(360f).scaleX(1f).scaleY(1f).setDuration(1600)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
        glow.animate().alpha(1f).setDuration(1200).start();

        // Name, then status line and the spoken welcome
        title.setTranslationY(dp(14));
        title.animate().alpha(1f).translationY(0f).setStartDelay(500).setDuration(700)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
        handler.postDelayed(() -> {
            subtitle.setText("All systems online");
            subtitle.animate().alpha(1f).setDuration(500).start();
            speakWelcome();
        }, 1200);
        handler.postDelayed(this::finishIntro, SHOW_MS);
    }

    /** Plays the recorded welcome line if there is one, else says it with text-to-speech. */
    private void speakWelcome() {
        try (AssetFileDescriptor fd = getAssets().openFd("voice/sys.welcome.m4a")) {
            welcome = new MediaPlayer();
            welcome.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            welcome.prepare();
            welcome.start();
            return;
        } catch (IOException e) {
            // no recording
        }
        if (ttsReady) tts.speak(welcomeText(), TextToSpeech.QUEUE_FLUSH, null, "welcome");
    }

    private String welcomeText() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("voice/index.txt"), "UTF-8"))) {
            for (String row; (row = r.readLine()) != null; ) {
                if (row.startsWith("sys.welcome|")) return row.substring(row.lastIndexOf('|') + 1);
            }
        } catch (IOException ignored) {
        }
        return WELCOME_FALLBACK;
    }

    private boolean speakerConnected() {
        for (AudioDeviceInfo d : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (d.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- screen

    private View buildUi() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.BLACK);

        FrameLayout reactor = new FrameLayout(this);
        reactor.setClipChildren(false);
        glow = new View(this);
        GradientDrawable halo = new GradientDrawable();
        halo.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        halo.setGradientRadius(dp(120));
        halo.setColors(new int[]{0xAA4DA3FF, 0x334DA3FF, 0x004DA3FF});
        glow.setBackground(halo);
        glow.setAlpha(0.45f);
        reactor.addView(glow, new FrameLayout.LayoutParams(dp(240), dp(240), Gravity.CENTER));
        ImageView img = new ImageView(this);
        try (InputStream in = getAssets().open("arc_reactor.png")) {
            img.setImageBitmap(BitmapFactory.decodeStream(in));
        } catch (IOException e) {
            img.setImageDrawable(Icons.reactor());
        }
        core = img;
        reactor.addView(img, new FrameLayout.LayoutParams(dp(130), dp(130), Gravity.CENTER));
        box.addView(reactor, new LinearLayout.LayoutParams(dp(240), dp(240)));

        title = new TextView(this);
        title.setText("TECHNO TV");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setLetterSpacing(0.35f);
        title.setShadowLayer(dp(10), 0, 0, 0xFF2F7BFF);
        title.setAlpha(0f);
        box.addView(title);

        subtitle = new TextView(this);
        subtitle.setText("Initialising…");
        subtitle.setTextColor(0xCCBFE3FF);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitle.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        subtitle.setLetterSpacing(0.3f);
        subtitle.setPadding(0, dp(10), 0, 0);
        subtitle.setAlpha(0f);
        box.addView(subtitle);
        return box;
    }

    private void pulseWhileWaiting() {
        subtitle.animate().alpha(1f).setDuration(300).start();
        waitPulse = ValueAnimator.ofFloat(0f, 1f);
        waitPulse.setDuration(1600);
        waitPulse.setRepeatCount(ValueAnimator.INFINITE);
        waitPulse.setRepeatMode(ValueAnimator.REVERSE);
        waitPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        waitPulse.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            core.setScaleX(1f + 0.05f * t);
            core.setScaleY(1f + 0.05f * t);
            core.setRotation(core.getRotation() + 0.4f);
            glow.setAlpha(0.45f + 0.55f * t);
            subtitle.setAlpha(0.5f + 0.5f * t);
        });
        waitPulse.start();
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Any button skips; act on key-up so the press doesn't also reach the home screen
        if (event.getAction() == KeyEvent.ACTION_UP) finishIntro();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (power.isInteractive()) {
            begin();
            play();
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Covered while the animation was running on a lit screen: don't come back.
        // (While waiting, Google TV's wake-up screen can cover it; the Home redirect brings it back.)
        if (!done && started && power.isInteractive()) quit();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        audio.unregisterAudioDeviceCallback(speakerWatch);
        if (waitPulse != null) waitPulse.cancel();
        if (welcome != null) welcome.release();
        tts.shutdown();
        unregisterReceiver(screen);
        showing = false;
        super.onDestroy();
    }

    /** Close without opening the home screen. */
    private void quit() {
        done = true;
        showing = false;
        finish();
    }

    private void finishIntro() {
        if (done) return;
        done = true;
        showing = false;
        // Remember for next time whether the speaker is in use (it's reconnected by now if it's on)
        prefs.edit().putBoolean("btSpeaker", speakerConnected()).apply();
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
