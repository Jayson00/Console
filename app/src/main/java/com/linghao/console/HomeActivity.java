package com.linghao.console;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText mEditText1,mEditText2;
    private Button mButton1,mButton2,mButton3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mEditText1 = findViewById(R.id.et_add_username);
        mEditText2 = findViewById(R.id.et_del_username);
        mButton1 = findViewById(R.id.btn_add_friend);
        mButton2 = findViewById(R.id.btn_del_friend);
        mButton3 = findViewById(R.id.btn_my_friend);
        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);
        mButton3.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_add_friend:
                new Thread(addrun).start();
                break;
            case R.id.btn_del_friend:
                new Thread(delrun).start();
                break;
            case R.id.btn_my_friend:
                Intent intent = new Intent(this,MyFriendActivity.class);
                startActivity(intent);
                break;
        }
    }


    Runnable addrun = new Runnable() {
        @Override
        public void run() {
            EMClient.getInstance().contactManager().aysncAddContact("123","123", new EMCallBack() {
                @Override
                public void onSuccess() {
                    System.out.println("添加成功");
                }

                @Override
                public void onError(int code, String error) {
                    System.out.println("添加失败"+code);
                    System.out.println("添加失败"+error);
                }

                @Override
                public void onProgress(int progress, String status) {
                    System.out.println("onProgress"+progress);
                    System.out.println("onProgress的状态"+status);
                }
            });
        }
    };


    Runnable delrun = new Runnable() {
        @Override
        public void run() {
            try {
                EMClient.getInstance().contactManager().addContact("123","123");
            } catch (HyphenateException e) {
                e.printStackTrace();
            }
        }
    };
}
