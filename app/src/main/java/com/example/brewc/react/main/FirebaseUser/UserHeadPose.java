package com.example.brewc.react.main.FirebaseUser;

import com.microsoft.projectoxford.face.contract.HeadPose;

/**
 * Created by brewc on 12/4/2016.
 */

public class UserHeadPose extends HeadPose {
    private double pitch, roll, yaw;

    public UserHeadPose(double pitch, double roll, double yaw) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
    }

    private UserHeadPose() {}

    public double getPitch() {
        return pitch;
    }

    public double getRoll() {
        return roll;
    }

    public double getYaw() {
        return yaw;
    }
}
