package kr.hyosang.smarthome.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;

import kr.hyosang.smarthome.common.Logger;
import kr.hyosang.smarthome.R;
import kr.hyosang.smarthome.serviceclient.ServiceListener;
import kr.hyosang.smarthome.serviceclient.ServiceManager;
import kr.hyosang.smarthome.wattmeter.WhMeterVO;

/**
 * Created by Hyosang on 2016-01-07.
 */
public class DashboardActivity extends Activity {
    private TextView mTxtHour;
    private TextView mTxtMin;
    private TextView mTxtDt;
    private TextView mTxtWatt;
    private TextView mTxtWattUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        mTxtHour = (TextView) findViewById(R.id.txt_hour);
        mTxtMin = (TextView) findViewById(R.id.txt_min);
        mTxtDt = (TextView) findViewById(R.id.txt_dt);
        mTxtWatt = (TextView) findViewById(R.id.txt_watt);
        mTxtWattUnit = (TextView) findViewById(R.id.txt_watt_unit);

        findViewById(R.id.btn_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(DashboardActivity.this, SettingActivity.class);
                startActivity(i);
            }
        });

        (new Thread() {
            @Override
            public void run() {
                ServiceManager service = ServiceManager.connect(DashboardActivity.this);
                service.addListener(mServiceListener);
                if (service.isConnected()) {
                    Logger.d("Service connect OK");
                } else {
                    Logger.d("Service connect failed");
                }
            }
        }).start();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(mReceiver, filter);

        updateDateTime();
    }

    @Override
    protected void onDestroy() {
        ServiceManager.getInstance().removeListener(mServiceListener);
        ServiceManager.getInstance().disconnect(this);

        unregisterReceiver(mReceiver);


        super.onDestroy();
    }

    private void updateDateTime() {
        Calendar c = Calendar.getInstance();
        int h = c.get(Calendar.HOUR);
        int m = c.get(Calendar.MINUTE);
        int mm = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DAY_OF_MONTH);

        if(c.get(Calendar.HOUR_OF_DAY) == 12) {
            h = 12;
        }

        mTxtHour.setText(String.valueOf(h));
        mTxtMin.setText(String.format("%02d", m));
        mTxtDt.setText(String.format("%02d/%02d", mm, d));
    }

    private ServiceListener mServiceListener = new ServiceListener() {
        @Override
        public void onWattMeasured(WhMeterVO data) {
            String watt = String.format("%.1f", data.currentWatt);
            mTxtWatt.setText(watt);
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(Intent.ACTION_TIME_TICK.equals(action)) {
                updateDateTime();
            }
        }
    };
}
