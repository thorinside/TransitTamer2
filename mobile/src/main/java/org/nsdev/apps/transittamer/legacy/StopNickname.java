package org.nsdev.apps.transittamer.legacy;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;

public class StopNickname implements Parcelable {

    @DatabaseField(id = true)
    private String stopId;

    @DatabaseField
    private String nickName;

    public StopNickname() {
        // Required
    }

    public StopNickname(String stopId, String nickName) {
        this.stopId = stopId;
        this.nickName = nickName;
    }

    private StopNickname(Parcel in) {
        stopId = in.readString();
        nickName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(stopId);
        out.writeString(nickName);
    }

    @Override
    public String toString() {
        return String.format("%s - %s", stopId, nickName);
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<StopNickname> CREATOR
            = new Parcelable.Creator<StopNickname>() {
        @Override
        public StopNickname createFromParcel(Parcel in) {
            return new StopNickname(in);
        }

        @Override
        public StopNickname[] newArray(int size) {
            return new StopNickname[size];
        }
    };

}
