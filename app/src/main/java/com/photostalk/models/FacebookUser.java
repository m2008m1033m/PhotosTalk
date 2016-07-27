package com.photostalk.models;

/**
 * Created by mohammed on 2/19/16.
 */
public class FacebookUser extends Model {
    private String mFacebookId;
    private String mFullName;
    private String mEmail;

    public String getFacebookId() {
        return mFacebookId;
    }

    public void setFacebookId(String facebookId) {
        mFacebookId = facebookId;
    }

    public String getFullName() {
        return mFullName;
    }

    public void setFullName(String fullName) {
        mFullName = fullName;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    @Override
    public void copyFrom(Model model) {
        FacebookUser other = ((FacebookUser) model);
        setId(other.getId());
        setFullName(other.getFullName());
        setEmail(other.getEmail());
    }
}
