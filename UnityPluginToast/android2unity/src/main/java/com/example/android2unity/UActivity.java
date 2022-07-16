package com.example.android2unity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

public class UActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("zzzzzz","UActivity");
        String message = getIntent().getStringExtra("message");
        boolean send_flag = getIntent().getBooleanExtra("send",false);
        Log.d("zzzzzz", String.valueOf(send_flag));
        if(send_flag){
            setContentView(R.layout.activity_camera);
            Log.d("zzzzzz", "unity");
            SendMessageToUnity(message);
            finish();
        }else {
            if (null == savedInstanceState) {
                Log.d("zzzzzz", "camera");
                setContentView(R.layout.activity_camera);
                getFragmentManager().beginTransaction().replace(R.id.container, Camera2VideoFragment.newInstance()).commit();
            }
        }
    }
    //s1: GameObject name that will receive the message.
    //s2: Name of the Method that will handle the message.
    //s3: The message.
    public void SendMessageToUnity(String str){
        UnityPlayer.UnitySendMessage("Canvas","ReceiveMessage",str);
    }


}
