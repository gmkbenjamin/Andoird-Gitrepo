package io.github.gmkbenjamin.gitrepo.beta.ui.adapter;

import android.content.Context;

import java.util.List;

import io.github.gmkbenjamin.gitrepo.beta.db.entity.User;

public class UsersPopupListAdapter extends BasePopupListAdapter<User> {

    private List<User> users;

    public UsersPopupListAdapter(Context context, List<User> users, int resourceIconPullIcon, int resourceIconPushPullIcon) {
        super(context, resourceIconPullIcon, resourceIconPushPullIcon);
        this.users = users;
    }

    @Override
    protected List<User> getData() {
        return users;
    }

    @Override
    protected void setData(List<User> data) {
        this.users = data;
    }

    @Override
    protected int getDataItemId(int position) {
        return users.get(position).getId();
    }

    @Override
    protected String getDataItemName(int position) {
        return users.get(position).getFullname();
    }

}