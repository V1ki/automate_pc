package com.yeesotr.auto.android;

import io.appium.java_client.android.AndroidDriver;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.URL;

@Slf4j
class CommandUtilsTest {

    @RepeatedTest(1000)
    void execCommandSync() throws Exception{

//        AndroidSerialPort androidSerialPort = new AndroidSerialPort("COM3");
//        boolean isOpen = androidSerialPort.openPort();
//        Assertions.assertEquals(isOpen, true);
//        String ip = androidSerialPort.getIpAddr();
//        log.info("ip:" + ip);
//        Assertions.assertEquals(ip, "172.18.18.178");


//        androidSerialPort.setAdbTcpPort(5555);
        String result = CommandUtils.execCommandSync("cmd /c C:\\Users\\BugsWan\\Desktop\\automation\\Android\\platform-tools\\adb.exe get-state") ;
        log.info("result:\n {}",result);
        Assertions.assertEquals(result, "device");

//        androidSerialPort.closePort() ;
    }



    @Test
    void execCommandAsync() throws Exception{
        //$ENV:ANDROID_HOME="D:\Android\Sdk"; $ENV:JAVA_HOME="D:\Java\jdk1.8.0_251";
        CommandUtils.execCommandAsync("appium.cmd --allow-insecure=adb_shell --port="+10089);
    }

    @Test
    void findAvailablePort() throws Exception{
        CommandUtils.findAvailablePort(4723);
//        CommandUtils.execCommandAsync("netstat -ano | findstr 4723");
    }

    @Test
    void testExecCommand()throws Exception{
        CommandUtils commandUtils = new CommandUtils() ;
        int port = CommandUtils.findAvailablePort(4723);
        commandUtils.executeCommand("set ANDROID_HOME=D:\\Android\\Sdk");
        commandUtils.executeCommand("set JAVA_HOME=D:\\Java\\jdk1.8.0_251");
        commandUtils.executeCommand("appium.cmd --allow-insecure=adb_shell --port="+port);

        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability("platformName", "Android");
        //automationName=UiAutomator1
        URL remoteUrl = new URL("http://localhost:"+port+"/wd/hub");

        AndroidDriver driver = new AndroidDriver<>(remoteUrl, desiredCapabilities);
    }
}