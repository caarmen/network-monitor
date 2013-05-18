/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.networkmonitor.app.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

public class NetMonService extends Service {
	private static final String TAG = Constants.TAG
			+ NetMonService.class.getSimpleName();

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd' 'HH:mm:ss", Locale.US);
	// private static final String HOST = "www.google.com";
	private static final String HOST = "173.194.34.16";
	private static final int PORT = 80;
	private static final int TIMEOUT = 15000;
	private static final String HTTP_GET = "GET / HTTP/1.1\r\n\r\n";
	private static final String UNKNOWN = "";

	private PrintWriter mOutputStream;
	private TelephonyManager mTelephonyManager;
	private ConnectivityManager mConnectivityManager;
	private volatile boolean mDestroyed;


	private static final String[] COLUMNS = new String[] { NetMonColumns.TIMESTAMP,
			NetMonColumns.NETWORK_TYPE, NetMonColumns.MOBILE_DATA_NETWORK_TYPE,
			NetMonColumns.GOOGLE_CONNECTION_TEST, NetMonColumns.SIM_STATE,
			NetMonColumns.DETAILED_STATE, NetMonColumns.IS_CONNECTED, NetMonColumns.IS_ROAMING,
			NetMonColumns.IS_AVAILABLE, NetMonColumns.IS_FAILOVER, NetMonColumns.DATA_ACTIVITY,
			NetMonColumns.DATA_STATE, NetMonColumns.REASON, NetMonColumns.EXTRA_INFO,
			NetMonColumns.IS_NETWORK_METERED, NetMonColumns.CDMA_CELL_BASE_STATION_ID,
			NetMonColumns.CDMA_CELL_LATITUDE, NetMonColumns.CDMA_CELL_LONGITUDE,
			NetMonColumns.CDMA_CELL_NETWORK_ID, NetMonColumns.CDMA_CELL_SYSTEM_ID,
			NetMonColumns.GSM_CELL_ID, NetMonColumns.GSM_CELL_LAC, NetMonColumns.GSM_CELL_PSC };

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		if (!isServiceEnabled()) {
			Log.d(TAG, "onCreate Service is disabled: stopping now");
			stopSelf();
			return;
		}
		Log.d(TAG, "onCreate Service is enabled: starting monitor loop");
		new Thread() {
			@Override
			public void run() {
				try {
					initFile();
				} catch (IOException e) {
					Log.e(TAG, "run Could not init file", e);
					return;
				}
				mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				monitorLoop();
			}
		}.start();
	}

	private boolean isServiceEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				Constants.PREF_SERVICE_ENABLED,
				Constants.PREF_SERVICE_ENABLED_DEFAULT);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	protected void initFile() throws IOException {
		File file = new File(getExternalFilesDir(null), Constants.FILE_NAME_LOG);
		mOutputStream = new PrintWriter(new BufferedWriter(new FileWriter(file,
				true)));
		String header = TextUtils.join(",", COLUMNS);
		mOutputStream.println(header);
		mOutputStream.flush();
	}

	protected void monitorLoop() {
		while (!mDestroyed) {
			ContentValues values = new ContentValues();
			values.put(NetMonColumns.TIMESTAMP, DATE_FORMAT.format(new Date()));
			values.put(NetMonColumns.GOOGLE_CONNECTION_TEST, isNetworkUp() ? "PASS"
					: "FAIL");
			values.putAll(getActiveNetworkInfo());
			values.put(NetMonColumns.MOBILE_DATA_NETWORK_TYPE, getDataNetworkType());
			values.putAll(getCellLocation());
			values.put(NetMonColumns.DATA_ACTIVITY, getDataActivity());
			values.put(NetMonColumns.DATA_STATE, getDataState());
			values.put(NetMonColumns.SIM_STATE, getSimState());
			values.put(NetMonColumns.IS_NETWORK_METERED,
					mConnectivityManager.isActiveNetworkMetered());
			writeValuesToFile(values);

			// Sleep
			long updateInterval = getUpdateInterval();
			Log.d(TAG, "monitorLoop Sleeping " + updateInterval / 1000
					+ " seconds...");
			SystemClock.sleep(updateInterval);

			// Loop if service is still enabled, otherwise stop
			if (!isServiceEnabled()) {
				Log.d(TAG, "onCreate Service is disabled: stopping now");
				stopSelf();
				return;
			}
		}
	}

	private void writeValuesToFile(ContentValues contentValues) {
		Log.d(TAG, "writeValuesToFile " + contentValues);
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < COLUMNS.length; i++) {
			Object value = contentValues.get(COLUMNS[i]);
			String valueString = value == null ? UNKNOWN : String
					.valueOf(value);
			if (valueString.contains(",")) {
				valueString.replaceAll("\"", "\"\"");
				valueString = "\"" + valueString + "\"";
			}
			sb.append(valueString);
			if (i < COLUMNS.length - 1)
				sb.append(",");
			contentValues.put(COLUMNS[i],valueString);
		}
		mOutputStream.println(sb);
		mOutputStream.flush();
		
		getContentResolver().insert(NetMonColumns.CONTENT_URI, contentValues);
	}

	private long getUpdateInterval() {
		String updateIntervalStr = PreferenceManager
				.getDefaultSharedPreferences(this).getString(
						Constants.PREF_UPDATE_INTERVAL,
						Constants.PREF_UPDATE_INTERVAL_DEFAULT);
		long updateInterval = Long.valueOf(updateIntervalStr);
		return updateInterval;
	}

	private boolean isNetworkUp() {
		Socket socket = null;
		try {
			socket = new Socket();
			socket.setSoTimeout(TIMEOUT);
			Log.d(TAG, "isNetworkUp Resolving " + HOST);
			InetSocketAddress remoteAddr = new InetSocketAddress(HOST, PORT);
			InetAddress address = remoteAddr.getAddress();
			if (address == null) {
				Log.d(TAG, "isNetworkUp Could not resolve");
				return false;
			}
			Log.d(TAG, "isNetworkUp Resolved " + address.getHostAddress());
			Log.d(TAG, "isNetworkUp Connecting...");
			socket.connect(remoteAddr, TIMEOUT);
			Log.d(TAG, "isNetworkUp Connected");

			Log.d(TAG, "isNetworkUp Sending GET...");
			OutputStream outputStream = socket.getOutputStream();
			outputStream.write(HTTP_GET.getBytes("utf-8"));
			outputStream.flush();
			Log.d(TAG, "isNetworkUp Sent GET");
			InputStream inputStream = socket.getInputStream();
			Log.d(TAG, "isNetworkUp Reading...");
			int read = inputStream.read();
			Log.d(TAG, "isNetworkUp Read read=" + read);
			return read != -1;
		} catch (Throwable t) {
			Log.d(TAG, "isNetworkUp Caught an exception", t);
			return false;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					Log.w(TAG, "isNetworkUp Could not close socket", e);
				}
			}
		}
	}

	private ContentValues getActiveNetworkInfo() {
		ContentValues values = new ContentValues();

		NetworkInfo activeNetworkInfo = mConnectivityManager
				.getActiveNetworkInfo();
		if (activeNetworkInfo == null)
			return values;
		values.put(NetMonColumns.NETWORK_TYPE, activeNetworkInfo.getTypeName() + "/"
				+ activeNetworkInfo.getSubtypeName());
		values.put(NetMonColumns.IS_ROAMING, activeNetworkInfo.isRoaming());
		values.put(NetMonColumns.IS_AVAILABLE, activeNetworkInfo.isAvailable());
		values.put(NetMonColumns.IS_CONNECTED, activeNetworkInfo.isConnected());
		values.put(NetMonColumns.IS_FAILOVER, activeNetworkInfo.isFailover());
		values.put(NetMonColumns.DETAILED_STATE, activeNetworkInfo.getDetailedState().toString());
		values.put(NetMonColumns.REASON, activeNetworkInfo.getReason());
		values.put(NetMonColumns.EXTRA_INFO, activeNetworkInfo.getExtraInfo());
		return values;
	}

	private String getDataNetworkType() {
		int networkType = mTelephonyManager.getNetworkType();
		switch (networkType) {
		case TelephonyManager.NETWORK_TYPE_1xRTT:
			return "1xRTT";
		case TelephonyManager.NETWORK_TYPE_CDMA:
			return "CDMA";
		case TelephonyManager.NETWORK_TYPE_EDGE:
			return "EDGE";
		case TelephonyManager.NETWORK_TYPE_EHRPD:
			return "EHRPD";
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
			return "EVDO_0";
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
			return "EVDO_A";
		case TelephonyManager.NETWORK_TYPE_EVDO_B:
			return "EVDO_B";
		case TelephonyManager.NETWORK_TYPE_GPRS:
			return "GPRS";
		case TelephonyManager.NETWORK_TYPE_HSDPA:
			return "HSDPA";
		case TelephonyManager.NETWORK_TYPE_HSPA:
			return "HSPA";
		case TelephonyManager.NETWORK_TYPE_HSPAP:
			return "HSPAP";
		case TelephonyManager.NETWORK_TYPE_HSUPA:
			return "HSUPA";
		case TelephonyManager.NETWORK_TYPE_IDEN:
			return "IDEN";
		case TelephonyManager.NETWORK_TYPE_LTE:
			return "LTE";
		case TelephonyManager.NETWORK_TYPE_UMTS:
			return "UMTS";
		case TelephonyManager.NETWORK_TYPE_UNKNOWN:
			return "UNKNOWN";
		default:
			return UNKNOWN;
		}
	}

	private String getDataActivity() {
		int dataActivity = mTelephonyManager.getDataActivity();
		switch (dataActivity) {
		case TelephonyManager.DATA_ACTIVITY_DORMANT:
			return "DORMANT";
		case TelephonyManager.DATA_ACTIVITY_IN:
			return "IN";
		case TelephonyManager.DATA_ACTIVITY_INOUT:
			return "INOUT";
		case TelephonyManager.DATA_ACTIVITY_NONE:
			return "NONE";
		case TelephonyManager.DATA_ACTIVITY_OUT:
			return "OUT";
		default:
			return UNKNOWN;
		}
	}

	private String getDataState() {
		int dataState = mTelephonyManager.getDataState();
		switch (dataState) {
		case TelephonyManager.DATA_CONNECTED:
			return "CONNECTED";
		case TelephonyManager.DATA_CONNECTING:
			return "CONNECTING";
		case TelephonyManager.DATA_DISCONNECTED:
			return "DISCONNECTED";
		case TelephonyManager.DATA_SUSPENDED:
			return "SUSPENDED";
		default:
			return UNKNOWN;
		}
	}

	private String getSimState() {
		int simState = mTelephonyManager.getSimState();
		switch (simState) {
		case TelephonyManager.SIM_STATE_ABSENT:
			return "ABSENT";
		case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
			return "NETWORK LOCKED";
		case TelephonyManager.SIM_STATE_PIN_REQUIRED:
			return "PIN REQUIRED";
		case TelephonyManager.SIM_STATE_PUK_REQUIRED:
			return "PUK REQUIRED";
		case TelephonyManager.SIM_STATE_READY:
			return "READY";
		case TelephonyManager.SIM_STATE_UNKNOWN:
			return "UNKNOWN";
		default:
			return UNKNOWN;
		}
	}

	private ContentValues getCellLocation() {
		ContentValues values = new ContentValues();
		CellLocation cellLocation = mTelephonyManager.getCellLocation();
		if (cellLocation instanceof GsmCellLocation) {
			GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
			values.put(NetMonColumns.GSM_CELL_ID, gsmCellLocation.getCid());
			values.put(NetMonColumns.GSM_CELL_LAC, gsmCellLocation.getLac());
			values.put(NetMonColumns.GSM_CELL_PSC, gsmCellLocation.getPsc());
		} else if (cellLocation instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
			values.put(NetMonColumns.CDMA_CELL_BASE_STATION_ID,
					cdmaCellLocation.getBaseStationId());
			values.put(NetMonColumns.CDMA_CELL_LATITUDE,
					cdmaCellLocation.getBaseStationLatitude());
			values.put(NetMonColumns.CDMA_CELL_LONGITUDE,
					cdmaCellLocation.getBaseStationLongitude());
			values.put(NetMonColumns.CDMA_CELL_NETWORK_ID,
					cdmaCellLocation.getNetworkId());
			values.put(NetMonColumns.CDMA_CELL_SYSTEM_ID,
					cdmaCellLocation.getSystemId());
		}
		return values;
	}

	@Override
	public void onDestroy() {
		mDestroyed = true;
		if (mOutputStream != null)
			mOutputStream.close();
		super.onDestroy();
	}
}
