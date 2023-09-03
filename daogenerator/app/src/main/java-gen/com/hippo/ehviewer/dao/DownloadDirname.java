package com.hippo.ehviewer.dao;

import org.greenrobot.greendao.annotation.*;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table "DOWNLOAD_DIRNAME".
 */
@Entity(nameInDb = "DOWNLOAD_DIRNAME")
public class DownloadDirname {

    @Id
    private long gid;
    private String dirname;

    @Generated
    public DownloadDirname() {
    }

    public DownloadDirname(long gid) {
        this.gid = gid;
    }

    @Generated
    public DownloadDirname(long gid, String dirname) {
        this.gid = gid;
        this.dirname = dirname;
    }

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    public String getDirname() {
        return dirname;
    }

    public void setDirname(String dirname) {
        this.dirname = dirname;
    }

}
