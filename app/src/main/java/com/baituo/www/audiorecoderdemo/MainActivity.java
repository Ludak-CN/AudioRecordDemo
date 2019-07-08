package com.baituo.www.audiorecoderdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.baituo.www.audiorecoderdemo.activity.audio.AudioRecordActivity;
import com.baituo.www.audiorecoderdemo.activity.audio.AudioTrackActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button toRecordActivity,toTrackActivity ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);

        toRecordActivity = findViewById(R.id.toRecordActivity);
        toTrackActivity = findViewById(R.id.toTrackActivity);

        toRecordActivity.setOnClickListener(this);
        toTrackActivity.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()){
            case R.id.toRecordActivity:
                intent.setClass(this, AudioRecordActivity.class);
                break ;
            case R.id.toTrackActivity:
                intent.setClass(this, AudioTrackActivity.class);
                break ;
        }
        startActivity(intent);
    }
}
