package kr.hyosang.smarthome.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import kr.hyosang.common.HttpUtil;
import kr.hyosang.smarthome.common.Logger;
import kr.hyosang.smarthome.R;
import kr.hyosang.smarthome.serviceclient.ServiceListener;
import kr.hyosang.smarthome.serviceclient.ServiceManager;
import kr.hyosang.smarthome.wattmeter.WhMeterVO;

/**
 * Created by Hyosang on 2016-01-07.
 */
public class DashboardActivity extends Activity {
    private static final int MSG_TEMP_MEASURE_REQUEST = 0x01;

    private static final int INTERVAL_TEMP = 60 * 1000;     //1min

    private TextView mTxtHour;
    private TextView mTxtMin;
    private TextView mTxtDt;
    private TextView mTxtWatt;
    private TextView mTxtWattUnit;
    private TextView mTxtWattTime;
    private TextView mTxtWattMore;
    private TextView mTxtTemp;
    private TextView mTxtHumi;
    private TextView mTempTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        mTxtHour = (TextView) findViewById(R.id.txt_hour);
        mTxtMin = (TextView) findViewById(R.id.txt_min);
        mTxtDt = (TextView) findViewById(R.id.txt_dt);
        mTxtWatt = (TextView) findViewById(R.id.txt_watt);
        mTxtWattUnit = (TextView) findViewById(R.id.txt_watt_unit);
        mTxtWattTime = (TextView) findViewById(R.id.txt_watt_time);
        mTxtWattMore = (TextView) findViewById(R.id.txt_watt_more);
        mTxtTemp = (TextView) findViewById(R.id.txt_temp);
        mTxtHumi = (TextView) findViewById(R.id.txt_humi);
        mTempTime = (TextView) findViewById(R.id.txt_temp_time);

        findViewById(R.id.btn_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(DashboardActivity.this, SettingActivity.class);
                startActivity(i);
            }
        });

        findViewById(R.id.layout_watt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServiceManager.getInstance().requestWattMeasure();
                Toast.makeText(DashboardActivity.this, "전력사용량 측정 요청", Toast.LENGTH_SHORT).show();
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

        //온습도 체크
        mHandler.sendEmptyMessage(MSG_TEMP_MEASURE_REQUEST);
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

    private void updateTempHumi() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String host = pref.getString("pref_server_ip", "");

        if(host != null && host.length() > 0) {
            String addr = String.format("http://%s/json/live.php", host);
            HttpUtil.HttpData req = HttpUtil.HttpData.createGetRequest(addr);
            req.mListener = new HttpUtil.HttpListener() {
                @Override
                public void onCompleted(HttpUtil.HttpData httpData) {
                    Logger.d(httpData.responseBody);

                    try {
                        JSONObject json = new JSONObject(httpData.responseBody);

                        mTxtTemp.setText(String.format("%.1f℃", json.optDouble("temp")));
                        mTxtHumi.setText(String.format("%.1f%%", json.optDouble("humi")));
                        mTempTime.setText("측정시간 " + json.optString("time_str"));
                    }catch(JSONException e) {
                        Logger.e(e);
                    }
                }
            };
            HttpUtil.getInstance().add(req);
        }
    }

    private ServiceListener mServiceListener = new ServiceListener() {
        @Override
        public void onWattMeasured(WhMeterVO data) {
            String watt = "--.-";
            String unit = "W";

            if(data.currentWatt > 950f) {
                watt = String.format("%.2f", data.currentWatt / 1000f);
                unit = "kW";
            }else {
                watt = String.format("%.1f", data.currentWatt);
                unit = "W";
            }

            mTxtWatt.setText(watt);
            mTxtWattUnit.setText(unit);

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(data.measuredTime);
            int m = c.get(Calendar.MONTH) + 1;
            int d = c.get(Calendar.DAY_OF_MONTH);
            int hh = c.get(Calendar.HOUR_OF_DAY);
            int mm = c.get(Calendar.MINUTE);
            int ss = c.get(Calendar.SECOND);
            String measured = String.format("측정시간 %02d/%02d %02d:%02d:%02d", m, d, hh, mm, ss);
            mTxtWattTime.setText(measured);

            String more = String.format("당월누적 %.4fkWh / 전압 %.1fV / 전류량 %.2fA",
                    data.monthlyUsedWatt / 1000f, data.currentVoltage, data.totalCurrent);
            mTxtWattMore.setText(more);


        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_TEMP_MEASURE_REQUEST: {
                    updateTempHumi();

                    //다음 체크 요청
                    mHandler.sendEmptyMessageAtTime(MSG_TEMP_MEASURE_REQUEST, SystemClock.uptimeMillis() + INTERVAL_TEMP);
                }
                break;
            }
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
