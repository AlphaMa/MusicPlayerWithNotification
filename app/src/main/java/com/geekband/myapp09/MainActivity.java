package com.geekband.myapp09;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{


    private Messenger messenger;
    private List<String> list;
    private ArrayAdapter arrayAdapter;
    private ListView listView;
    private TextView textView;
    private Button preButton;
    private Button playButton;
    private Button nextButton;
    private Intent intent;

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            messenger = new Messenger(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };


    private Messenger replayMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == MusicService.SERVICE_TO_MAIN_WHAT){
                switch (msg.arg1){
                    case MusicService.PRE_STATUS:
                        textView.setText(msg.getData().get("songName").toString());
                        playButton.setText("暂停");
                        MusicService.IS_PLAY = true;
                        break;
                    case MusicService.PLAY_STATUS:
                        textView.setText(msg.getData().get("songName").toString());
                        if(MusicService.IS_PLAY){
                            playButton.setText("播放");
                            MusicService.IS_PLAY = false;
                        }else{
                            playButton.setText("暂停");
                            MusicService.IS_PLAY = true;
                        }
                        break;
                    case MusicService.NEXT_STATUS:
                        textView.setText(msg.getData().get("songName").toString());
                        playButton.setText("暂停");
                        MusicService.IS_PLAY = true;
                        break;
                }
            }

        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        //获取播放列表
        getListName();
        bindService(intent,conn, Context.BIND_AUTO_CREATE);

        preButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);




    }

    private void init() {
        listView = (ListView) findViewById(R.id.main_list_view);
        textView = (TextView) findViewById(R.id.main_text_view);
        preButton = (Button) findViewById(R.id.main_pre);
        playButton = (Button) findViewById(R.id.main_play);
        nextButton = (Button) findViewById(R.id.main_next);
        intent = new Intent(this,MusicService.class);
    }

    private void getListName() {
        list = new ArrayList<String>();
        String rawName;
        for(int i=0; i<MusicService.RES_IDS.length; i++){
            rawName = getSongName(MusicService.RES_IDS[i]);
            list.add(rawName);
        }

        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,list);
        listView.setAdapter(arrayAdapter);
    }

    //获得歌曲名称
    public String getSongName(int resId){
        String name = getResources().getResourceName(resId);
        int index = name.lastIndexOf(File.separator);//找到最后一个路径分隔符
        name = name.substring(index + 1);//截取歌曲名
        return name;
    }


    @Override
    public void onClick(View view) {
        Message msg = Message.obtain();
        msg.replyTo = replayMessenger;
        msg.what = MusicService.MAIN_TO_SERVICE_WHAT;
        switch (view.getId()){
            case R.id.main_pre:
                msg.arg1 = MusicService.PRE_STATUS;
//                playButton.setText("暂停");
//                MusicService.IS_PLAY = true;
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.main_play:
                msg.arg1 = MusicService.PLAY_STATUS;
//                if(MusicService.IS_PLAY){
//                    playButton.setText("播放");
//                    MusicService.IS_PLAY = false;
//                }else{
//                    playButton.setText("暂停");
//                    MusicService.IS_PLAY = true;
//                }
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.main_next:
                msg.arg1 = MusicService.NEXT_STATUS;
//                playButton.setText("暂停");
//                MusicService.IS_PLAY = true;
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }
}
