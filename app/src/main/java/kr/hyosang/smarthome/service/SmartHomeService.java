package kr.hyosang.smarthome.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import kr.hyosang.smarthome.IServiceInterface;
import kr.hyosang.smarthome.serviceclient.ServiceManager;
import kr.hyosang.smarthome.common.Logger;
import kr.hyosang.smarthome.wattmeter.WhMeter;
import kr.hyosang.smarthome.wattmeter.WhMeterVO;

/**
 * Created by Hyosang on 2016-01-07.
 */
public class SmartHomeService extends Service {
    public static final String EXTRA_MESSENGER = "extra_messenger";
    public static final String EXTRA_BUNDLE_DATA = "extra_bundle_data";

    public static final int WATT_MEASURE_INTERVAL = 30 * 1000;  //30 seconds

    public static final int MSG_WATT_MEASURED = 0x01;
    public static final int MSG_DO_WATT_MEASURE = 0x02;

    private Messenger mClient = null;

    private WhMeter mWhMeter = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mWhMeter = WhMeter.createInstance(this);
        mWhMeter.setHandler(mHandler);

        measureWatt();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            Messenger messenger = (Messenger) intent.getExtras().get(EXTRA_MESSENGER);
            mClient = messenger;
        }else {
            mClient = null;
        }

        return START_NOT_STICKY;
    }

    private void sendClient(int what, Parcelable data) {
        if(mClient != null) {
            try {
                Message msg = Message.obtain();
                msg.what = what;
                msg.getData().putParcelable(EXTRA_BUNDLE_DATA, data);
                mClient.send(msg);
            }catch(RemoteException e) {
                Logger.e(e);
            }
        }
    }

    private void measureWatt() {
        mWhMeter.query();

        //요청에 실패할수도 있으므로 여기서 다음 측정메세지를 추가한다.
        if(!mHandler.hasMessages(MSG_DO_WATT_MEASURE)) {
            mHandler.sendEmptyMessageAtTime(MSG_DO_WATT_MEASURE, SystemClock.uptimeMillis() + WATT_MEASURE_INTERVAL);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_WATT_MEASURED: {
                    WhMeterVO val = mWhMeter.getValue();
                    sendClient(ServiceManager.MSG_WATT_MEASURED, val);
                }
                break;

                case MSG_DO_WATT_MEASURE: {
                    measureWatt();
                }
                break;
            }
        }
    };

    private final IServiceInterface.Stub mBinder = new IServiceInterface.Stub() {
        @Override
        public void requestWattMeasure() throws RemoteException {
            mWhMeter.query();
        }
    };


}
