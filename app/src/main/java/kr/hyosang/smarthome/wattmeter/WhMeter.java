package kr.hyosang.smarthome.wattmeter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kr.hyosang.smarthome.common.Logger;
import kr.hyosang.smarthome.service.SmartHomeService;

/**
 * Created by Hyosang on 2016-01-08.
 */
public class WhMeter {
    private static WhMeter mInstance = null;

    private String mDevAddr = "";
    private CommThread mCommThread = null;
    private BluetoothDevice mDevice = null;
    private WhMeterVO mValue = null;
    private Handler mUpdatedHandler = null;


    private WhMeter() {
    }

    public static WhMeter createInstance(Context context) {
        if(mInstance == null) {
            mInstance = new WhMeter();
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        mInstance.mDevAddr = pref.getString("pref_bt_address", "");

        Logger.d("Watt-hour meter address : " + mInstance.mDevAddr);

        if(mInstance.mDevice == null) {
            mInstance.mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mInstance.mDevAddr);
            mInstance.mDevice.createBond();
        }

        return mInstance;
    }

    public static WhMeter getInstance() {
        return mInstance;
    }

    public void setHandler(Handler h) {
        mUpdatedHandler = h;
    }

    public void query() {
        if(mCommThread != null && mCommThread.isAlive()) {
            Logger.d("Already running thread");
            return;
        }else {
            mCommThread = new CommThread();
            mCommThread.start();
        }

    }

    public WhMeterVO getValue() {
        return mValue;
    }


    private class CommThread extends Thread {
        private ReceiverThread mReceiver = null;
        private SenderThread mSender = null;

        private WhMeterVO threadValue = new WhMeterVO();

        @Override
        public void run() {
            try {
                BluetoothSocket socket = mDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                socket.connect();

                Logger.d("CONNECTED?" + socket.isConnected());

                mReceiver = new ReceiverThread(socket.getInputStream());
                mSender = new SenderThread(socket.getOutputStream());

                mReceiver.start();
                mSender.start();

                //시작
                mSender.send("rt0");

                synchronized(this) {
                    this.wait();
                }

                mSender.interrupt();
                mReceiver.interrupt();

                socket.close();

                Logger.d(mValue.toString());

                if(mUpdatedHandler != null) {
                    mUpdatedHandler.sendEmptyMessage(SmartHomeService.MSG_WATT_MEASURED);
                }

            }catch(IOException e) {
                Logger.e(e);
            }catch(InterruptedException e) {
                Logger.e(e);
            }
        }

        public void onReceived(String body) {
            Pattern p = Pattern.compile("([A-Z]{2})([0-9])([0-9]+)");
            Matcher m = p.matcher(body);

            if(m.matches()) {
                String cmd = m.group(1);
                int value = Integer.parseInt(m.group(3), 10);

                if("RT".equals(cmd)) {
                    //init.
                    mSender.send("rb0");
                }else if("RB".equals(cmd)) {
                    //현재소비전력. RB800780905 = 780.905W
                    threadValue.currentWatt = (float)value / 1000f;

                    mSender.send("rf0");
                }else if("RF".equals(cmd)) {
                    //당월누적사용량. RF9001576681 = 15.76681kWh
                    threadValue.monthlyUsedWatt = (float)value / 100f;

                    mSender.send("rc0");
                }else if("RC".equals(cmd)) {
                    //전압. RC800219154 = 219.154V
                    threadValue.currentVoltage = (float)value / 1000f;

                    mSender.send("rd0");
                }else if("RD".equals(cmd)) {
                    //피상전류. RD800004414 = 4.414A
                    threadValue.totalCurrent = (float)value / 1000f;

                    //측정완료
                    threadValue.measuredTime = System.currentTimeMillis();

                    mValue = threadValue;

                    synchronized (CommThread.this) {
                        notifyAll();
                    }
                }else {
                    Logger.d("Unknown body = " + body);
                }
            }else {
                Logger.d("Pattern not matched = " + body);
            }
        }
    }

    private class ReceiverThread extends Thread {
        private InputStream inputStream = null;
        public Handler mListener = null;

        public ReceiverThread(InputStream is) {
            inputStream = is;
        }

        @Override
        public void run() {
            int b;
            StringBuffer sb = new StringBuffer();

            try {
                while(true) {
                    b = inputStream.read();

                    if (b == 2) {
                        //message start
                        sb.setLength(0);
                    } else if (b == 3) {
                        //message end
                        String reply = sb.toString();
                        if (reply.length() > 0) {
                            Logger.d("Reply Message : " + reply);

                            mCommThread.onReceived(reply);
                        } else {
                            Logger.d("StringBuffer is empty");
                        }
                    } else {
                        //message body
                        sb.append((char) b);
                    }
                }
            }catch(IOException e) {
                Logger.e(e);
            }

        }
    }

    private class SenderThread extends Thread {
        private OutputStream outputStream = null;

        public SenderThread(OutputStream os) {
            outputStream = os;
        }

        public void send(String msg) {
            try {
                msg = ((char) 2) + msg + ((char) 3);
                outputStream.write(msg.getBytes("US-ASCII"));
            }catch(IOException e) {
                Logger.e(e);
            }
        }
    }
}
