package io.github.gmkbenjamin.gitrepo.beta.ui.adapter;

import android.content.Context;

import java.util.List;

import io.github.gmkbenjamin.gitrepo.beta.db.entity.Permission;

public class UserPermissionListAdapter extends BasePermissionListAdapter {

    public UserPermissionListAdapter(Context context, List<Permission> items, int resourceIconPull, int resourceIconPushPull) {
        super(context, items, resourceIconPull, resourceIconPushPull);
    }

    @Override
    protected String getItemName(int position) {
        return items.get(position).getUser().getFullname();
    }

}
