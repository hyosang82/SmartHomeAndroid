package kr.hyosang.smarthome.serviceclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import kr.hyosang.smarthome.IServiceInterface;
import kr.hyosang.smarthome.common.Logger;
import kr.hyosang.smarthome.service.SmartHomeService;
import kr.hyosang.smarthome.wattmeter.WhMeterVO;

/**
 * Created by Hyosang on 2016-01-07.
 */
public class ServiceManager {
    public static final int MSG_WATT_MEASURED = 0x01;

    private static ServiceManager mInstance = null;

    private IServiceInterface mService = null;

    private List<ServiceListener> mListeners = new ArrayList<ServiceListener>();

    private ServiceManager() {
    }

    public static ServiceManager connect(Context context) {
        context = context.getApplicationContext();

        if(mInstance == null) {
            mInstance = new ServiceManager();
        }

        //start service
        Intent i = new Intent(context.getApplicationContext(), SmartHomeService.class);
        i.putExtra(SmartHomeService.EXTRA_MESSENGER, mInstance.mMessenger);
        context.startService(i);

        //bind service
        context.bindService(i, mInstance.mServiceConnection, Context.BIND_AUTO_CREATE);


        int retryCount = 5;
        while((mInstance.mService == null) && (retryCount > 0)) {
            retryCount--;

            Logger.d("Waiting service connect...");

            try {
                Thread.sleep(300);
            }catch(InterruptedException e) {
                break;
            }
        }

        return mInstance;
    }

    public static ServiceManager getInstance() {
        return mInstance;
    }

    public boolean isConnected() {
        return (mService != null);
    }

    public void disconnect(Context context) {
        context.getApplicationContext().unbindService(mServiceConnection);
    }

    public void addListener(ServiceListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(ServiceListener listener) {
        mListeners.remove(listener);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = IServiceInterface.Stub.asInterface(iBinder);
            Logger.d("Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d("Service disconnected");

        }
    };

    /////////////////////SERVICE METHODS
    public void requestWattMeasure() {
        try {
            if(mService != null) {
                mService.requestWattMeasure();
            }
        }catch(RemoteException e) {
            Logger.e(e);
        }
    }



    /////////////////////SERVICE METHODS END


    private Messenger mMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Method m = null;
            List<Object> args = new ArrayList<Object>();

            switch(msg.what) {
                case MSG_WATT_MEASURED: {
                    WhMeterVO data = (WhMeterVO) getData(WhMeterVO.class, msg);
                    Logger.d(data.toString());
                    try {
                        m = ServiceListener.class.getMethod("onWattMeasured", WhMeterVO.class);
                        args.add(data);
                    }catch(NoSuchMethodException e) {
                    }
                }
                break;
            }

            if(m != null) {
                Object [] arg = args.toArray(new Object[0]);
                for(ServiceListener l : mListeners) {
                    try {
                        m.invoke(l, arg);
                    }catch(IllegalAccessException e) {
                        Logger.e(e);
                    }catch(InvocationTargetException e) {
                        Logger.e(e);
                    }
                }

            }

        }

        private Parcelable getData(Class clazz, Message msg) {
            Bundle bundle = msg.getData();
            bundle.setClassLoader(clazz.getClassLoader());
            return bundle.getParcelable(SmartHomeService.EXTRA_BUNDLE_DATA);
        }
    });
}
