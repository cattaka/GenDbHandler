
package net.cattaka.gendbhandler.test.model;

import java.io.Serializable;

public class TinySerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    private int mData;

    public TinySerializable() {
    }

    public int getmData() {
        return mData;
    }

    public void setmData(int mData) {
        this.mData = mData;
    }

}
