package com.hippo.ehviewer.dao;

import static com.hippo.ehviewer.AppConfig.getDefaultExternalDownloadDir;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryTagGroup;

public class ExternalDownloadInfo extends DownloadInfo
{
    public String language;
    public String size;
    public GalleryTagGroup[] tags;
    public String filePath;

    private static String externalDownloadDir = "";

    public GalleryDetail ToGalleryDetail() {
        GalleryDetail galleryDetail = new GalleryDetail();
        galleryDetail.posted = posted;
        galleryDetail.category = category;
        galleryDetail.favoriteName = favoriteName;
        galleryDetail.favoriteSlot = favoriteSlot;
        galleryDetail.gid = gid;
        galleryDetail.pages = pages;
        galleryDetail.rated = rated;
        galleryDetail.rating = rating;
        galleryDetail.simpleLanguage = simpleLanguage;
        galleryDetail.simpleTags = simpleTags;
        galleryDetail.spanGroupIndex = spanGroupIndex;
        galleryDetail.spanIndex = spanIndex;
        galleryDetail.spanSize = spanSize;
        galleryDetail.tgList = tgList;
        galleryDetail.thumb = thumb;
        galleryDetail.thumbHeight = thumbHeight;
        galleryDetail.thumbWidth = thumbWidth;
        galleryDetail.title = title;
        galleryDetail.titleJpn = titleJpn;
        galleryDetail.token = token;
        galleryDetail.uploader = uploader;
        return galleryDetail;
    }

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

        if (externalDownloadDir.isEmpty()) {
            // cache externalDownloadDir due to its poor performance
            externalDownloadDir = getDefaultExternalDownloadDir().getPath();
        }

        var localPath = object.getString("localPath");
        info.filePath = externalDownloadDir + "/" + localPath;

        info.thumb = info.thumb.startsWith("http")
                ? info.thumb
                : externalDownloadDir + "/" + info.thumb;

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
