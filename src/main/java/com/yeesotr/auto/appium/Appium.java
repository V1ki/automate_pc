package com.yeesotr.auto.appium;

import com.yeesotr.auto.android.CommandUtils;
import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.env.Environment;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 一个设备 对应着 一个Appium 的服务端。
 */
@Data
@EqualsAndHashCode(exclude = "connectedDevice")
@Slf4j
public class Appium {

    private final int port ;
    private CommandUtils commander = new CommandUtils();
    private AndroidDriver<MobileElement> driver;
    private Device connectedDevice ;
    private boolean isConnected ;

    public Appium() throws Exception {
        this.port = CommandUtils.findAvailablePort(11013);
    }


    public void start(){
        log.info("Start Appium with port:" + port);
        commander.executeCommand("set ANDROID_HOME="+ Environment.ANDROID_HOME);
        commander.executeCommand("set JAVA_HOME="+Environment.JAVA_HOME);
        commander.executeCommand("node \""+Environment.APPIUM_ENTRY+"\" " +
                "--allow-insecure=adb_shell " +
                "--log-timestamp " +
                "--log-level=info " +
                "--port=" + port);
    }


    public void connect(Device device) throws IOException {
        this.connectedDevice = device ;
        this.connectedDevice.setAppium(this);

        String sdk = device.platformNo() ;
        int platformNo = -1 ;
        try {
            platformNo = Integer.parseInt(sdk) ;
        }catch (Exception e){}
        if(platformNo < 22){
            initUiAutomator1Driver(device.getSerial());
        }
        else {
            initDriver(device.getSerial());
        }
        log.info("device connect to device success!");
        isConnected = true ;

    }


    public void disconnect(){
        if (this.connectedDevice == null || driver == null) {
            return;
        }
        try {
            driver.quit();
        }catch (Exception e){
            log.warn(e.getMessage());
        }

        isConnected = false ;
    }


    public void stop(){
        log.debug(" stop appium[{}]",port);
        if(connectedDevice != null){
            disconnect();
            connectedDevice.setAppium(null);
        }
        commander.close();
        try {

            CommandUtils.execCommandSync("cmd /c \"for /f \"usebackq tokens=5\" %i in (`netstat -ano^|findstr 0.0.0.0:"+port+"`) do taskkill /f /pid %i\"");
        } catch (Exception e) {
            log.warn(e.getMessage());
        }


    }


    public void initDriver(String udid) throws MalformedURLException {
        log.debug("init appium driver with UiAutomator2 - udid: {}",udid);
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability("platformName", "Android");
        desiredCapabilities.setCapability("udid",udid);
        desiredCapabilities.setCapability("newCommandTimeout", "600");
//        desiredCapabilities.setCapability("automationName", "UiAutomator1");
//        desiredCapabilities.setCapability("platformVersion", "8.1.0");
//        desiredCapabilities.setCapability("deviceName", "CM201_2_YS");
//        desiredCapabilities.setCapability("appPackage", "com.andromeda.androbench2");
//        desiredCapabilities.setCapability("appPackage", "com.android.settings");
//        desiredCapabilities.setCapability("appActivity", ".Settings");
//        desiredCapabilities.setCapability("ensureWebviewsHavePages", true);

        URL remoteUrl = new URL("http://localhost:" + port + "/wd/hub");

        driver = new AndroidDriver<>(remoteUrl, desiredCapabilities);
    }


    private void initUiAutomator1Driver(String udid) throws MalformedURLException {
        log.debug("init appium driver with UiAutomator1 - udid: {}",udid);
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability("platformName", "Android");
        //automationName=UiAutomator1
        desiredCapabilities.setCapability("automationName", "UiAutomator1");
//        desiredCapabilities.setCapability("platformVersion", "4.4.2");
        desiredCapabilities.setCapability("udid",udid);
//        desiredCapabilities.setCapability("deviceName", deviceName);
//        desiredCapabilities.setCapability("appPackage", "com.andromeda.androbench2");
        desiredCapabilities.setCapability("appPackage", "com.android.settings");
        desiredCapabilities.setCapability("appActivity", ".Settings");
        desiredCapabilities.setCapability("newCommandTimeout", "600");
//        desiredCapabilities.setCapability("ensureWebviewsHavePages", true);

        URL remoteUrl = new URL("http://localhost:" + port + "/wd/hub");

        driver = new AndroidDriver<>(remoteUrl, desiredCapabilities);
    }

}
