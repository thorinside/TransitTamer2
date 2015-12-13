package org.nsdev.apps.transittamer.model;

/**
 * Created by neal on 2015-11-28.
 */
public class Stop {

    private int mId;
    private String mAgencyId;
    private String mStopId;
    private String mStopCode;
    private String mName;
    private String mDescription;
    private double mLatitude;
    private double mLongitude;
    private String mNickName;

    public Stop() {
        // Required
    }

    public static Stop fromLegacy(org.nsdev.apps.transittamer.legacy.Stop legacyStop) {
        Stop stop = new Stop();
        stop.setId(legacyStop.getId());
        stop.setStopId(legacyStop.getStopId());
        stop.setStopCode(legacyStop.getStopCode());
        stop.setAgencyId(legacyStop.getAgencyId());
        stop.setName(legacyStop.getName());
        stop.setNickName(legacyStop.getNickName());
        stop.setDescription(legacyStop.getDescription());
        stop.setLatitude(legacyStop.getLatitude());
        stop.setLongitude(legacyStop.getLongitude());
        return stop;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getAgencyId() {
        return mAgencyId;
    }

    public void setAgencyId(String agencyId) {
        mAgencyId = agencyId;
    }

    public String getStopId() {
        return mStopId;
    }

    public void setStopId(String stopId) {
        mStopId = stopId;
    }

    public String getStopCode() {
        return mStopCode;
    }

    public void setStopCode(String stopCode) {
        mStopCode = stopCode;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public String getNickName() {
        return mNickName;
    }

    public void setNickName(String nickName) {
        mNickName = nickName;
    }
}
