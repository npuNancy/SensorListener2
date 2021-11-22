package com.example.sensorlistener2.bean;

public class CheckBean {
    private String Url; //音乐路径
    private String title; //音乐名
    private String artist; //艺术家
    private int duration; //音乐时长
                        //音乐Id
    private boolean isChecked; // 每条item选中状态


    public String getUrl(){
        return this.Url;
    }
    public String getTitle(){
        return this.title;
    }
    public String getArtist(){
        return this.artist;
    }
    public int getDuration(){
        return this.duration;
    }

    public void setUrl(String url) {
        Url = url;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }


    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
