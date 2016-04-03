package com.photostalk.models;

/**
 * Created by mohammed on 2/19/16.
 */
public class AccessToken extends Model {
    private String mAccessToken;
    private String mRefreshToken;
    private long mExpiration;

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String accessToken) {
        mAccessToken = accessToken;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        mRefreshToken = refreshToken;
    }

    public long getExpiration() {
        return mExpiration;
    }

    public void setExpiration(long expiration) {
        mExpiration = expiration;
    }

    @Override
    public void copyFrom(Model model) {
        AccessToken other = ((AccessToken) model);
        setAccessToken(other.getAccessToken());
        setRefreshToken(other.getRefreshToken());
        setExpiration(other.getExpiration());
    }
}
