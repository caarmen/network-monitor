# Network Monitor Privacy Policy
## Introduction

Network Monitor is a free, hobby app maintained on the personal free time of one developer. The app is [open source](https://github.com/caarmen/network-monitor).
The app was created to fill a personal need of the developer, and was published in the hopes that others might find it useful.
The developer has no interest in, and does not, collect data from its users.

That said, a privacy policy is now a requirement to publish Network Monitor on the Google Play store. In addition to this being a requirement, the developer hopes that users will find this document useful, to understand what kind of data the application collects, and to be aware of how the user can share this data if he or she chooses.

## Collecting data
When you enable the Network Monitor service, your device collects information about your device and network. Some of the data may be considered as "personal data", such as IP addresses. The entire list of data collected locally is listed in the section "Data collected". This data is stored in an internal database, by default accessible only to Network Monitor and unaccessible to other applications.

## Sharing data
Network Monitor never shares or sends data without explicit action from you, the user. You may explicitly choose to share the collected data, as well as additional optional sensitive information for some additional features, by actively using the following features in the application. 

* **Main screen: Share log file.**  You may choose to export some or all of the collected data, depending on the file format.  Network Monitor creates a file and displays a system notification. When you tap on the system notification, you are prompted to choose from a set of applications on your device. The application you choose will receive the file and will share the file. The sending of your data is performed by the application you choose, not by Network Monitor.
* **Advanced options: Speed test: Upload test (FTP).** If you fill in the credentials for the Upload test (FTP), the credentials are stored in the shared preferences of the application, by default unaccessible to other applications (see the point "Export settings" below). If you enter the FTP credentials, and enable the Speed test, Network Monitor will send the file downloaded from the Download test, to the FTP server, at the interval specified by the "Speed test frequency" setting. If you are concerned about privacy, don't do something stupid like change the Download test URL to a file containing sensitive information and fill in FTP server credentials for a server that hosts files publicly. If you do this, your sensitive information file will become available publicly.
* **Advanced options: Send yourself reports by e-mail.** You can choose to make Network Monitor periodically e-mail you a report about the locally collected data. If you want to use this feature, you must enter your e-mail credentials. The e-mail credentials are stored in the shared preferences of the application, by default unaccessible to other applications (see the point "Export settings" below). If you use Gmail to send the reports, you must enable 2-step verification for your Gmail account and create an app password for Network Monitor. You may view instructions on doing this by tapping on "Not receiving e-mails?" in the "Send yourself reports by e-mail" settings screen. If you do not want to set up 2-step authentication, you may go to Gmail settings and enable less secure apps, but this is not recommended! You will see warnings on the Gmail website when you try to enable less secure apps.
* **Advanced options: Export settings.** If you tap on "Export settings", you will be prompted to choose a location to save the settings. You can choose to save the settings on the device, or to share them via an application installed on your device. The application you choose will receive the file and save or share the file. At this point, the file is no longer protected and will be visible to other applications or humans, depending how you chose to save or share the file. *Be Careful*: if you have entered FTP or e-mail credentials, those credentials, including the passwords, are included in the settings file! The full list of settings which are persisted is listed in the section "Application settings".

## Interaction with servers
* Network Monitor does not include any analytics tracking or ads. The developer is not interested in your data. Network Monitor has no back-end server. (Note, this is why you must enter FTP credentials if you want to use the upload test feature).
* To perform the "Socket Connection Test" and "HTTP Connection Test" tests in each report, Network Monitor performs a "ping" of the Google home page, specifically at google.com. Google's web servers will log an incoming connection from your IP address.
  - You can change this server in Advanced options: Server, if you wish to perform the tests connecting to a different server.
  - You can disable these tests altogether by toggling off Advanced options: Enable data connection tests.
* If you enable the speed test (Advanced options: Speed test: Enable speed test), which is disabled by default, then Network Monitor will download a file from GitHub at the interval specified by the "Speed test frequency" setting. GitHub's web servers will log an incoming connection from your IP address. You can always disable the speed test at any time.


## Data collected
When you enable the service, the following data is collected each time Network Monitor adds a log. The data is collected at the interval specified in the "Log frequency" setting on the main screen. The list of these fields is visible in the app from two places:
* Advanced Settings: Fields to monitor
* Log view: Menu: Fields to monitor

The list of data:
* Socket Connection Test: the result of testing a connection to the Google main website.
* HTTP Connection Test: the result of testing a connection to the Google main website.
* [Network Type](https://developer.android.com/reference/android/net/NetworkInfo.html#getTypeName()) Requires the `ACCESS_NETWORK_STATE` permission.
* [Mobile Data Network Type](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getNetworkType())
* [SIM State](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getSimState())
* [Service State](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getServiceState()) Requires the `READ_PHONE_STATE` permission.
* [Detailed State](https://developer.android.com/reference/android/net/NetworkInfo.DetailedState.html) Requires the `ACCESS_NETWORK_STATE` permission.
* [Is Connected](https://developer.android.com/reference/android/net/NetworkInfo.html#isConnected()) Requires the `ACCESS_NETWORK_STATE` permission.
* [Is Roaming](https://developer.android.com/reference/android/net/NetworkInfo.html#isRoaming()) Requires the `ACCESS_NETWORK_STATE` permission.
* [Is Available](https://developer.android.com/reference/android/net/NetworkInfo.html#isAvailable()) Requires the `ACCESS_NETWORK_STATE` permission.
* [Is Failover](https://developer.android.com/reference/android/net/NetworkInfo.html#isFailover()) Requires the `ACCESS_NETWORK_STATE` permission.
* [Data Activity](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getDataActivity())
* [Data State](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getDataState())
* [Reason](https://developer.android.com/reference/android/net/NetworkInfo.html#getReason()) Requires the `ACCESS_NETWORK_STATE` permission.
* [Extra Info](https://developer.android.com/reference/android/net/NetworkInfo.html#getExtraInfo()) Requires the `ACCESS_NETWORK_STATE` permission.
* [WiFi SSID](https://developer.android.com/reference/android/net/wifi/WifiInfo.html#getSSID())
* [WiFi BSSID](https://developer.android.com/reference/android/net/wifi/WifiInfo.html#getBSSID())
* [WiFi Signal Strength (0-4)](https://developer.android.com/reference/android/net/wifi/WifiInfo.html#getRssi())
* [SIM Operator](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getSimOperatorName())
* [SIM MCC](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getSimOperator())
* [SIM MNC](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getSimOperator())
* [Network Operator](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getNetworkOperatorName())
* [Network MCC](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getNetworkOperator())
* [Network MNC](https://developer.android.com/reference/android/telephony/TelephonyManager.html#getNetworkOperator())
* [Is Network Metered](https://developer.android.com/reference/android/net/ConnectivityManager.html#isActiveNetworkMetered()) Requires the `ACCESS_NETWORK_STATE` permission.
  - [Using Google Play Services](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderApi.html#requestLocationUpdates(com.google.android.gms.common.api.GoogleApiClient,+com.google.android.gms.location.LocationRequest,+com.google.android.gms.location.LocationListener))
  - [Not using Google Play Services](https://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(java.lang.String,+long,+float,+android.location.LocationListener))
* [Cell Signal Strength (0-4)](https://developer.android.com/reference/android/telephony/SignalStrength.html) Requires the `READ_PHONE_STATE` permission.
* [Cell Signal Strength (dBm)](https://developer.android.com/reference/android/telephony/SignalStrength.html) Requires the `READ_PHONE_STATE` permission.
* [ASU Level](https://developer.android.com/reference/android/telephony/SignalStrength.html) Requires the `READ_PHONE_STATE` permission.
* [RxQual](https://developer.android.com/reference/android/telephony/SignalStrength.html#getGsmBitErrorRate()) Requires the `READ_PHONE_STATE` permission.
* [LTE RSRQ](https://developer.android.com/reference/android/telephony/SignalStrength.html) Requires the `READ_PHONE_STATE` permission.
* [Network Interface](https://developer.android.com/reference/java/net/NetworkInterface.html#getName())
* [IPv4 Address](https://developer.android.com/reference/java/net/InetAddress.html#getHostAddress())
* [IPv6 Address](https://developer.android.com/reference/java/net/InetAddress.html#getHostAddress())
* [Battery Level](https://developer.android.com/reference/android/content/Intent.html#ACTION_BATTERY_CHANGED)
* [Most consuming app (not collected by default)](https://developer.android.com/reference/android/app/usage/NetworkStatsManager.html#queryDetailsForUid(int,+java.lang.String,+long,+long,+int)) Requires the `READ_PHONE_STATE` permission.
* [Most consuming app data (bytes) (not collected by default)](https://developer.android.com/reference/android/app/usage/NetworkStatsManager.html#queryDetailsForUid(int,+java.lang.String,+long,+long,+int)) Requires the `READ_PHONE_STATE` permission.
* Download Speed (Mbps) (not collected by default)
* Upload Speed (Mbps) (not collected by default)


## Application settings
If you use the "Export settings" feature, you will be able to save a file containing the settings of the app. This file contains the following settings:

Main settings:
* `PREF_SERVICE_ENABLED`: Whether the service is enabled.
* `PREF_UPDATE_INTERVAL`: The interval of the log entries.

Advanced settings:
* `PREF_ENABLE_CONNECTION_TEST`: Whether the Socket Connection Test or HTTP Connection Test should be performed.
* `PREF_SELECTED_COLUMNS`: The columns to display in the log view.
* `PREF_TEST_SERVER`: The server which Network Monitor pings to test network connectivity. By default, this is the Google home page.
* `PREF_WAKE_INTERVAL`: The value of the Advanced Options: Prevent sleep setting.
* `PREF_SCHEDULER`: Indicates whether "Precise timing" or "Save battery" is chosen for the Advanced options: Polling interval setting.
* `PREF_NOTIFICATION_PRIORITY`: The priority of the ongoing notification which is displayed whenever the service is enabled.
* `PREF_NOTIFICATION_ENABLED`: Whether alert notifications should be created when connectivity tests fail.
* `PREF_NOTIFICATION_RINGTONE`: The ringtone for alert notifications.
* `PREF_DB_RECORD_COUNT`: The max number of rows to store in the database at a given time.
* `PREF_THEME`: The theme setting.

Log view settings:
* `PREF_SORT_COLUMN_NAME`: The column by which the log entries are sorted in the log view.
* `PREF_SORT_ORDER`: Whether the data is sorted in ascending or descending order.
* `PREF_FILTER_RECORD_COUNT`: The number of rows to display in the log view.
* `PREF_FREEZE_HTML_TABLE_HEADER`: Whether the table header in the log view is frozen.
* `PREF_FILTERED_VALUES_<column_name>`: Only display rows with this value for the given `column_name`.

File export settings:
* `PREF_KML_EXPORT_COLUMN`: The column to include when exporting to Kml format.
* `PREF_EXPORT_GNUPLOT_SERIES`, `PREF_EXPORT_GNUPLOT_Y_AXIS`: settings for the Gnuplot export format.

E-mail report settings:
* `PREF_EMAIL_INTERVAL`: Interval at which to send e-mail reports (Default: never)
* `PREF_EMAIL_REPORT_FORMATS`: File attachments to include in e-mail reports.
* `PREF_EMAIL_SERVER`: The name of the SMTP e-mail server for e-mail reports.
* `PREF_EMAIL_PORT`: The port of the SMTP e-mail server for e-mail reports.
* :exclamation: **`PREF_EMAIL_USER`**: The username of the SMTP e-mail server for e-mail reports, if you explicitly entered this.
* :exclamation: **`PREF_EMAIL_PASSWORD`**: The password of the account on the SMTP e-mail server for e-mail reports, if you explicitly entered this.
* :exclamation: **`PREF_EMAIL_RECIPIENTS`**: The e-mail addresses of recipients of e-mail reports, if you explicitly entered this.
* `PREF_EMAIL_LAST_EMAIL_SENT`: The last time an e-mail report was sent successfully.
* `PREF_EMAIL_SECURITY`: The security protocol of the SMTP e-mail server for e-mail reports.

Speed test settings:
* `PREF_SPEED_TEST_ENABLED`: Whether speed tests are enabled (disabled by default).
* `PREF_SPEED_TEST_DOWNLOAD_URL`: The file to download for the download speed test.
* `PREF_SPEED_TEST_INTERVAL`: The interval at which to perform speed tests.
* `PREF_SPEED_TEST_UPLOAD_SERVER`: The name of the FTP server for the upload speed test.
* `PREF_SPEED_TEST_UPLOAD_PORT`: The port of the FTP server for the upload speed test.
* :exclamation: **`PREF_SPEED_TEST_UPLOAD_USER`**: The username of the FTP server for the upload speed test, if you explicitly entered this.
* :exclamation: **`PREF_SPEED_TEST_UPLOAD_PASSWORD`**: The password of the account on the FTP server for the upload speed test, if you explicitly entered this.
* `PREF_SPEED_TEST_UPLOAD_PATH`: The path to the file on the FTP server for the upload speed test.
* `PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT_FILE_BYTES`: The size of the file downloaded at the last download speed test.
* `PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT_STATUS`: The result of the last download speed test.
* `PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT_TOTAL_BYTES`: The total bytes downloaded during the last download speed test.
* `PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT_TRANSFER_TIME`: The download time for the last download speed test.
* `show_app_warning`: Whether to show a warning pop-up every time the service is toggled on.
