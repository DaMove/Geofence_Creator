
package com.location.geofencecreator;

import android.content.Context;
import android.content.res.Resources;

import com.google.android.gms.location.GeofenceStatusCodes;

/*
  Merely a helper class with a static method called getErrorMessage
 */
public class GeofenceErrorMessages {
    /**
     * Using singleton pattern to declare private constructor to prevent instantiation.
     */
    private GeofenceErrorMessages() {}

    /**
     * Returns the error string for the geofencing error code.
     */
    public static String getErrorMessage(Context context, int errorCode) {
        Resources mResources = context.getResources();
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return mResources.getString(R.string.geofence_not_available);
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return mResources.getString(R.string.geofence_too_many_geofences);
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return mResources.getString(R.string.geofence_too_many_pending_intents);
            default:
                return mResources.getString(R.string.unknown_geofence_error);
        }
    }
}

