package com.endc.reminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    int button_csp_state = 0, dedicate_time = 0, rest_time = 0, random_time;
    int range1, range2, duration, start_sound_position, end_sound_position;
    boolean running = false, pause = false, pause_in_dedicate;
    Button button_csp, button_over, button_dedicate, button_rest;
    EditText EditText_range1, EditText_range2, EditText_duration, EditText_start_sound_position, EditText_end_sound_position;
    Switch Switch_start_sound, Switch_start_vibrate, Switch_start_wake_up_screen, Switch_start_notify, Switch_end_sound, Switch_end_vibrate, Switch_end_wake_up_screen, Switch_end_notify;
    TextView TextView_start_sound_choose, TextView_end_sound_choose, TextView_time_check, TextView_state;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    RingtoneManager ringtoneManager = new RingtoneManager(this);
    Vibrator vibrator;
    NotificationManagerCompat channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 配置文件
        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // 控件
        button_csp = findViewById(R.id.button_csp);
        button_over = findViewById(R.id.button_over);
        button_dedicate = findViewById(R.id.button_dedicate);
        button_rest = findViewById(R.id.button_rest);
        EditText_range1 = findViewById(R.id.range1);
        EditText_range2 = findViewById(R.id.range2);
        EditText_duration = findViewById(R.id.duration);
        EditText_start_sound_position = findViewById(R.id.start_sound_position);
        EditText_end_sound_position = findViewById(R.id.end_sound_position);
        Switch_start_sound = findViewById(R.id.start_sound);
        Switch_start_vibrate = findViewById(R.id.start_vibrate);
        Switch_start_wake_up_screen = findViewById(R.id.start_wake_up_screen);
        Switch_start_notify = findViewById(R.id.start_notify);
        Switch_end_sound = findViewById(R.id.end_sound);
        Switch_end_vibrate = findViewById(R.id.end_vibrate);
        Switch_end_wake_up_screen = findViewById(R.id.end_wake_up_screen);
        Switch_end_notify = findViewById(R.id.end_notify);
        TextView_start_sound_choose = findViewById(R.id.start_sound_choose);
        TextView_end_sound_choose = findViewById(R.id.end_sound_choose);
        TextView_time_check = findViewById(R.id.time_check);
        TextView_state = findViewById(R.id.state);

        // 按钮事件
        button_csp.setOnClickListener(v -> {button_csp_click();});
        button_over.setOnClickListener(v -> {button_over_click();});
        findViewById(R.id.button_check).setOnClickListener(v -> {TextView_time_check.setVisibility(4 - TextView_time_check.getVisibility());});
        button_dedicate.setOnClickListener(v -> {dedicate();});
        button_rest.setOnClickListener(v -> rest());
        Switch_start_notify.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 0);
            }
        });
        Switch_end_notify.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 0);
            }
        });

        // 铃声数量初始化
        TextView_start_sound_choose.setText(getString(R.string.label_sound_choose) + "(0-" + ringtoneManager.getCursor().getCount() + ")");
        TextView_end_sound_choose.setText(getString(R.string.label_sound_choose) + "(0-" + ringtoneManager.getCursor().getCount() + ")");

        // 配置文件载入
        EditText_range1.setText(sharedPreferences.getString("range1", ""));
        EditText_range2.setText(sharedPreferences.getString("range2", ""));
        EditText_duration.setText(sharedPreferences.getString("duration", ""));
        EditText_start_sound_position.setText(sharedPreferences.getString("start_sound_position", ""));
        EditText_end_sound_position.setText(sharedPreferences.getString("end_sound_position", ""));
        Switch_start_sound.setChecked(sharedPreferences.getBoolean("start_sound", false));
        Switch_start_vibrate.setChecked(sharedPreferences.getBoolean("start_vibrate", false));
        Switch_start_wake_up_screen.setChecked(sharedPreferences.getBoolean("start_wake_up_screen", false));
        Switch_start_notify.setChecked(sharedPreferences.getBoolean("start_notify", false));
        Switch_end_sound.setChecked(sharedPreferences.getBoolean("end_sound", false));
        Switch_end_vibrate.setChecked(sharedPreferences.getBoolean("end_vibrate", false));
        Switch_end_wake_up_screen.setChecked(sharedPreferences.getBoolean("end_wake_up_screen", false));
        Switch_end_notify.setChecked(sharedPreferences.getBoolean("end_notify", false));

        // 震动
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 通知通道
        channel = CreateNotificationChannel("reminder_channel", "休息提醒", "", NotificationManager.IMPORTANCE_DEFAULT);

    }
    // 创建通知通道
    private NotificationManagerCompat CreateNotificationChannel(String channel_id, String channel_name, String description, int level){
        NotificationChannel channel = new NotificationChannel(channel_id, channel_name, level);
        if (!description.isEmpty()){
            channel.setDescription(description);
        }
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        return NotificationManagerCompat.from(this);
    }
    private void button_csp_click(){
        switch (button_csp_state){
            case 0:
                String error = "";
                if (EditText_range1.getText().isEmpty()){
                    error += "随机时间范围起始为空\n";
                }
                if (EditText_range2.getText().isEmpty()){
                    error += "随机时间范围终值为空\n";
                }
                if (EditText_duration.getText().isEmpty()){
                    error += "休息时间为空\n";
                }
                if (EditText_start_sound_position.getText().isEmpty()){
                    error += "开始休息铃声选择为空\n";
                }
                if (EditText_end_sound_position.getText().isEmpty()){
                    error += "结束休息铃声选择为空\n";
                }
                if (!error.isEmpty()){
                    new AlertDialog.Builder(this).setTitle("错误").setMessage(error).create().show();
                    break;
                }
                int _range1 = Integer.parseInt(EditText_range1.getText().toString());
                int _range2 = Integer.parseInt(EditText_range2.getText().toString());
                if (_range1 > _range2){
                    new AlertDialog.Builder(this).setTitle("错误").setMessage("随机时间范围起始值应<=终值").create().show();
                    break;
                }
                range1 = _range1;
                range2 = _range2;
                duration = Integer.parseInt(EditText_duration.getText().toString());
                start_sound_position = Integer.parseInt(EditText_start_sound_position.getText().toString());
                end_sound_position = Integer.parseInt(EditText_end_sound_position.getText().toString());

                editor.putString("range1", EditText_range1.getText().toString());
                editor.putString("range2", EditText_range2.getText().toString());
                editor.putString("duration", EditText_duration.getText().toString());
                editor.putString("start_sound_position", EditText_start_sound_position.getText().toString());
                editor.putString("end_sound_position", EditText_end_sound_position.getText().toString());
                editor.putBoolean("start_sound", Switch_start_sound.isChecked());
                editor.putBoolean("start_vibrate", Switch_start_vibrate.isChecked());
                editor.putBoolean("start_wake_up_screen", Switch_start_wake_up_screen.isChecked());
                editor.putBoolean("start_notify", Switch_start_notify.isChecked());
                editor.putBoolean("end_sound", Switch_end_sound.isChecked());
                editor.putBoolean("end_vibrate", Switch_end_vibrate.isChecked());
                editor.putBoolean("end_wake_up_screen", Switch_end_wake_up_screen.isChecked());
                editor.putBoolean("end_notify", Switch_end_notify.isChecked());
                editor.commit();

                button_csp_state = 1;
                button_csp.setText(getString(R.string.button_start));
                button_over.setEnabled(true);
                break;
            case 1:
                running = true;
                if (pause) {
                    if (pause_in_dedicate) {
                        button_dedicate.callOnClick();
                    } else {
                        button_rest.callOnClick();
                    }
                } else {
                    button_dedicate.callOnClick();
                }
                button_csp_state = 2;
                button_csp.setText(getString(R.string.button_pause));
                button_csp.setEnabled(false);
                break;
            case 2:
                pause = true;
                button_csp_state = 1;
                button_csp.setText(getString(R.string.button_start));
                button_csp.setEnabled(false);
                break;
        }
    }
    private void button_over_click(){
        running = false;
        pause = false;
        button_over.setEnabled(false);
        button_csp.setText(getString(R.string.button_confirm));
        button_csp_state = 0;
        TextView_state.setText(getString(R.string.state_ready));
    }
    private void dedicate(){
        int total_time;
        TextView_state.setText(getString(R.string.state_dedicate));
        if (pause) {
            total_time = random_time - dedicate_time;
            pause = false;
        } else {
            random_time = (int) (range1 + (range2 - range1) * Math.random());
            total_time = random_time;
            dedicate_time = 0;
        }
        new CountDownTimer(total_time * 1000L, 1000){

            @Override
            public void onFinish() {
                button_rest.callOnClick();
            }

            @Override
            public void onTick(long l) {
                TextView_time_check.setText(++dedicate_time + "/" + random_time);
                if (!running) {
                    cancel();
                }
                if (pause) {
                    TextView_state.setText(getString(R.string.state_pause));
                    pause_in_dedicate = true;
                    cancel();
                }
                button_csp.setEnabled(true);
            }
        }.start();
    }
    private void rest(){
        rest_start();
        int total_time;
        TextView_state.setText(getString(R.string.state_rest));
        if (pause) {
            total_time = duration - rest_time;
            pause = false;
        } else {
            total_time = duration;
            rest_time = 0;
        }
        new CountDownTimer(total_time * 1000L, 1000){

            @Override
            public void onFinish() {
                rest_end();
                button_dedicate.callOnClick();
            }

            @Override
            public void onTick(long l) {
                TextView_time_check.setText(++rest_time + "/" + duration);
                if (!running) {
                    cancel();
                }
                if (pause) {
                    TextView_state.setText(getString(R.string.state_pause));
                    pause_in_dedicate = false;
                    cancel();
                }
                button_csp.setEnabled(true);
            }
        }.start();
    }
    private void rest_start(){
        if (Switch_start_sound.isChecked()) {
            Ringtone ringtone = ringtoneManager.getRingtone(start_sound_position);
            ringtone.play();
            new CountDownTimer(3000, 3000){

                @Override
                public void onFinish() {
                    ringtone.stop();
                }

                @Override
                public void onTick(long l) {

                }
            }.start();
        }
        if (Switch_start_vibrate.isChecked()) {
            vibrator.vibrate(new long[]{0, 500, 250, 500}, -1);
        }
        if (Switch_start_wake_up_screen.isChecked()) {
            startActivities(new Intent[]{new Intent(this, WakeUpScreen.class)});
        }
        if (Switch_start_notify.isChecked()) {
            channel.notify(1, (new NotificationCompat.Builder(this, "reminder_channel")
                    .setContentTitle("休息时间到")
                    .setContentText("休息" + duration + "秒")
                    .setSmallIcon(R.mipmap.ic_launcher)).build());
        }
    }
    private void rest_end(){
        if (Switch_end_sound.isChecked()) {
            Ringtone ringtone = ringtoneManager.getRingtone(end_sound_position);
            ringtone.play();
            new CountDownTimer(3000, 3000){

                @Override
                public void onFinish() {
                    ringtone.stop();
                }

                @Override
                public void onTick(long l) {

                }
            }.start();
        }
        if (Switch_end_vibrate.isChecked()) {
            vibrator.vibrate(new long[]{0, 500, 250, 500}, -1);
        }
        if (Switch_end_wake_up_screen.isChecked()) {
            startActivities(new Intent[]{new Intent(this, WakeUpScreen.class)});
        }
        if (Switch_end_notify.isChecked()) {
            channel.notify(1, (new NotificationCompat.Builder(this, "reminder_channel")
                    .setContentTitle("休息时间结束了")
                    .setContentText("开始专注吧")
                    .setSmallIcon(R.mipmap.ic_launcher)).build());
        }
    }
}