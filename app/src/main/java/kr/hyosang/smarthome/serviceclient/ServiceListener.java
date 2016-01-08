package kr.hyosang.smarthome.serviceclient;

import kr.hyosang.smarthome.wattmeter.WhMeterVO;

/**
 * Created by Hyosang on 2016-01-08.
 */
public abstract class ServiceListener {
    public void onWattMeasured(WhMeterVO data) {
    }
}
