package org.jraf.android.networkmonitor.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class NetMonColumns implements BaseColumns {
	public static final String TABLE_NAME = "networkmonitor";
	public static final Uri CONTENT_URI = Uri
			.parse(NetMonProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

	public static final String _ID = BaseColumns._ID;

	public static final String TIMESTAMP = "timestamp";
	public static final String NETWORK_TYPE = "network_type";
	public static final String MOBILE_DATA_NETWORK_TYPE = "mobile_data_network_type";
	public static final String GOOGLE_CONNECTION_TEST = "google_connection_test";
	public static final String SIM_STATE = "sim_state";
	public static final String DETAILED_STATE = "detailed_state";
	public static final String IS_CONNECTED = "is_connected";
	public static final String IS_ROAMING = "is_roaming";
	public static final String IS_AVAILABLE = "is_available";
	public static final String IS_FAILOVER = "is_failover";
	public static final String DATA_ACTIVITY = "data_activity";
	public static final String DATA_STATE = "data_state";
	public static final String REASON = "reason";
	public static final String EXTRA_INFO = "extra_info";
	public static final String IS_NETWORK_METERED = "is_network_metered";
	public static final String DEVICE_LATITUDE = "device_latitude";
	public static final String DEVICE_LONGITUDE = "device_longitude";
	public static final String CDMA_CELL_BASE_STATION_ID = "cdma_cell_base_station_id";
	public static final String CDMA_CELL_LATITUDE = "cdma_cell_latitude";
	public static final String CDMA_CELL_LONGITUDE = "cdma_cell_longitude";
	public static final String CDMA_CELL_NETWORK_ID = "cdma_cell_network_id";
	public static final String CDMA_CELL_SYSTEM_ID = "cdma_cell_system_id";
	public static final String GSM_CELL_ID = "gsm_cell_id";
	public static final String GSM_CELL_LAC = "gsm_cell_lac";
	public static final String GSM_CELL_PSC = "gsm_cell_psc";

	public static final String DEFAULT_ORDER = _ID;
}