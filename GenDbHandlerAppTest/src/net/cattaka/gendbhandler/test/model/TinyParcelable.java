
package net.cattaka.gendbhandler.test.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TinyParcelable implements Parcelable {
    private int mData;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mData);
    }

    public static final Parcelable.Creator<TinyParcelable> CREATOR = new Parcelable.Creator<TinyParcelable>() {
        public TinyParcelable createFromParcel(Parcel in) {
            return new TinyParcelable(in);
        }

        public TinyParcelable[] newArray(int size) {
            return new TinyParcelable[size];
        }
    };

    private TinyParcelable(Parcel in) {
        mData = in.readInt();
    }

    public TinyParcelable() {
    }

    public int getmData() {
        return mData;
    }

    public void setmData(int mData) {
        this.mData = mData;
    }

}
