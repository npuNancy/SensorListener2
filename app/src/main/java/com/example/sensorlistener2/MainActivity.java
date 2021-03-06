package com.example.sensorlistener2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.sensorlistener2.adapter.MyAdapter;
import com.example.sensorlistener2.bean.CheckBean;
import com.example.sensorlistener2.bean.MusicPlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{
    final private String TAG = "Log Info";
    private CheckBox mCkSelectAll; // 全选按钮
    private Button mBtnStartPlay; // 开始播放并采集按钮
    private ListView listView; // 音频list
    private TextView textViewLog;  // 信息输出区

    private MyAdapter myMusicAdapter;
    private String logInfo;

    private Handler handler;

    private List<CheckBean> musicCheckInfoList = new ArrayList<>(); //音乐播放器列表
    public int musicIndex; // 当前播放的音乐的在所有音频文件里的index
    public int checkedMusicIndex; // 当前播放的音乐的在选中音频文件里的index
    public int checkedMusicNumber; // 选中的音频的数量

    private SensorManager sensorManager;
    private MySensorEventListener sensorEventListener;
    private String linearAccelerationData; // 线性加速度传感器数据
    private String gyroscopeSensorData; // 陀螺仪传感器数据
    private String accelerometerSensorData; // 加速度传感器数据

    /* 以【时间】而非【下标】来区分 同个音频采集的来的不同传感器数据文件 */
    private String sensorDataFilename; // 传感器信号文件名
    private String nowTime; // 用于传感器信号文件名, 防止重名



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 保持屏幕常亮

        // 初始化View
        mBtnStartPlay = (Button) findViewById(R.id.btn_start_play);
        mCkSelectAll = (CheckBox) findViewById(R.id.select_all);
        listView =(ListView) findViewById(R.id.music_list);
        textViewLog = (TextView) findViewById(R.id.logs);

        textViewLog.setMovementMethod(ScrollingMovementMethod.getInstance()); // 垂直滚动
        Log.i(TAG,"success: 垂直滚动");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); // 获取传感器管理器
        sensorEventListener = new MySensorEventListener();
        Log.i(TAG,"success: 获取传感器管理器");

        // 权限查询与获取
        int permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCheck3 = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BODY_SENSORS);
        if ( permissionCheck1 != PackageManager.PERMISSION_GRANTED ||
                permissionCheck2 != PackageManager.PERMISSION_GRANTED ||
                permissionCheck3 != PackageManager.PERMISSION_GRANTED ){
            Log.e(TAG,"error: permission deny");
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.BODY_SENSORS
            }, 1);
        }

        // 获取音乐列表
        musicCheckInfoList = getMusicCheckInfoList(getApplicationContext());
        Log.i(TAG,"success: getMusicCheckInfoList");
        Log.i(TAG,"success: MusicCheckInfoList len = " + musicCheckInfoList.size());

        // 加载Adapter
        myMusicAdapter = new MyAdapter(MainActivity.this, musicCheckInfoList, mCkSelectAll);
        listView.setAdapter(myMusicAdapter);
        mCkSelectAll.setOnCheckedChangeListener(this);
        myMusicAdapter.notifyDataSetChanged(); //调用notifyDataSetChanged刷新列表
        Log.i(TAG,"success: List View");

        mBtnStartPlay.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mBtnStartPlay.setEnabled(false); // 设置按钮不可点击
                mBtnStartPlay.setText("不要触碰手机！！");
                checkedMusicNumber = getCheckedMusicNumber(); // 选中音频文件的数量
                nowTime = getStringDate(); // 用于传感器信号文件名, 防止重名
                playAllMusics(); /* 按顺序播放选中音乐，并采集传感器信号写入文件 */
            }

        });

    }

    /* Log 自动滚动到最后一行 */
    void refreshTextView(String text){
        textViewLog.post(new Runnable() {
            @Override
            public void run() {
                textViewLog.append(text);
                int scrollAmount = textViewLog.getLayout().getLineTop(textViewLog.getLineCount()) - textViewLog.getHeight();
                if (scrollAmount > 0){
                    textViewLog.scrollTo(0, scrollAmount);
                }
                else{
                    textViewLog.scrollTo(0, 0);
                }
            }
        });
    }

    @SuppressLint("Range")
    /* 从本地获取音乐列表 */
    public static List<CheckBean> getMusicCheckInfoList(Context context){
        String TAG = "Log Info";
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        List<CheckBean> musicInfoList = new ArrayList<>();
        int i = 0;
        while(cursor.moveToNext()){
            CheckBean musicInfo = new CheckBean();
            musicInfo.setUrl(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
            musicInfo.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            musicInfo.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            musicInfo.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
            musicInfo.setChecked(false);
            musicInfoList.add(musicInfo);
            i+=1;
            Log.i(TAG, "Music Info: " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        }
        Log.i(TAG,"Music List Len"+ i);
        return musicInfoList;
    }

    @Override
    /* 全选按钮 */
    public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
        mCkSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myMusicAdapter.ckAllVertical(isChecked);
            }
        });
    }

    /* 返回选中音频文件的数量 */
    public int getCheckedMusicNumber(){
        int i = 0;
        for (CheckBean musicInfo: musicCheckInfoList){
            if (musicInfo.isChecked()){
                i += 1;
            }
        }
        return i;
    }

    /* 按顺序播放选中音乐，并采集传感器信号写入文件 */
    public void playAllMusics(){
        textViewLog.setText("日志：\n有异常问题先重启APP\n");
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0){
                    refreshTextView(logInfo); // 输出“开始播放” or “播放结束”
                    listView.smoothScrollToPositionFromTop(musicIndex, 0); // 当前item滑动到页面顶部
                } else if(msg.what == 1){
                    mBtnStartPlay.setEnabled(true); // 设置按钮可点击
                    mBtnStartPlay.setText("开始采集");
                } else if(msg.what == 2){
                    startSensorListener(); // 开始采集传感器信号
                    Log.i(TAG,"开始采集传感器信号");
                } else if(msg.what == 3){
                    finishSensorListener(); // 结束采集
                    Log.i(TAG,"传感器信号采集结束");
                } else if(msg.what == 4){
                    // 把传感器信号写入文件
                    String file1 = saveCSV(sensorDataFilename, linearAccelerationData, nowTime, "linearAcceleration");
                    String file2 = saveCSV(sensorDataFilename, gyroscopeSensorData, nowTime, "gyroscope");
                    String file3 = saveCSV(sensorDataFilename, accelerometerSensorData, nowTime, "accelerometer");
                }
            }
        };
        // 循环音乐列表
        new Thread(new Runnable() {
            @Override
            public void run() {
                musicIndex = 0;
                checkedMusicIndex = 0;
                for (CheckBean musicInfo: musicCheckInfoList){
                    playOneMusic(handler, musicInfo);
                    musicIndex += 1;
                }
                handler.sendEmptyMessage(1); // 设置按钮可点击
            }
        }).start();
    }

    /* 播放一首音乐 */
    public void playOneMusic(Handler handler, CheckBean musicInfo){
        if (musicInfo.isChecked()){
            checkedMusicIndex += 1;
            logInfo = "开始播放：第 " + checkedMusicIndex + " / " + checkedMusicNumber  + " 个: " + musicInfo.getTitle() + "\n";
            handler.sendEmptyMessage(0);

            try {
                /*
                 * 先启动传感器，再开始播放音频
                 * 播放结束后等待100ms，再结束传感器信号的采集
                 * 可以完整的采集到一个音频的所有传感器信号
                 * */
                handler.sendEmptyMessage(2); // 开始采集传感器信号
                MusicPlayer musicPlayer = new MusicPlayer(musicInfo, textViewLog); // 播放音频

                // 等待一首歌的时间
                int duration = musicInfo.getDuration();
                while(duration >= 0){
                    if(musicPlayer.getIsPlaying()){
                        Thread.currentThread().sleep(1);
                        duration -= 1;
                    } else {
                        duration = -1;
                    }
                }

                Thread.currentThread().sleep(100); // 等待一个震动传递到传感器的时间
                handler.sendEmptyMessage(3); // 结束采集传感器信号

                sensorDataFilename = musicInfo.getTitle(); // 获取文件名
                handler.sendEmptyMessage(4); // 把传感器信号写入文件

                logInfo = "播放结束：" + musicInfo.getTitle() + "\n";
                handler.sendEmptyMessage(0);

                Thread.currentThread().sleep(1000); // 结束后隔1s播放下一首
            } catch (InterruptedException e) {
                logInfo = "Error: InterruptedException\n";
                handler.sendEmptyMessage(0);
                Log.e(TAG,e.toString());
            }

        }
    }

    /* 开始采集传感器信号 */
    public void startSensorListener() {
        this.linearAccelerationData = "";
        this.gyroscopeSensorData = "";
        this.accelerometerSensorData = "";
        Sensor linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);// 获取线性加速度传感器对象
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);// 获取陀螺仪传感器对象
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// 获取加速度传感器
        this.sensorManager.registerListener(sensorEventListener, linearAccelerationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        this.sensorManager.registerListener(sensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
        this.sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /* 结束采集传感器信号 */
    public void finishSensorListener() {
        this.sensorManager.unregisterListener(sensorEventListener);
    }

    /* 可以得到传感器实时测量出来的变化值 */
    public class MySensorEventListener implements SensorEventListener {
        @SuppressLint("DefaultLocale")
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSensorChanged(SensorEvent event) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss"); //设置时间格式
            Date date = new Date(System.currentTimeMillis());
            String nowTime = formatter.format(date);

            // 得到线性加速度的值
            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                linearAccelerationData += String.format("%s, %f, %f, %f\n", nowTime, event.values[0], event.values[1], event.values[2]);
            }
            // 得到陀螺仪传感器的值
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                gyroscopeSensorData += String.format("%s, %f, %f, %f\n", nowTime, event.values[0], event.values[1], event.values[2]);
            }
            // 得到加速度的值
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerSensorData += String.format("%s, %f, %f, %f\n", nowTime, event.values[0], event.values[1], event.values[2]);
            }

        }

        // 重写变化
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }


    /* 保存文件并返回文件名 */
    public String saveCSV(String filename, String text,String current, String sensorType){
        /*
        * sensorType: 传感器类型 linearAcceleration gyroscope accelerometer
        * */
        String newFilename = "";
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e(TAG, "外部存储不可用");
        }
        if (text.equals("")){
            return  "失败";
        }
        // 建立传感器数据保存路径
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/SenData/" + sensorType + "/");
        if(!dir.exists()){ dir.mkdirs(); }
        if(dir.exists()){
            /* 以【时间】而非【下标】来区分 同个音频采集的来的不同传感器数据文件 */
            newFilename = String.format("%s_%s.csv", filename, current);
            File file = new File(dir.getAbsolutePath(), newFilename);
            Log.i(TAG, "========== file 是否存在： " + file.exists() + " ==========");
            if(!file.exists()) {
                try {
                    file.createNewFile(); //创建新文件
                    FileOutputStream outStream = new FileOutputStream(file); // 获取文件的输出流对象
                    outStream.write(text.getBytes(StandardCharsets.UTF_8)); // 获取字符串对象的byte数组并写入文件流
                    outStream.close(); // 最后关闭文件输出流
                    Log.i(TAG, "========== " + file.toString() + " 保存成功==========");
                } catch (FileNotFoundException e){
                    Log.e(TAG, "可能是没删除干净");
                    Log.e(TAG, e.toString());
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }else {

            }
        }
        return newFilename;
    }

    /* 返回同名文件的index */
    public int getFileIndex(File path, String filename){
        int index = 0;
        String [] filenames = path.list();
        for (String itemname: filenames){
            String[] seg = itemname.split(".csv")[0].split("_"); // 切分的片段
            String last = seg[seg.length-1]; // 最后一个片段
            String fullFilename = filename+"_"+last+".csv";

            if (itemname.equals(fullFilename)){
                // 是同名文件
                int idx = index;
                try {
                    idx = Integer.parseInt(last);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                index = Math.max(index+1, idx);
            }
        }
        return index;
    }

    /* 获取现在时间 格式为 yyyyMMddHHmmss */
    public static String getStringDate() {
        Date currentTime = new Date();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

}