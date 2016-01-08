package kr.hyosang.smarthome.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import kr.hyosang.smarthome.R;

public class SettingActivity extends PreferenceActivity {
    private ListPreference mBtAddrPref;

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

        mBtAddrPref = (ListPreference) findPreference("pref_bt_address");


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
