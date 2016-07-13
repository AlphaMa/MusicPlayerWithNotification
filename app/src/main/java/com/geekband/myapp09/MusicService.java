package com.geekband.myapp09;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;

/**
 * Created by SUN on 2016/7/12.
 */
public class MusicService extends Service{

    public static final int[] RES_IDS = {R.raw.i_start_rock,R.raw.sun_da_shen};
    private int songNum = 0;
    private MediaPlayer mediaPlayer;
    private String songName;
    private int resId;
    public static final int PRE_STATUS = 1;
    public static final int PLAY_STATUS = 2;
    public static final int NEXT_STATUS = 3;
    public static final int SERVICE_TO_MAIN_WHAT = 110;
    public static final int MAIN_TO_SERVICE_WHAT = 111;
    public static final String ACTION_BUTTON = "INTENT_ACTION";
    public static boolean IS_PLAY = false;
    private boolean isFirst = true;
    private ButtonBroadcastReceiver bReceiver;

    private NotificationManager mNotificationManager;
    private Messenger reMessenger;


    Messenger messenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            reMessenger = msg.replyTo;
            Message reMsg = Message.obtain();
            reMsg.what = SERVICE_TO_MAIN_WHAT;
            Bundle bundle = new Bundle();
//            reMsg.replyTo = messenger;
            if(msg.what==MAIN_TO_SERVICE_WHAT){
                switch (msg.arg1){
                    case PRE_STATUS:
                        pervious();
                        bundle.putString("songName",getSongName(resId));
                        reMsg.setData(bundle);
                        reMsg.arg1 = PRE_STATUS;
                        mRemoteViews.setTextViewText(R.id.notification_play,"暂停");
                        showButtonNotify();
                        try {
                            msg.replyTo.send(reMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }


                        break;
                    case PLAY_STATUS:
                        if(isFirst){
                            play();
                            isFirst = false;
                        }else{
                            pause();

                        }
                        bundle.putString("songName",getSongName(resId));
                        reMsg.setData(bundle);
                        reMsg.arg1 = PLAY_STATUS;
                        if(mediaPlayer.isPlaying()){
                            mRemoteViews.setTextViewText(R.id.notification_play,"暂停");
                        }else{
                            mRemoteViews.setTextViewText(R.id.notification_play,"播放");
                        }
                        showButtonNotify();
                        try {
                            msg.replyTo.send(reMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }



                        break;
                    case NEXT_STATUS:
                        next();
                        bundle.putString("songName",getSongName(resId));
                        reMsg.setData(bundle);
                        reMsg.arg1 = NEXT_STATUS;
                        mRemoteViews.setTextViewText(R.id.notification_play,"暂停");
                        showButtonNotify();
                        try {
                            msg.replyTo.send(reMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }


                        break;

                }
            }

        }
    });
    private RemoteViews mRemoteViews;
    private NotificationCompat.Builder mBuilder;


    @TargetApi(Build.VERSION_CODES.M)
    public void showButtonNotify() {



        mRemoteViews.setImageViewResource(R.id.notification_image_view, R.mipmap.ic_launcher);
        mRemoteViews.setTextViewText(R.id.notification_text_view, getSongName(resId));
        mRemoteViews.setTextColor(R.id.notification_text_view, Color.BLACK);



        //点击的事件处理
        Intent buttonIntent = new Intent(ACTION_BUTTON);
		/* 上一首按钮 */
        buttonIntent.putExtra("buttonId", PRE_STATUS);
        //这里加了广播，所及INTENT的必须用getBroadcast方法
        PendingIntent intent_prev = PendingIntent.getBroadcast(this, 1, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notification_pre, intent_prev);
		/* 播放/暂停  按钮 */
        buttonIntent.putExtra("buttonId", PLAY_STATUS);
        PendingIntent intent_paly = PendingIntent.getBroadcast(this, 2, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notification_play, intent_paly);
		/* 下一首 按钮  */
        buttonIntent.putExtra("buttonId", NEXT_STATUS);
        PendingIntent intent_next = PendingIntent.getBroadcast(this, 3, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.notification_next, intent_next);

        //点击跳转
        Intent intent = new Intent(this,MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        mBuilder.setContent(mRemoteViews)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())// 通知产生的时间，会在通知信息里显示
                .setTicker("正在播放")
                .setPriority(Notification.PRIORITY_DEFAULT)// 设置该通知优先级
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher);


        //加载
        mNotificationManager.notify(1, mBuilder.build());



    }


    /** 通知栏点击广播接收 */
    public void initButtonReceiver(){
        bReceiver = new ButtonBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BUTTON);
        registerReceiver(bReceiver, intentFilter);
    }

    /**
     *	 广播监听按钮点击事件
     */
    public class ButtonBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(ACTION_BUTTON)){
                Message reMsg = Message.obtain();
                reMsg.what = SERVICE_TO_MAIN_WHAT;
                Bundle bundle = new Bundle();
                //通过传递过来的ID判断按钮点击属性或者通过getResultCode()获得相应点击事件
                int buttonId = intent.getIntExtra("buttonId", 0);
                switch (buttonId) {
                    case PRE_STATUS:
                        pervious();
                        bundle.putString("songName",getSongName(resId));
                        reMsg.setData(bundle);
                        reMsg.arg1 = PRE_STATUS;
                        mRemoteViews.setTextViewText(R.id.notification_play,"暂停");
                        showButtonNotify();
                        try {
                            reMessenger.send(reMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                        Log.i("info" , "上一首");
                        Toast.makeText(getApplicationContext(), "上一首", Toast.LENGTH_SHORT).show();
                        break;
                    case PLAY_STATUS:
                        pause();
                        bundle.putString("songName",getSongName(resId));
                        reMsg.setData(bundle);
                        reMsg.arg1 = PLAY_STATUS;
                        String play_status = "";
                        Log.i("info","IS_PLAY = " + IS_PLAY);
                        if(mediaPlayer.isPlaying()){
                            mRemoteViews.setTextViewText(R.id.notification_play,"暂停");
                            play_status = "开始播放";
                        }else{
                            mRemoteViews.setTextViewText(R.id.notification_play,"播放");
                            play_status = "暂停";
                        }
                        showButtonNotify();

                        try {
                            reMessenger.send(reMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }



                        Log.i("info" , play_status);
                        Toast.makeText(getApplicationContext(), play_status, Toast.LENGTH_SHORT).show();
                        break;
                    case NEXT_STATUS:
                        next();
                        bundle.putString("songName",getSongName(resId));
                        reMsg.setData(bundle);
                        reMsg.arg1 = NEXT_STATUS;

                        mRemoteViews.setTextViewText(R.id.notification_play,"暂停");
                        showButtonNotify();
                        try {
                            reMessenger.send(reMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }


                        Log.i("info" , "下一首");
                        Toast.makeText(getApplicationContext(), "下一首", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }




    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification);

        initButtonReceiver();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bReceiver!=null){
            unregisterReceiver(bReceiver);
        }
//        mediaPlayer.stop();
        mNotificationManager.cancelAll();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    public void play(){
        mediaPlayer.reset(); //重置多媒体
        Log.i("info","songNum =" + songNum);
        //得到当前播放音乐的ID
        resId = RES_IDS[songNum];

        //获取歌名
        songName = getSongName(resId);
        mediaPlayer = MediaPlayer.create(MusicService.this, resId);//为多媒体对象设置播放路径
        Log.i("info","service --play()");
        mediaPlayer.start();//开始播放
        //如果当前歌曲播放完毕,自动播放下一首.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer arg0) {
                next();
                Message reMsg = Message.obtain();
                reMsg.what = SERVICE_TO_MAIN_WHAT;
                Bundle bundle = new Bundle();
                bundle.putString("songName",getSongName(resId));
                reMsg.setData(bundle);
                reMsg.arg1 = NEXT_STATUS;

                mRemoteViews.setTextViewText(R.id.notification_play,"暂停");
                showButtonNotify();
                try {
                    reMessenger.send(reMsg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Log.i("info","MusicService----播放");
    }

    public void pause(){
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            Log.i("info","MusicService----暂停");
        }else {
            mediaPlayer.start();
            Log.i("info","MusicService----再播放");
        }
    }

    public void pervious(){
        if (songNum==0){
            songNum=RES_IDS.length-1;
        }else {
            songNum--;
        }
        play();
        Log.i("info","MusicService----上一首");
    }

    public void next(){
        if (songNum==RES_IDS.length-1){
            songNum = 0;
        }else{
            songNum++;
        }
        play();
        Log.i("info","MusicService----下一首");
    }

    //获得歌曲名称
    public String getSongName(int resId){
        String name = getResources().getResourceName(resId);
        int index = name.lastIndexOf(File.separator);//找到最后一个路径分隔符
        name = name.substring(index + 1);//截取歌曲名
        return name;
    }



}
