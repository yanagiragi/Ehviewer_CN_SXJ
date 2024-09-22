package com.hippo.ehviewer.ui.scene.download;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.dao.DownloadLabel;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.scene.Announcer;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExternalDownloadLabelDraw {
    private final LayoutInflater inflater;
    private final ExternalDownloadsScene scene;
    private final ViewGroup container;
    private final Context context;

    private View view;
    private Toolbar toolbar;
    private ListView listView;

    public ExternalDownloadLabelDraw(LayoutInflater inflater, @Nullable ViewGroup container, ExternalDownloadsScene scene){
        this.inflater = inflater;
        this.container = container;
        this.scene = scene;
        this.context = scene.getEHContext();
    }

    public View createView(){
        view = inflater.inflate(R.layout.bookmarks_draw, container, false);
        assert context != null;
        AssertUtils.assertNotNull(context);

        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.download_labels);
        toolbar.inflateMenu(R.menu.drawer_download);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            switch (id) {
                case R.id.action_settings:
                    Toast.makeText(scene.getEHContext(), R.string.function_not_supported_description, Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.action_default_download_label:
                    Toast.makeText(scene.getEHContext(), R.string.function_not_supported_description, Toast.LENGTH_SHORT).show();
                    return true;
            }
            return false;
        });

        var dict = scene.getLabelList();
        var labels = new ArrayList<String>();
        labels.add(scene.getString(R.string.default_download_label_name));
        for (var label : dict.keySet()) {
            if (labels.contains(label)) {
                continue;
            }
            labels.add(label);
        }

        // TODO handle download label items update
        final List<DownloadLabelItem> downloadLabelList = new ArrayList<>();
        for (var label : labels) {
            var count = dict.get(label);
            downloadLabelList.add(new DownloadLabelItem(label, count == null ? 0 : count));
        }

        listView = (ListView) view.findViewById(R.id.list_view);
        DownloadLabelAdapter adapter = new DownloadLabelAdapter(Objects.requireNonNull(scene.getEHContext()), R.layout.item_download_label_list, downloadLabelList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String label;
            if (position == 0) {
                label = null;
            } else {
                label = labels.get(position);
            }
            if (!ObjectUtils.equal(label, scene.mLabel)) {
                scene.mLabel = label;
                scene.updateForLabel();
                scene.updateView();
                scene.closeDrawer(Gravity.RIGHT);
            }

        });
        return view;
    }
}
