package com.photostalk.models;

import java.io.Serializable;

/**
 * Created by mohammed on 2/18/16.
 */
public abstract class Model implements Serializable {
    protected String mId;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public abstract void copyFrom(Model model);

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Model && mId.equals(((Model) o).mId);
    }
}
