package com.hippo.ehviewer.dao;

import android.os.Parcelable;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryTagGroup;

import java.util.Arrays;

public class ExternalDownloadInfo extends DownloadInfo
{
    public String language;
    public String size;
    public GalleryTagGroup[] tags;

    public static ExternalDownloadInfo externalDownloadInfoFromJson(JSONObject object) throws ClassCastException {
        ExternalDownloadInfo info = new ExternalDownloadInfo ();
        info.updateInfo(GalleryInfo.galleryInfoFromJson(object));
        info.finished = object.getIntValue("finished");
        info.legacy = object.getIntValue("legacy");
        info.label = object.getString("label");
        info.downloaded = object.getIntValue("downloaded");
        info.remaining = object.getLongValue("remaining");
        info.speed = object.getLongValue("speed");
        info.state = object.getIntValue("state");
        info.time = object.getLongValue("time");
        info.total = object.getIntValue("total");
        info.language = object.getString("language");
        info.size = object.getString("size");

        // TODO: Verify if this logic works
        JSONArray array = object.getJSONArray("tags");
        if (array != null) {
            info.tags = new GalleryTagGroup[array.size()];
            for (int i = 0; i < info.tags.length; ++i) {
                JSONObject element = array.getJSONObject(i);
                info.tags[i].addTag(element.toString());
            }
        }

        return info;
    }
}
