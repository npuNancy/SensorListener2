package com.example.sensorlistener2.bean;

import android.media.MediaPlayer;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class MusicPlayer {
    private static final String TAG="MusicPlayer";
    public MediaPlayer mediaPlayer;  // 播放器
    public String playStatus; // 播放状态
    private CheckBean musicInfo; // 音乐信息

    private TextView textViewLog;

    public MusicPlayer(final CheckBean musicInfo, TextView textViewLog){
        this.textViewLog = textViewLog;
        this.musicInfo = musicInfo;
        try{
            File file = new File(musicInfo.getUrl());
            if(!file.exists()){
//                textViewLog.append("------ 文件不存在 ------\n");
                Log.i(TAG, "------ 文件不存在 ------");
                return;
            }else {
//                textViewLog.append("------文件"+musicInfo.getTitle()+" 存在  ----\n");
                Log.i(TAG,"------文件："+musicInfo.getTitle()+" 存在  --------------------");
            }
            if(mediaPlayer!=null && mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }


            mediaPlayer = null;
            mediaPlayer = new MediaPlayer();// 创建媒体播放对象
            mediaPlayer.setDataSource(musicInfo.getUrl());
            mediaPlayer.prepare();
            mediaPlayer.start();// 播放音乐
            playStatus = mediaPlayer.isPlaying()? "开始播放" : "播放失败";
            Log.i(TAG, "**********************    "+ playStatus +"    ***********************");
//            textViewLog.append("*******    "+ playStatus +"  *****\n");

            /* 播放结束回调函数 */
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.release();
                    Log.i(TAG, "**********************    播放结束    ***********************");
//                    textViewLog.append("*****  播放结束 ******\n");
                }
            });

        }catch (Exception e){
            e.printStackTrace();
//            textViewLog.append("====Error: " + e.toString() + " ====\n");
        }
    }

    public String getPlayStatus(){  return this.playStatus;  }
    public MediaPlayer getMediaPlayer(){
        return this.mediaPlayer;
    }
    public boolean getIsPlaying() {
        try{
            return this.mediaPlayer.isPlaying();
        } catch (IllegalStateException e){
            return false;

        }
    }
}
