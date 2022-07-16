package com.example.android2unity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

public class MainActivity extends UnityPlayerActivity {
    //Context mContext = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("zzzzzzz", "Android initilization");

    }

    public int Add(int a, int b) {
        Log.d("hi", "add");
        return a + b;
    }

    public int ToastToast(int c,Context mContext) {
        Intent intent = new Intent(mContext, UActivity.class);
        intent.putExtra("message","null");
        intent.putExtra("send",false);
        mContext.startActivity(intent);
        return 1;
    }
}
