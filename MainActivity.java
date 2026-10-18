package com.example.aviatorscreenreader;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.graphics.Color;

public class MainActivity extends Activity {
    static final int REQ_CAPTURE = 404;
    TextView status, latest, history;
    EditText manual;
    Button start, stop, add;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,24,24,24);
        root.setBackgroundColor(Color.rgb(16,24,39));
        ScrollView sc = new ScrollView(this); sc.addView(root);
        TextView title = new TextView(this); title.setText("🚀 Aviator Live Screen Reader");
        title.setTextSize(24); title.setTextColor(Color.WHITE); root.addView(title);
        TextView note = new TextView(this);
        note.setText("Reads multiplier text visible on your screen after you explicitly allow screen capture. It does NOT predict hidden/future rounds or place bets.");
        note.setTextColor(Color.LTGRAY); note.setPadding(0,12,0,16); root.addView(note);
        status = new TextView(this); status.setText("Status: stopped"); status.setTextColor(Color.YELLOW); root.addView(status);
        latest = new TextView(this); latest.setText("Latest detected multiplier: —"); latest.setTextSize(20); latest.setTextColor(Color.WHITE); latest.setPadding(0,16,0,16); root.addView(latest);
        start = new Button(this); start.setText("Start screen reading"); root.addView(start);
        stop = new Button(this); stop.setText("Stop screen reading"); root.addView(stop);
        manual = new EditText(this); manual.setSingleLine(true); manual.setHint("Manual multiplier, e.g. 2.45"); root.addView(manual);
        add = new Button(this); add.setText("Add manual result"); root.addView(add);
        history = new TextView(this); history.setText("Recent results: none"); history.setTextColor(Color.WHITE); history.setPadding(0,18,0,18); root.addView(history);
        TextView caution = new TextView(this); caution.setText("OCR can misread numbers; verify every detected value. Past multipliers cannot reliably predict the next result. Use only for recordkeeping."); caution.setTextColor(Color.LTGRAY); root.addView(caution);
        setContentView(sc);
        start.setOnClickListener(v -> {
            MediaProjectionManager m = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(m.createScreenCaptureIntent(), REQ_CAPTURE);
        });
        stop.setOnClickListener(v -> { stopService(new Intent(this, CaptureService.class)); status.setText("Status: stopped"); });
        add.setOnClickListener(v -> {
            try { double x = Double.parseDouble(manual.getText().toString().trim()); if(x < 1 || x > 1000000) throw new Exception();
                CaptureService.addResult(this, x, "manual"); manual.setText(""); refresh();
            } catch(Exception e) { Toast.makeText(this,"Enter a valid multiplier ≥ 1.00",Toast.LENGTH_SHORT).show(); }
        });
    }
    @Override protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r,c,d);
        if(r==REQ_CAPTURE && c==RESULT_OK && d!=null) {
            Intent s = new Intent(this,CaptureService.class);
            s.putExtra("resultCode",c); s.putExtra("data",d);
            startForegroundService(s);
            status.setText("Status: screen reading active — Android may show a capture indicator");
            CaptureService.setListener(this::refresh);
        } else status.setText("Screen capture permission not granted.");
    }
    void refresh() {
        runOnUiThread(() -> {
            String last = CaptureService.getLatest();
            latest.setText("Latest detected multiplier: " + (last==null ? "—" : last));
            history.setText("Recent results:\\n" + CaptureService.getHistoryText());
        });
    }
}
