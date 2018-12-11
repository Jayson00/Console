package com.linghao.console;

/**
 * Created by linghao on 2018/12/4.
 */

public class MusicFileDTO {

    String name;
    String singername;
    long duration;
    long size;
    String data;


    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSingername() {
        return singername;
    }

    public void setSingername(String singername) {
        this.singername = singername;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "LocalMusic{" +
                "name='" + name + '\'' +
                ", singername='" + singername + '\'' +
                ", duration=" + duration +
                ", size=" + size +
                ", data='" + data + '\'' +
                '}';
    }
}
