package kr.hyosang.smarthome.wattmeter;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;

/**
 * Created by Hyosang on 2016-01-08.
 */
public class WhMeterVO implements Parcelable {
    public float currentWatt = 0;
    public float monthlyUsedWatt = 0;
    public float currentVoltage = 0;
    public float totalCurrent = 0;
    public long measuredTime;

    public WhMeterVO() {
    }

    public WhMeterVO(Parcel in) {
        currentWatt = in.readFloat();
        monthlyUsedWatt = in.readFloat();
        currentVoltage = in.readFloat();
        totalCurrent = in.readFloat();
        measuredTime = in.readLong();
    }

    public String toString() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(measuredTime);

        StringBuffer sb = new StringBuffer();

        sb.append("현재사용량: ").append(String.format("%.3fW", currentWatt)).append(", ")
                .append("당월누적사용량: ").append(String.format("%.5fkWh", monthlyUsedWatt / 1000f)).append(", ")
                .append("현재전압: ").append(String.format("%.3fV", currentVoltage)).append(", ")
                .append("피상전류: ").append(String.format("%.3fA", totalCurrent)).append(", ")
                .append("측정시간: ").append(c.getTime().toString());

        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(currentWatt);
        parcel.writeFloat(monthlyUsedWatt);
        parcel.writeFloat(currentVoltage);
        parcel.writeFloat(totalCurrent);
        parcel.writeLong(measuredTime);
    }

    public static final Creator<WhMeterVO> CREATOR = new Creator<WhMeterVO>() {
        @Override
        public WhMeterVO createFromParcel(Parcel in) {
            return new WhMeterVO(in);
        }

        @Override
        public WhMeterVO[] newArray(int size) {
            return new WhMeterVO[size];
        }
    };

}