package com.revyuk.githublist;

import android.graphics.Bitmap;

import java.util.concurrent.ExecutionException;

/**
 * Created by Vitaly Revyuk on 13.11.2014.
 */
public class GitHubUser {
    private String userLogin;
    private String avatarUrl;
    private String userUrl;
    private Bitmap userBitmap;

    GitHubUser(String userLOGIN, String avatarURL, String userURL, Bitmap bm) {
        userLogin = userLOGIN;
        avatarUrl = avatarURL;
        userUrl = userURL;
        userBitmap = bm;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getUserUrl() {
        return userUrl;
    }

    public void setUserUrl(String userUrl) {
        this.userUrl = userUrl;
    }

    public Bitmap getUserBitmap() {
        return userBitmap;
    }

    public void setUserBitmap(Bitmap userBitmap) {
        this.userBitmap = userBitmap;
    }
}
