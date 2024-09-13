package com.hippo.ehviewer.dao;

import android.os.Parcelable;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryTagGroup;

import java.util.Arrays;

public class ExternalDownloadInfo extends DownloadInfo
{
    public String language;
    public String size;
    public GalleryTagGroup[] tags;
    public GalleryDetail galleryDetail;

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
        info.galleryDetail = new GalleryDetail();

        JSONArray groupedTags = object.getJSONArray("groupedTags");
        if (groupedTags != null) {
            info.tags = new GalleryTagGroup[groupedTags.size()];
            for (int i = 0; i < groupedTags.size(); ++i) {
                info.tags[i] = new GalleryTagGroup();

                var groupedTag = groupedTags.getJSONObject(i);
                var groupName = groupedTag.getString("groupName");
                info.tags[i].groupName = groupName;

                var tagList = groupedTag.getJSONArray("tagList");
                for (int j = 0; j < tagList.size(); ++j) {
                    var tag = tagList.getString(j);
                    info.tags[i].addTag(tag);
                }
            }
        }

        return info;
    }
}
