package ca.rmen.android.networkmonitor.app.service.datasources;

import android.app.usage.ConfigurationStats;
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
    private static final int SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM = 5;
    private static final int SPEED_TEST_INTERVAL_NETWORK_CHANGE = -2;
    private static final int SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE = -1;

    private SpeedTestPreferences mPreferences;

    private NetMonSignalStrength mNetMonSignalStrength;
    private int mLastSignalStrengthDbm;

    // For fetching data regarding the network such as signal strength, network type etc.
    private static TelephonyManager mTelephonyManager;

    // For finding changes in the signal strength
    private int mOldSignalStrength;

    // For finding changes in the network
    private int mNetworkType;

    // For counting entries since last speedtest was performed
    private int mIntervalCounter;

    SpeedTestAdvancedInterval(Context context) {
        Log.v(TAG, "onCreate");
        mPreferences = SpeedTestPreferences.getInstance(context);
        mIntervalCounter = 0;
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
            int mode = Integer.parseInt(mPreferences.getAdvancedSpeedInterval());
            if (mode == SPEED_TEST_INTERVAL_NETWORK_CHANGE) {
                // check for change in network
                return changedNetwork();
            }
            else if (mode == SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE) {
                // check for change in network and for a difference in dbm by 5
                if (changedDbm() || changedNetwork()) {
                    return true;
                }
            }
            else {
                mIntervalCounter++;
                if (mode <= mIntervalCounter) {
                    mIntervalCounter = 0;
                    return true;
                }
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
        Log.v(TAG, "changedDbm by: " + SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM + '?');
        if (mLastSignalStrengthDbm != NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            if (mLastSignalStrengthDbm >= mOldSignalStrength + SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM
                    || mLastSignalStrengthDbm <= mOldSignalStrength - SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM ) {
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
