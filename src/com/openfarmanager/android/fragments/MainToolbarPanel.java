package com.openfarmanager.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.openfarmanager.android.App;
import com.openfarmanager.android.R;
import com.openfarmanager.android.toolbar.MenuBuilder;
import com.openfarmanager.android.utils.SystemUtils;

import static com.openfarmanager.android.controllers.FileSystemController.*;

public class MainToolbarPanel extends Fragment {

    private MainToolbar mToolbar;
    private Handler mHandler;
    private float mDensity;
    private int mMinWidth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDensity = getResources().getDisplayMetrics().density;
        mMinWidth = (int) (80 * mDensity);

        mToolbar = new MainToolbar(getActivity());
        if (mHandler != null) {
            mToolbar.setHandler(mHandler);
        }
        return mToolbar;
    }

    public void invalidate() {
        mToolbar.buildMenu(true);
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
        if (mToolbar != null) {
            mToolbar.setHandler(handler);
        }
    }

    private class MainToolbar extends LinearLayout {

        private Handler mHandler;
        private MenuBuilder mMenu;
        private int mItemsCount;

        private OnClickListener mClickListener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                MenuItem item = (MenuItem) view.getTag();
                if (item.hasSubMenu()) {
                    showSubMenu(item);
                } else {
                    sendMessage(item);
                }
            }
        };

        private void sendMessage(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_select:
                    sendMessage(SELECT);
                    return;
                case R.id.action_new:
                    sendMessage(NEW);
                    return;
                case R.id.menu_action:
                    sendMessage(MENU);
                    return;
                case R.id.action_quckview:
                    sendMessage(QUICKVIEW);
                    return;
                case R.id.action_exit:
                    sendMessage(EXIT);
                    return;
                case R.id.action_diff:
                    sendMessage(DIFF);
                    return;
                case R.id.action_find:
                    sendMessage(SEARCH);
                    return;
                case R.id.action_help:
                    sendMessage(HELP);
                    return;
                case R.id.action_settings:
                    sendMessage(SETTINGS);
                    return;
                /*
                case R.id.action_dropbox:
                    sendMessage(NETWORK_DROPBOX);
                    return;
                case R.id.action_skydrive:
                    sendMessage(NETWORK_SKYDRIVE);
                    return;
                case R.id.action_yandex:
                    sendMessage(NETWORK_YANDEX);
                    return;
                */
                case R.id.action_network:
                    sendMessage(NETWORK);
                    return;
                case R.id.action_applauncher:
                    sendMessage(APPLAUNCHER);
                    return;
                case R.id.action_bookmarks:
                    sendMessage(BOOKMARKS);
                    return;
            }
        }

        View.OnTouchListener mAltListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!SystemUtils.isHoneycombOrNever()) {
                        view.setSelected(!view.isSelected());
                        view.setBackgroundResource(view.isSelected() ? R.color.grey_button : R.color.selected_item);
                    }
                    sendMessage(ALT_DOWN);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (SystemUtils.isHoneycombOrNever()) {
                        sendMessage(ALT_UP);
                    }
                }
                return true;
            }
        };

        private void showSubMenu(MenuItem item) {
            SubMenuDialog dialog = SubMenuDialog.newInstance(item, new SubMenuDialog.OnActionSelectedListener() {
                @Override
                public void onActionSelected(MenuItem item) {
                    sendMessage(item);
                }
            });
            try {
                dialog.show(getFragmentManager(), "dialog");
            } catch (Exception ignore) {}
        }

        public MainToolbar(Context context) {
            super(context);
            initMenu(context);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w != oldw) {
                buildMenu(false);
            }
        }

        public void buildMenu(boolean forceRedraw) {
            int itemsCount = getMeasuredWidth() / mMinWidth;
            if (itemsCount == mItemsCount && !forceRedraw) {
                return;
            }

            removeAllViews();

            mItemsCount = itemsCount;
            int used = mMenu.size();

            SparseArray<TextView> mViews = new SparseArray<TextView>();

            for (int i = 0; i < mMenu.size(); i++) {
                MenuItem item = mMenu.getItem(i);
                mViews.put(i * 100, getTextView(item));
            }

            int expanded = 0;
            while (true) {
                if (expanded == mMenu.size() || used > mItemsCount) {
                    break;
                }

                MenuItem item = mMenu.getItem(expanded);
                expanded++;
                if (!item.hasSubMenu()) {
                    continue;
                }
                if (used + item.getSubMenu().size() > mItemsCount) {
                    continue;
                }

                used += item.getSubMenu().size() - 1;

                int index = 0;
                for (int i = 0; i < mViews.size(); i++) {
                    if (mViews.valueAt(i).getTag().equals(item)) {
                        index = mViews.keyAt(i);
                        mViews.remove(index);
                    }
                }

                for (int i = 0; i < item.getSubMenu().size(); i++) {
                    MenuItem sub = item.getSubMenu().getItem(i);
                    sub.getOrder();
                    mViews.put(index + i, getTextView(sub));
                }
            }

            for (int i = 0; i < mViews.size(); i++) {
                addView(mViews.valueAt(i));
            }

            post(new Runnable() {
                public void run() {
                    requestLayout();
                }
            });
        }

        private TextView getTextView(MenuItem item) {
            int threedip = (int) (3 * mDensity);
            int size = App.sInstance.getSettings().getBottomPanelFontSize();
            TextView view = new TextView(getContext());
            view.setText(item.getTitle());
            view.setGravity(Gravity.CENTER);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
            layoutParams.setMargins(threedip, 0, threedip, 0);
            view.setLayoutParams(layoutParams);
            view.setTag(item);
            if (item.getItemId() == R.id.action_alt) {
                view.setOnTouchListener(mAltListener);
            } else {
                view.setOnClickListener(mClickListener);
            }
            view.setBackgroundColor(getResources().getColor(R.color.selected_item));
            view.setTextColor(getResources().getColor(R.color.black));
            view.setSingleLine();
            view.setPadding(threedip, threedip, threedip, threedip);
            view.setHeight((int) ((6 + 2 * size) * mDensity));
            view.setMinWidth((int) (80 * mDensity));
            return view;
        }

        private void initMenu(Context context) {
            mMenu = new MenuBuilder(context);
            int res = context.getResources().getIdentifier("main", "menu", context.getPackageName());
            new MenuInflater(context).inflate(res, mMenu);
        }

        public void setHandler(Handler handler) {
            this.mHandler = handler;
        }

        public void sendMessage(int what) {
            if (mHandler != null) {
                mHandler.sendEmptyMessage(what);
            }
        }

        private void sendMessage(final int what, final Object obj) {
            if (mHandler != null) {
                mHandler.sendMessage(mHandler.obtainMessage(what, obj));
            }
        }
    }

    /**
     * Used to show select dialog for non-expanded groups
     */
    private static class SubMenuDialog extends BaseDialog {

        private MenuItem menu;
        private OnActionSelectedListener listener;

        public static SubMenuDialog newInstance(MenuItem menu, OnActionSelectedListener listener) {
            SubMenuDialog dialog = new SubMenuDialog();
            dialog.setMenu(menu);
            dialog.setListener(listener);
            return dialog;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Action_Dialog);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            getDialog().setTitle(getSafeString(R.string.app_name));
            View view = inflater.inflate(R.layout.dialog_file_action_menu, container, false);

            final ListView actionsList = (ListView) view.findViewById(R.id.action_list);

            String[] items = new String[menu.getSubMenu().size()];
            for (int i = 0; i < menu.getSubMenu().size(); i++) {
                MenuItem sub = menu.getSubMenu().getItem(i);
                items[i] = (String) sub.getTitle();
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    App.sInstance.getApplicationContext(), android.R.layout.simple_list_item_1, android.R.id.text1, items) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View item = super.getView(position, convertView, parent);
                    item.setMinimumWidth(actionsList.getWidth());
                    return item;
                }
            };

            actionsList.setAdapter(adapter);

            actionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    dismiss();
                    listener.onActionSelected(menu.getSubMenu().getItem(i));

                }
            });

            return view;
        }

        public void setMenu(MenuItem menu) {
            this.menu = menu;
        }

        public void setListener(OnActionSelectedListener listener) {
            this.listener = listener;
        }

        public interface OnActionSelectedListener {
            void onActionSelected(MenuItem item);
        }
    }

}