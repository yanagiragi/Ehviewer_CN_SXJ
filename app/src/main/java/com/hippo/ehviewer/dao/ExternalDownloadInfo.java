package com.hippo.ehviewer.dao;

import static com.hippo.ehviewer.AppConfig.getDefaultExternalDownloadDir;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryTagGroup;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;

public class ExternalDownloadInfo extends DownloadInfo
{
    public String language;
    public String size;
    public GalleryTagGroup[] tags;
    public String filePath;
    public int torrentCount;

    private static String externalDownloadDir = "";

    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.2f %cB", bytes / 1000.0, ci.current());
    }

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
        galleryDetail.torrentCount = torrentCount;
        galleryDetail.torrentUrl = "https://exhentai.org/gallerytorrents.php?gid=" + gid + "&t=" + token;
        galleryDetail.language = language;
        galleryDetail.size = size;
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
        info.size = object.getString("size");
        info.torrentCount = object.getIntValue("torrentcount");
        info.pages = object.getIntValue("filecount");
        info.posted = object.getString("posted");

        var size = object.getLongValue("filesize");
        info.size = humanReadableByteCountSI(size);

        if (externalDownloadDir.isEmpty()) {
            // cache externalDownloadDir due to its poor performance
            externalDownloadDir = getDefaultExternalDownloadDir().getPath();
        }

        var localPath = object.getString("localPath");
        info.filePath = externalDownloadDir + "/" + localPath.replace("\\","/");

        info.thumb = info.thumb.startsWith("http")
                ? info.thumb
                : externalDownloadDir + "/" + info.thumb;

        info.tgList = new ArrayList<>();
        var tags = object.getJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.size(); ++i) {
                var tag = tags.getString(i);
                info.tgList.add(tag);
            }
        }

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

        info.simpleLanguage = object.getString("simpleLanguage");
        info.language = object.getString("simpleLanguage");
        if (info.language == null && info.tags != null) {
            for(int i = 0; i < info.tags.length; ++i) {
                if (info.tags[i].groupName.compareTo("language") != 0) {
                    continue;
                }

                for(int j = 0; j < info.tags[i].size(); ++j) {
                    var tag = info.tags[i].getTagAt(j);
                    if (tag != "translate") {
                        info.language = tag.substring(0, 1).toUpperCase() + tag.substring(1);
                        break;
                    }
                }

                if (info.language != null) {
                    break;
                }
            }
        }

        return info;
    }
}
