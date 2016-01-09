// IServiceInterface.aidl
package kr.hyosang.smarthome;

import kr.hyosang.smarthome.wattmeter.WhMeterVO;

// Declare any non-default types here with import statements

interface IServiceInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void requestWattMeasure();
}
