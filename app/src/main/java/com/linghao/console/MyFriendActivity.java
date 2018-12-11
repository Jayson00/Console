package com.linghao.console;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.List;

public class MyFriendActivity extends AppCompatActivity {

    private ListView mListView;
    private MyListAdapter mAdapter;
    private List<String> muserList = new ArrayList<>();
    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_my_friend);
        mListView = findViewById(R.id.list_friend);
        new Thread(mRunnable).start();
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                muserList = EMClient.getInstance().contactManager().getAllContactsFromServer();

                System.out.println("获得的联系人数据的数量："+muserList.size());
                if (muserList.size()>0){
                    mHandler.sendEmptyMessage(10);
                }
            } catch (HyphenateException e) {
                e.printStackTrace();
            }
        }
    };

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 10:
                    mAdapter = new MyListAdapter(mActivity,muserList);
                    mListView.setAdapter(mAdapter);
                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            Intent intent = new Intent(mActivity,MyChatActivity.class);
                            intent.putExtra("user",muserList.get(i));
                            startActivity(intent);
                        }
                    });
                    break;
            }
        }
    };
}
