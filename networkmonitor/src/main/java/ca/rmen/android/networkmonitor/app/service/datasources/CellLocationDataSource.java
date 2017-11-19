/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2017 Carmen Alvarez (c@rmen.ca)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.rmen.android.networkmonitor.app.service.datasources;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.util.List;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.PermissionUtil;
import java8.util.stream.StreamSupport;

/**
 * Retrieves information from the current cell we are connected to.
 */
public class CellLocationDataSource implements NetMonDataSource {

    private static final String TAG = Constants.TAG + CellLocationDataSource.class.getSimpleName();
    /**
     * @see CdmaCellLocation#getBaseStationLatitude()
     */
    private static final int CDMA_COORDINATE_DIVISOR = 3600 * 4;
    private TelephonyManager mTelephonyManager;
    private Context mContext;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mContext = context;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues();
        if (!PermissionUtil.hasLocationPermission(mContext)) {
            Log.d(TAG, "No location permissions");
            return values;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fillCellLocation(values);
        } else {
            fillCellLocationV17(values);
        }
        return values;
    }

    private void fillCellLocation(ContentValues values) {
        //noinspection deprecation We only use this deprecated call when the new APIs aren't available or don't work.
        if (PermissionUtil.hasLocationPermission(mContext)) {
            @SuppressLint("MissingPermission") CellLocation cellLocation = mTelephonyManager.getCellLocation();
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
                setGsmCellInfo(values, gsmCellLocation.getCid(), gsmCellLocation.getLac());
            } else if (cellLocation instanceof CdmaCellLocation) {
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
                setCdmaCellInfo(values, cdmaCellLocation.getBaseStationId(), cdmaCellLocation.getBaseStationLatitude(), cdmaCellLocation.getBaseStationLongitude(), cdmaCellLocation.getNetworkId(), cdmaCellLocation.getSystemId());
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void fillCellLocationV17(ContentValues values) {
        if (PermissionUtil.hasLocationPermission(mContext)) {
            @SuppressLint("MissingPermission") List<CellInfo> cellInfos = mTelephonyManager.getAllCellInfo();
            if (cellInfos == null || cellInfos.isEmpty()) {
                fillCellLocation(values);
            } else {
                StreamSupport.stream(cellInfos).filter(CellInfo::isRegistered).forEach(cellInfo -> {
                    if (cellInfo instanceof CellInfoGsm) {
                        CellInfoGsm gsmCellInfo = (CellInfoGsm) cellInfo;
                        CellIdentityGsm cellIdentityGsm = gsmCellInfo.getCellIdentity();
                        setGsmCellInfo(values, cellIdentityGsm.getCid(), cellIdentityGsm.getLac());
                    } else if (cellInfo instanceof CellInfoCdma) {
                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
                        CellIdentityCdma cellIdentityCdma = cellInfoCdma.getCellIdentity();
                        setCdmaCellInfo(values, cellIdentityCdma.getBasestationId(), cellIdentityCdma.getLatitude(), cellIdentityCdma.getLongitude(), cellIdentityCdma.getNetworkId(), cellIdentityCdma.getSystemId());
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && cellInfo instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfo;
                        CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
                        setGsmCellInfo(values, cellIdentityWcdma.getCid(), cellIdentityWcdma.getLac());
                        values.put(NetMonColumns.GSM_CELL_PSC, cellIdentityWcdma.getPsc());
                    } else if (cellInfo instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                        CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
                        values.put(NetMonColumns.LTE_CELL_CI, cellIdentityLte.getCi());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            values.put(NetMonColumns.LTE_CELL_EARFCN, cellIdentityLte.getEarfcn());
                        }
                        values.put(NetMonColumns.LTE_CELL_TAC, cellIdentityLte.getTac());
                        values.put(NetMonColumns.LTE_CELL_PCI, cellIdentityLte.getPci());
                    }
                });
            }
        }
    }

    private void setGsmCellInfo(ContentValues values, int cid, int lac) {
        // The javadoc says the cell id should be less than FFFF, but this
        // isn't always so. We'll report both the full cell id returned by
        // Android, and the truncated one (taking only the last 2 bytes).
        int shortCid = cid > 0 ? cid & 0xFFFF : cid;
        int rnc = cid > 0 ? cid >> 16 & 0xFFFF : 0;
        values.put(NetMonColumns.GSM_FULL_CELL_ID, cid);
        if (rnc > 0) values.put(NetMonColumns.GSM_RNC, rnc);
        values.put(NetMonColumns.GSM_SHORT_CELL_ID, shortCid);
        values.put(NetMonColumns.GSM_CELL_LAC, lac);
    }

    private void setCdmaCellInfo(ContentValues values, int baseStationId, int latitude, int longitude, int networkId, int systemId) {
        values.put(NetMonColumns.CDMA_CELL_BASE_STATION_ID, baseStationId);
        if (latitude < Integer.MAX_VALUE) {
            values.put(NetMonColumns.CDMA_CELL_LATITUDE, (double) latitude / CDMA_COORDINATE_DIVISOR);
        }
        if (longitude < Integer.MAX_VALUE) {
            values.put(NetMonColumns.CDMA_CELL_LONGITUDE, (double) longitude / CDMA_COORDINATE_DIVISOR);
        }
        values.put(NetMonColumns.CDMA_CELL_NETWORK_ID, networkId);
        values.put(NetMonColumns.CDMA_CELL_SYSTEM_ID, systemId);
    }

}
