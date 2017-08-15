/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;

/**
 * Retrieve the network interface names and IP addresses, of all network interfaces which are up and which are not a loopback interface.
 */
public class NetworkInterfaceDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + NetworkInterfaceDataSource.class.getSimpleName();

    @Override
    public void onCreate(Context context) {}

    @Override
    public void onDestroy() {}

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues result = new ContentValues(1);
        try {
            // It's possible for the device to have multiple interfaces up at a given time.  
            // This can happen on "normal" phones when switching between WiFi and 3G, 
            // or on some devices which have multiple interfaces up all the time.

            // We'll save the name and addresses of all interfaces.  If there happen to 
            // be multiple ones, we'll return a delimited list.
            // In most cases, we should have one interface name, one IP v4 address, and one IP v6 address.
            List<String> interfaceNames = new ArrayList<>();
            List<String> ipv4Addresses = new ArrayList<>();
            List<String> ipv6Addresses = new ArrayList<>();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (isValidNetworkInterface(networkInterface)) {
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    if (inetAddresses.hasMoreElements()) interfaceNames.add(networkInterface.getName());
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if (inetAddress instanceof Inet6Address) ipv6Addresses.add(inetAddress.getHostAddress());
                        else
                            ipv4Addresses.add(inetAddress.getHostAddress());
                    }
                }
            }
            result.put(NetMonColumns.NETWORK_INTERFACE, TextUtils.join(";", interfaceNames));
            result.put(NetMonColumns.IPV4_ADDRESS, TextUtils.join(";", ipv4Addresses));
            result.put(NetMonColumns.IPV6_ADDRESS, TextUtils.join(";", ipv6Addresses));
        } catch (SocketException e) {
            Log.e(TAG, "getContentValues Could not retrieve NetworkInterfaces:  " + e.getMessage(), e);
        }
        return result;
    }

    private boolean isValidNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        return networkInterface.isUp() && !networkInterface.isLoopback();
    }

}
