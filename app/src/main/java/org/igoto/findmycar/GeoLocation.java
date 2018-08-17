package org.igoto.findmycar;

import java.util.Date;

/**
 * Created by programer on 2.11.2015.
 */
public class GeoLocation {
    private double longitude = 0;
    private double latitude = 0;
    private Date time = null;
    private String code = "";

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
