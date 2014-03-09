package com.openfarmanager.android.core.network.googledrive;

import android.database.Cursor;

import com.openfarmanager.android.App;
import com.openfarmanager.android.R;
import com.openfarmanager.android.core.DataStorageHelper;
import com.openfarmanager.android.core.dbadapters.NetworkAccountDbAdapter;
import com.openfarmanager.android.core.network.NetworkApi;
import com.openfarmanager.android.filesystem.FileProxy;
import com.openfarmanager.android.googledrive.model.About;
import com.openfarmanager.android.googledrive.model.Token;
import com.openfarmanager.android.model.NetworkAccount;
import com.openfarmanager.android.model.NetworkEnum;
import com.yandex.disk.client.Credentials;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * author: Vlad Namashko
 */
public class GoogleDriveApi implements NetworkApi {

    private static String ACCESS_TOKEN = "access_token";
    private static String REFRESH_TOKEN = "refresh_token";
    private static String PERMISSION_ID = "permission_id";

    public NetworkAccount saveAccount(About about, Token token) {
        JSONObject authData = new JSONObject();
        try {
            authData.put(ACCESS_TOKEN, token.getAccessToken());
            authData.put(REFRESH_TOKEN, token.getRefreshToken());
            authData.put(PERMISSION_ID, about.getPermissionId());
            long id = NetworkAccountDbAdapter.insert(about.getDisplayName(), NetworkEnum.GoogleDrive.ordinal(), authData.toString());

            Cursor cursor = NetworkAccountDbAdapter.getAccountById(id);
            if (cursor != null) {
                int idxId = cursor.getColumnIndex(NetworkAccountDbAdapter.Columns.ID);
                int idxUserName = cursor.getColumnIndex(NetworkAccountDbAdapter.Columns.USER_NAME);
                int idxAuthData = cursor.getColumnIndex(NetworkAccountDbAdapter.Columns.AUTH_DATA);

                cursor.moveToNext();
                String authDataString = cursor.getString(idxAuthData);
                JSONObject data = new JSONObject(authDataString);
                return new GoogleDriveAccount(cursor.getLong(idxId), cursor.getString(idxUserName),
                        Token.fromLocalData(data.getString(ACCESS_TOKEN), data.getString(REFRESH_TOKEN)),
                            data.getString(PERMISSION_ID));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int getAuthorizedAccountsCount() {
        return NetworkAccountDbAdapter.count(NetworkEnum.GoogleDrive.ordinal());
    }

    @Override
    public List<NetworkAccount> getAuthorizedAccounts() {
        List<NetworkAccount> accounts = new ArrayList<NetworkAccount>();
        Cursor cursor = NetworkAccountDbAdapter.getAccounts(NetworkEnum.GoogleDrive.ordinal());

        if (cursor == null) {
            return accounts;
        }

        try {
            int idxId = cursor.getColumnIndex(NetworkAccountDbAdapter.Columns.ID);
            int idxUserName = cursor.getColumnIndex(NetworkAccountDbAdapter.Columns.USER_NAME);
            int idxAuthData = cursor.getColumnIndex(NetworkAccountDbAdapter.Columns.AUTH_DATA);

            while(cursor.moveToNext()) {
                String authData = cursor.getString(idxAuthData);
                try {
                    JSONObject data = new JSONObject(authData);
                    GoogleDriveAccount account = new GoogleDriveAccount(cursor.getLong(idxId), cursor.getString(idxUserName),
                            Token.fromLocalData(data.getString(ACCESS_TOKEN), data.getString(REFRESH_TOKEN)), data.getString(PERMISSION_ID));
                    accounts.add(account);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } finally {
            cursor.close();
            DataStorageHelper.closeDatabase();
        }

        return accounts;
    }

    @Override
    public NetworkAccount newAccount() {
        return new GoogleDriveAccount(-1, App.sInstance.getResources().getString(R.string.btn_new), null, null);
    }

    @Override
    public NetworkAccount getCurrentNetworkAccount() {
        return null;
    }

    @Override
    public void delete(FileProxy file) throws Exception {

    }

    @Override
    public boolean createDirectory(String path) throws Exception {
        return false;
    }

    @Override
    public List<FileProxy> search(String path, String query) {
        return null;
    }

    @Override
    public boolean rename(String fullPath, String s) throws Exception {
        return false;
    }

    public static class GoogleDriveAccount extends NetworkAccount {

        private Token mToken;
        private String mPermissionId;

        public GoogleDriveAccount(long id, String userName, Token token, String permissionId) {
            mId = id;
            mUserName = userName;
            mToken = token;
            mPermissionId = permissionId;
        }

        public Token getToken() {
            return mToken;
        }

        public String getPermissionId() {
            return mPermissionId;
        }

    }
}
