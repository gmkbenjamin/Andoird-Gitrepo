package io.github.gmkbenjamin.gitrepo.beta.ui.adapter;

import android.content.Context;

import java.util.List;

import io.github.gmkbenjamin.gitrepo.beta.db.entity.Repository;

public class RepositoriesPopupListAdapter extends BasePopupListAdapter<Repository> {

    private List<Repository> repositories;

    public RepositoriesPopupListAdapter(Context context, List<Repository> repositories, int resourceIconPullIcon, int resourceIconPushPullIcon) {
        super(context, resourceIconPullIcon, resourceIconPushPullIcon);
        this.repositories = repositories;
    }

    @Override
    protected List<Repository> getData() {
        return repositories;
    }

    @Override
    protected void setData(List<Repository> data) {
        this.repositories = data;
    }

    @Override
    protected int getDataItemId(int position) {
        return repositories.get(position).getId();
    }

    @Override
    protected String getDataItemName(int position) {
        return repositories.get(position).getName();
    }

}