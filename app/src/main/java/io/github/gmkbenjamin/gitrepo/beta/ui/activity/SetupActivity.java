package io.github.gmkbenjamin.gitrepo.beta.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import io.github.gmkbenjamin.gitrepo.beta.R;
import io.github.gmkbenjamin.gitrepo.beta.ui.fragment.BaseFragment;
import io.github.gmkbenjamin.gitrepo.beta.ui.fragment.FragmentFactory;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.FragmentType;

public class SetupActivity extends ActionBarActivity  {

    private ViewPager viewPager;
    private FragmentType currentFragment = FragmentType.USERS;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat);
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.setup);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        viewPager = (ViewPager) findViewById(R.id.pager);

        new SetupAdapter(this, viewPager);

        if (savedInstanceState != null) {
            viewPager.setCurrentItem(savedInstanceState.getInt("tabSelection"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("tabSelection", viewPager.getCurrentItem());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(C.action.START_HOME_ACTIVITY);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            finish();
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currentFragment == FragmentType.USERS) {
            MenuItem addMenuItem = menu.add("Add");
            addMenuItem.setIcon(R.drawable.ic_actionbar_add_user);
            addMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            addMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem arg0) {
                    Intent intent = new Intent(C.action.START_ADD_USER_ACTIVITY);
                    startActivityForResult(intent, AddUserActivity.REQUEST_CODE_ADD_USER);
                    return true;
                }

            });
            return true;
        } else if (currentFragment == FragmentType.REPOSITORIES) {
            MenuItem addMenuItem = menu.add("Add");
            addMenuItem.setIcon(R.drawable.ic_actionbar_add_repository);
            addMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            addMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem arg0) {
                    Intent intent = new Intent(C.action.START_ADD_REPOSITORY_ACTIVITY);
                    startActivityForResult(intent, AddUserActivity.REQUEST_CODE_ADD_USER);
                    return true;
                }

            });
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    public FragmentType getCurrentFragment() {
        return currentFragment;
    }

    public void setCurrentFragment(FragmentType currentFragment) {
        this.currentFragment = currentFragment;
    }

    public static class SetupAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener, ActionBar.TabListener {

        private final ViewPager pager;
        private final SetupActivity activity;
        private List<BaseFragment> fragments;

        public SetupAdapter(SetupActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            this.activity = activity;
            this.pager = pager;

            FragmentType[] fragmentTypes = FragmentType.values();
            fragments = new ArrayList<BaseFragment>(fragmentTypes.length);
            for (FragmentType fragmentType : fragmentTypes) {
                fragments.add(FragmentFactory.createFragment(fragmentType));

                android.app.ActionBar.Tab tab = activity.getActionBar().newTab();
                tab.setTabListener(new android.app.ActionBar.TabListener() {
                    @Override
                    public void onTabSelected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction fragmentTransaction) {

                    }

                    @Override
                    public void onTabUnselected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction fragmentTransaction) {

                    }

                    @Override
                    public void onTabReselected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction fragmentTransaction) {

                    }
                });
                tab.setText(fragmentType.getTitle());
                activity.getActionBar().addTab(tab);
            }


            this.pager.setAdapter(this);
            this.pager.setOnPageChangeListener(this);
        }

        private void disableActionMode() {
            for (BaseFragment fragment : fragments) {
                fragment.disableActionMode();
            }
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position % fragments.size());
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                disableActionMode();
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            activity.getActionBar().setSelectedNavigationItem(position);
            activity.setCurrentFragment(FragmentType.values()[position]);
            activity.invalidateOptionsMenu();
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction transaction) {
            disableActionMode();
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction transaction) {
            disableActionMode();
            pager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction transaction) {
        }




    }
}
