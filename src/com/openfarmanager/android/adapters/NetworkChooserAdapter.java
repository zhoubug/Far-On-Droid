package com.openfarmanager.android.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.openfarmanager.android.App;
import com.openfarmanager.android.model.NetworkEnum;

/**
 * @author Vlad Namashko
 */
public class NetworkChooserAdapter extends BaseAdapter {

    @Override
    public int getCount() {
        return NetworkEnum.values().length;
    }

    @Override
    public Object getItem(int i) {
        return NetworkEnum.values()[i];
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = View.inflate(App.sInstance.getApplicationContext(), android.R.layout.simple_list_item_2, null);
        }

        NetworkEnum networkEnum = NetworkEnum.valuesList()[i];

        convertView.setTag(networkEnum);
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(NetworkEnum.getNetworkLabel(networkEnum));

        return convertView;
    }
}
