package com.yeesotr.auto.appium;

import com.yeesotr.auto.android.command.CommandFactory;
import com.yeesotr.auto.android.command.CommandUtils;
import com.yeesotr.auto.android.command.Commander;
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
public class Appium implements Commander.CommanderOutputCallback {

    private int port;
    private Commander commander = CommandFactory.getCommander();
    private AndroidDriver<MobileElement> driver;
    private Device connectedDevice;
    private boolean isStarted ;
    private boolean isConnected;

    public Appium() throws Exception {
        this.port = CommandUtils.findAvailablePort(11013);
        commander.addOutputCallback(this);
    }

    @Override
    public void onNewline(String newLine) {
        if(newLine.contains("Appium REST http interface listener started on")) {
            isStarted = true;
        }
        else if(newLine.contains("Could not start REST http interface listener")){
            // 如果没打开成功,就换个接口再试试
            try {
                this.port = CommandUtils.findAvailablePort(11013);
            } catch (Exception e) {
                e.printStackTrace();
            }
            start();
        }
    }

    public void start() {

        log.info("Start Appium with port:" + port);

        if (Environment.isWindows()) {
            commander.executeCommand("set JAVA_HOME=" + Environment.JAVA_HOME);
            commander.executeCommand("set ANDROID_HOME=" + Environment.ANDROID_HOME);
        } else {
            commander.executeCommand("export ANDROID_HOME=" + Environment.ANDROID_HOME);
            commander.executeCommand("export JAVA_HOME=" + Environment.JAVA_HOME);
        }
        commander.executeCommand("node \"" + Environment.APPIUM_ENTRY + "\" " +
                "--allow-insecure=adb_shell " +
                "--log-timestamp " +
                "--log-level=info " +
                "--port=" + port);
//        commander.executeCommand("sh /Users/v1ki/IdeaProjects/automate_pc/start.sh "+Environment.APPIUM_ENTRY+" "+port +" &");
    }


    public boolean connect(Device device) throws IOException {

        String sdk = device.getApiVersion();
        int platformNo = -1;
        try {
            platformNo = Integer.parseInt(sdk);
        } catch (Exception e) {

        }
        try {
            if (platformNo < 22) {
                initUiAutomator1Driver(device.getSerial());
            } else {
                initDriver(device.getSerial());
            }
        } catch (Exception e) {
//                e.printStackTrace();
            isConnected = false;
            log.warn("device can not connect to device!",e);
            return false;
        }

        log.info("device connect to device success!");
        isConnected = true;

        this.connectedDevice = device;
        this.connectedDevice.setAppium(this);
        return true;
    }


    public void disconnect() {
        if (this.connectedDevice == null || driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }

        isConnected = false;
    }


    public void stop() {
        log.debug(" stop appium[{}]", port);
        if (connectedDevice != null) {
            disconnect();
            connectedDevice.setAppium(null);
        }
        if (Environment.isWindows()) {
            try {
                CommandUtils.execCommandSync("cmd /c \"for /f \"usebackq tokens=5\" %i in (`netstat -ano^|findstr 0.0.0.0:" + port + "`) do taskkill /f /pid %i\"");
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        } else {
            // ps aux | grep '/Applications/Appium.app/Contents/Resources/app/node_modules/appium/build/lib/main.js' | awk '{print $2}' | xargs kill -9
//            commander.executeCommand(" ps aux | grep '" + Environment.APPIUM_ENTRY + "' | grep  port="+this.port+" | awk '{print $2}'");
            try {
                CommandUtils.execCommandSync(500, "/bin/sh", "-c","ps aux",
                        "grep "+ Environment.APPIUM_ENTRY ,
                        "grep port="+this.port,
                        "awk '{print $2}'"
                        );
            } catch (IOException e) {
                e.printStackTrace();
            }
//            commander.executeCommand(" ps aux | grep '" + Environment.APPIUM_ENTRY + "' | grep  port="+this.port+" | awk '{print $2}' | xargs kill -9");

        }

//        commander.close();

    }


    public void initDriver(String udid) throws Exception {
        log.debug("init appium driver with UiAutomator2 - udid: {}", udid);
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability("platformName", "Android");
        desiredCapabilities.setCapability("udid", udid);
        desiredCapabilities.setCapability("newCommandTimeout", 24 * 3600 + "");
        desiredCapabilities.setCapability("autoAcceptAlerts",true);
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


    private void initUiAutomator1Driver(String udid) throws Exception {
        log.debug("init appium driver with UiAutomator1 - udid: {}", udid);
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability("platformName", "Android");
        //automationName=UiAutomator1
        desiredCapabilities.setCapability("automationName", "UiAutomator1");
//        desiredCapabilities.setCapability("platformVersion", "4.4.2");
        desiredCapabilities.setCapability("udid", udid);
//        desiredCapabilities.setCapability("deviceName", deviceName);
//        desiredCapabilities.setCapability("appPackage", "com.andromeda.androbench2");
        desiredCapabilities.setCapability("appPackage", "com.android.settings");
        desiredCapabilities.setCapability("appActivity", ".Settings");
        desiredCapabilities.setCapability("newCommandTimeout", "600");
        desiredCapabilities.setCapability("autoAcceptAlerts",true);
//        desiredCapabilities.setCapability("ensureWebviewsHavePages", true);

        URL remoteUrl = new URL("http://localhost:" + port + "/wd/hub");

        driver = new AndroidDriver<>(remoteUrl, desiredCapabilities);
    }

}
