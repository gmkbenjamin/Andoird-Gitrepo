package io.github.gmkbenjamin.gitrepo.beta.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import io.github.gmkbenjamin.gitrepo.beta.R;
import io.github.gmkbenjamin.gitrepo.beta.ui.fragment.BaseFragment;
import io.github.gmkbenjamin.gitrepo.beta.ui.fragment.FragmentFactory;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.FragmentType;

public class SetupActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private FragmentType currentFragment = FragmentType.USERS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.setup);

        viewPager = findViewById(R.id.pager);
        TabLayout tabLayout = findViewById(R.id.tabs);

        new SetupAdapter(this, viewPager, tabLayout);

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
            return true;
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
            implements ViewPager.OnPageChangeListener {

        private final ViewPager pager;
        private final SetupActivity activity;
        private final List<BaseFragment> fragments;
        private final List<String> titles;

        public SetupAdapter(SetupActivity activity, ViewPager pager, TabLayout tabLayout) {
            super(activity.getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.activity = activity;
            this.pager = pager;

            FragmentType[] fragmentTypes = FragmentType.values();
            fragments = new ArrayList<>(fragmentTypes.length);
            titles = new ArrayList<>(fragmentTypes.length);
            for (FragmentType fragmentType : fragmentTypes) {
                fragments.add(FragmentFactory.createFragment(fragmentType));
                titles.add(fragmentType.getTitle());
            }

            this.pager.setAdapter(this);
            this.pager.addOnPageChangeListener(this);
            tabLayout.setupWithViewPager(this.pager);
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
        public CharSequence getPageTitle(int position) {
            return titles.get(position % titles.size());
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
            disableActionMode();
            activity.setCurrentFragment(FragmentType.values()[position]);
            activity.invalidateOptionsMenu();
        }
    }
}
