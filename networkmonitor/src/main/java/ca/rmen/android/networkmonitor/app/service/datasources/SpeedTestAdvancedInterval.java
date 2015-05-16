package ca.rmen.android.networkmonitor.app.service.datasources;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestPreferences;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Reason for it being in datasources is that I need access to NetMonSignalStrength, which is not public
 */
public class SpeedTestAdvancedInterval {
    private static final String TAG = Constants.TAG + SpeedTestAdvancedInterval.class.getSimpleName();

    private SpeedTestPreferences mPreferences;

    private NetMonSignalStrength mNetMonSignalStrength;
    private int mLastSignalStrengthDbm;

    // For fetching data regarding the network such as signal strength, network type etc.
    private static TelephonyManager mTelephonyManager;

    // For finding changes in the signal strength
    private int mOldSignalStrength;
    private int mDifference;

    // For finding changes in the network
    private int mNetworkType;

    // For counting entries since last speedtest was performed
    private int mIntervalCounter;

    SpeedTestAdvancedInterval(Context context) {
        Log.v(TAG, "onCreate");
        mPreferences = SpeedTestPreferences.getInstance(context);
        mIntervalCounter = 0;
        mDifference = 5;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);
        mNetMonSignalStrength = new NetMonSignalStrength(context);
        // Some value that never should be possible so we always updates the first run
        mOldSignalStrength = 255;
        mNetworkType = 255;
    }

    // Need to make sure we do not listen after we are done
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (mTelephonyManager != null) mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    // Determines if the function should do a download test this call
    public boolean doUpdate() {
        Log.v(TAG, "doUpdate() " + mPreferences.getAdvancedSpeedInterval() + " : " + mIntervalCounter );
        if (!mPreferences.isAdvancedEnabled()) {
            return true;
        }
        else {
            // Switch case since I have different types of modes for the speed interval
            int mode = Integer.parseInt(mPreferences.getAdvancedSpeedInterval());
            switch (mode){
                case -2: // check for change in network
                    return changedNetwork();
                case -1:// check for change in network and for a difference in dbm by 5
                    if (changedDbm() || changedNetwork()){
                        return true;
                    }
                    break;
                case 2:
                    mIntervalCounter++;
                    if (2 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 5:
                    mIntervalCounter++;
                    if (5 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 10:
                    mIntervalCounter++;
                    if (10 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 20:
                    mIntervalCounter++;
                    if (20 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 30:
                    mIntervalCounter++;
                    if (30 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 60:
                    mIntervalCounter++;
                    if (60 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 100:
                    mIntervalCounter++;
                    if (100 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 1000:
                    mIntervalCounter++;
                    if (1000 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                default:
                    return false;
            }
            return false;
        }
    }

    private boolean changedNetwork() {
        if (mTelephonyManager.getNetworkType() != mNetworkType ){
            mNetworkType = mTelephonyManager.getNetworkType();
            return true;
        }
        return false;
    }

    private boolean changedDbm() {
        Log.v(TAG, "changedDbm by: " + mDifference + '?');
        if (mLastSignalStrengthDbm != NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            if (mLastSignalStrengthDbm >= mOldSignalStrength + mDifference || mLastSignalStrengthDbm <= mOldSignalStrength - mDifference ) {
                Log.v(TAG,"mOldSignalStrength has been changed from " + mOldSignalStrength + " to " + mLastSignalStrengthDbm);
                mOldSignalStrength = mLastSignalStrengthDbm;
                return true;
            }
        }
        return false;
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Log.v(TAG, "onSignalStrengthsChanged: " + signalStrength);
            mLastSignalStrengthDbm = mNetMonSignalStrength.getDbm(signalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.v(TAG, "onServiceStateChanged " + serviceState);
            if (serviceState.getState() != ServiceState.STATE_IN_SERVICE) {
                mLastSignalStrengthDbm = NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            }
        }
    };

}
