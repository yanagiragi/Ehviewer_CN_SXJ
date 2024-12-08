package com.hippo.ehviewer.ui.scene.download;

import static com.hippo.ehviewer.GetText.getString;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.hippo.app.EditTextDialogBuilder;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExternalDownloadLabelDraw {

    private class FilterLabelDialogHelper implements View.OnClickListener {

        private final EditTextDialogBuilder mBuilder;
        private final AlertDialog mDialog;
        private final ExternalDownloadLabelDraw mScene;

        public FilterLabelDialogHelper(EditTextDialogBuilder builder, AlertDialog dialog, ExternalDownloadLabelDraw scene) {
            mBuilder = builder;
            mDialog = dialog;
            mScene = scene;
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            String text = mBuilder.getText();
            if (getString(R.string.default_download_label_name).equals(text)) {
                mBuilder.setError(getString(R.string.label_text_is_invalid));
            } else {
                mBuilder.setError(null);
                mDialog.dismiss();
                mScene.updateLabel(text);
            }
        }
    }

    public int totalCount;
    public int labelCount;

    private final LayoutInflater inflater;
    private final ExternalDownloadsScene scene;
    private final ViewGroup container;
    private final Context context;

    private View view;
    private Toolbar toolbar;

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
        toolbar.inflateMenu(R.menu.drawer_download);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            switch (id) {
                case R.id.action_settings:
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.label_stats)
                            .setMessage(String.format("\nLabel Count: %d\n\nTotal Count: %d", labelCount, totalCount))
                            .show();

                    return true;
                case R.id.action_default_download_label:
                    EditTextDialogBuilder builder = new EditTextDialogBuilder(context, null, getString(R.string.download_labels));
                    builder.setTitle(R.string.filter_label_title);
                    builder.setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.show();
                    new FilterLabelDialogHelper(builder, dialog, this);
                    return true;
            }
            return false;
        });

        updateLabel(null);

        return view;
    }

    public void updateLabel(String filter) {
        var dict = scene.getSortedLabelList();
        var labels = new ArrayList<String>();
        labels.add(scene.getString(R.string.default_download_label_name));

        totalCount = 0;
        for (var label : dict.keySet()) {
            if (TextUtils.isEmpty(filter) || label.contains(filter)) {
                labels.add(label);

                var count = dict.get(label);
                if (count != null) {
                    totalCount += count;
                }
            }
        }
        labelCount = labels.size();

        var title = context.getText(R.string.external_download_labels);
        if (!TextUtils.isEmpty(filter)) {
            title += "(" + filter + ")";
        }
        toolbar.setTitle(title);

        final List<DownloadLabelItem> downloadLabelList = new ArrayList<>();
        for (var label : labels) {
            var count = dict.get(label);
            downloadLabelList.add(new DownloadLabelItem(label, count == null ? 0 : count));
        }

        ListView listView = (ListView) view.findViewById(R.id.list_view);
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
    }
}
