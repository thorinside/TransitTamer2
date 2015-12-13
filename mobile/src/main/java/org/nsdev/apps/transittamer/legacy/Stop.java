package org.nsdev.apps.transittamer.legacy;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;

public class Stop implements Parcelable {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(uniqueIndex = true, uniqueIndexName = "stop_agencyIdstopId_idx")
    private String agencyId;

    @DatabaseField(uniqueIndex = true, uniqueIndexName = "stop_agencyIdstopId_idx")
    private String stopId;

    @DatabaseField
    private String stopCode;

    @DatabaseField
    private String name;

    @DatabaseField
    private String description;

    @DatabaseField
    private double latitude;

    @DatabaseField
    private double longitude;

    private String nickName;

    public Stop() {
        // Required
    }

    public Stop(String agencyId, String stopId, String stopCode, String name,
                String description, double stopLatitude, double stopLongitude) {
        this.agencyId = agencyId;
        this.stopId = stopId;
        this.stopCode = stopCode;
        this.description = description;
        this.name = name;
        this.latitude = stopLatitude;
        this.longitude = stopLongitude;
    }

    private Stop(Parcel in) {
        id = in.readInt();
        agencyId = in.readString();
        stopId = in.readString();
        stopCode = in.readString();
        name = in.readString();
        description = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeString(agencyId);
        out.writeString(stopId);
        out.writeString(stopCode);
        out.writeString(name);
        out.writeString(description);
        out.writeDouble(latitude);
        out.writeDouble(longitude);
    }

    @Override
    public String toString() {
        return String.format("%s - %s", stopCode, name);
    }

    public String getStopId() {
        return stopId;
    }

    public String getStopCode() {
        return stopCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public static final Parcelable.Creator<Stop> CREATOR
            = new Parcelable.Creator<Stop>() {
        @Override
        public Stop createFromParcel(Parcel in) {
            return new Stop(in);
        }

        @Override
        public Stop[] newArray(int size) {
            return new Stop[size];
        }
    };

}