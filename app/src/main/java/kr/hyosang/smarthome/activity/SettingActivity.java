package kr.hyosang.smarthome.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import kr.hyosang.smarthome.R;

public class SettingActivity extends PreferenceActivity {
    private ListPreference mBtAddrPref;
    private EditTextPreference mServerIp;

    private Map<String, String> mDiscoveryList = new TreeMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_setting);

        findPreference("pref_search_bt").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                adapter.startDiscovery();

                return true;
            }
        });

        findPreference("pref_download").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hyosang82/SmartHomeAndroid/blob/master/bin/app-debug.apk?raw=true"));
                startActivity(i);

                return false;
            }
        });

        mBtAddrPref = (ListPreference) findPreference("pref_bt_address");
        mServerIp = (EditTextPreference) findPreference("pref_server_ip");

        mServerIp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(o.toString());

                return true;
            }
        });

        mBtAddrPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(mBtAddrPref.getEntry());
                return true;
            }
        });

        setLoadedPref();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private void setLoadedPref() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mBtAddrPref.setSummary(pref.getString("pref_bt_address", ""));
        mServerIp.setSummary(pref.getString("pref_server_ip", ""));
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(SettingActivity.this, "BT Scan Started", Toast.LENGTH_SHORT).show();
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(SettingActivity.this, "BT Scan finished", Toast.LENGTH_SHORT).show();

                Set<Map.Entry<String, String>> entries = mDiscoveryList.entrySet();
                String [] nameset = new String[entries.size()];
                String [] valueset = new String[entries.size()];

                int i=0;
                for(Map.Entry<String, String> entry : entries) {
                    nameset[i] = String.format("%s (%s)", entry.getValue(), entry.getKey());
                    valueset[i] = entry.getKey();

                    i++;
                }

                mBtAddrPref.setEntries(nameset);
                mBtAddrPref.setEntryValues(valueset);
            }else if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice dev = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = dev.getName();
                String addr = dev.getAddress();
                Toast.makeText(SettingActivity.this, String.format("FOUND : %s (%s)", name, addr), Toast.LENGTH_SHORT).show();

                mDiscoveryList.put(addr, name);
            }
        }
    };
}
