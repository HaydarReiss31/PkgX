package com.haydareiss.pkgx;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    interface OnClick {
        void onClick(AppEntry entry);
    }

    private final List<AppEntry> items = new ArrayList<>();
    private final OnClick onClick;

    AppAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    void submit(List<AppEntry> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final AppEntry e = items.get(position);
        h.icon.setImageDrawable(e.icon);
        h.name.setText(e.label);
        h.pkg.setText(e.packageName);

        StringBuilder tag = new StringBuilder();
        if (e.system) tag.append(h.itemView.getContext().getString(R.string.tag_system));
        if (!e.enabled) {
            if (tag.length() > 0) tag.append(" · ");
            tag.append(h.itemView.getContext().getString(R.string.tag_disabled));
        }
        h.tag.setText(tag);
        h.tag.setVisibility(tag.length() == 0 ? View.GONE : View.VISIBLE);
        h.itemView.setAlpha(e.enabled ? 1f : 0.55f);
        h.itemView.setOnClickListener(v -> onClick.onClick(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView pkg;
        final TextView tag;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            pkg = itemView.findViewById(R.id.pkg);
            tag = itemView.findViewById(R.id.tag);
        }
    }
}
