package com.yeesotr.auto.android;

import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.env.Environment;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidTouchAction;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.touch.TapOptions;
import io.appium.java_client.touch.offset.PointOption;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class Automation {

    private AndroidDriver<MobileElement> driver;

    private Device device ;

    Dimension dim =  null ;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH_mm");

//    private final String resultDir ;

    public Automation(Device device ){
        this.device = device ;
        this.driver = device.getAppium().getDriver() ;
//        resultDir = Environment.RESULTS_DIR + "Test_" + simpleDateFormat.format(new Date())  + File.separator;
//
//        File resultDirFile = new File(resultDir) ;
//        if(!resultDirFile.exists() && !resultDirFile.mkdirs()){
//             log.warn("create test folder failed");
//        }
    }


    public AndroidDriver<MobileElement> getDriver() {
        return driver;
    }

    public void installNeededApp(boolean isAutomator1) {
        if (isAutomator1) {
            preInstallApp("com.andromeda.androbench2", Environment.APK_DIR+File.separator+"com.andromeda.androbench2_4.1.apk");
        } else {
            preInstallApp("com.andromeda.androbench2", Environment.APK_DIR+File.separator+"androbench5.0.1.apk");
        }

        preInstallApp("com.example.sg.h2test", Environment.APK_DIR+File.separator+"H2Test.apk");
        preInstallApp("com.yeestor.tools", Environment.APK_DIR+File.separator+"demo-release.apk");
        preInstallApp("com.silicongo.burntest", Environment.APK_DIR+File.separator+"YS_BIT_V1.32.apk");
    }


    public void terminateApp(String bundleId){
        try {
            driver.terminateApp(bundleId);
        }
        catch (Exception e) {
            log.warn("terminate app error: {} ,try to use am force-stop" ,e.getMessage());

            device.execAdbShell(" am force-stop "+bundleId) ;
        }

    }

    public void grantPermission(String packageName ,String permission){

        HashMap<String,String> args = new HashMap<>();
        args.put("command", "pm grant "+packageName+" "+permission);
//        args.put("args", Lists.newArrayList(command.toString()));
        String output = null ;
        try {
            output = String.valueOf(driver.executeScript("mobile:shell", args));
        }catch (Exception e){
            log.warn(e.getMessage());
        }
        log.info("output:" + output);
    }


    public void removeAllApp() {
        driver.removeApp("com.andromeda.androbench2");
        driver.removeApp("com.example.sg.h2test");
        driver.removeApp("com.silicongo.burntest");
        driver.removeApp("com.yeestor.tools");

    }


    public void captureStorageInfo() throws Exception {

        driver.startActivity(new Activity("com.android.settings", ".Settings$StorageDashboardActivity"));
        screenshot(Environment.RESULTS_DIR+"meminfo.png");
    }

    public boolean checkStatus() {
        try {
            Map<String, Object> status = driver.getStatus();
            log.info("status:{}  --sessionId: {} -- getSessionDetails:{} ",status,driver.getSessionId(),driver.getSessionDetails());
        }catch (Exception e){
            return false;
        }
        return true ;
    }

    public void reconnect(){

            // driver 异常,需要重新连接Appium.
            try {
                device.getAppium().connect(device);
                this.driver = device.getAppium().getDriver() ;

            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public int deleteHalfH2TestFile(String filepath) {
        // 删除一半文件
        //
        Map<String, Object> args = new HashMap<>();
        args.put("command", "ls "+filepath+" | wc -l");
//        args.put("args", Lists.newArrayList(, "| wc -l"));
        String output = String.valueOf(driver.executeScript("mobile:shell", args)).trim();
        log.info("output:" + output);

        if (!output.matches("\\d+")) {
            log.warn("output:" + output + " is not number");
            return 0;
        }

        int count = Integer.parseInt(output);
        int deleteCount = count / 2;

        // command
        //  find filepath \( -name "1[0-1].h2w" -o -name "[0-9].h2w" -o -name "1[2-3].h2w" \)  -exec rm {} +

        StringBuilder command = new StringBuilder("find "+ filepath + " \\( -name \"[0-9].h2w\" ");
        for (int i = 10; deleteCount > i; i += 10) {
            if (deleteCount < i + 10) {
                command.append(" -o -name \"").append(i / 10).append("[0-").append(deleteCount - i).append("].h2w\" ");
            } else {
                command.append(" -o -name \"").append(i / 10).append("[0-9].h2w\" ");
            }
        }
        command.append("\\) -exec rm {} +");

        log.info("command:" + command);
        args = new HashMap<>();
        args.put("command", command.toString());
//        args.put("args", Lists.newArrayList(command.toString()));
        try {
            output = String.valueOf(driver.executeScript("mobile:shell", args));
        }catch (Exception e){
            log.warn(e.getMessage());
        }
        log.info("output:" + output);

        return deleteCount;
    }

    public byte[] readSmartInfo(String filename) {

        try {
            Files.write(Paths.get(Environment.INSTALL_DIR+File.separator+"smart_info"), filename.getBytes());
        } catch (IOException e) {
            log.warn(e.getMessage());
        }


        try {
            driver.pushFile("/data/local/tmp/smart_info", new File(Environment.INSTALL_DIR+File.separator+"smart_info"));
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
//        driver.setClipboardText(filename) ;
        driver.startActivity(new Activity("com.yeestor.tools", ".SmartInfoActivity"));
//        String text = driver.getClipboardText();
        byte[] bytes = null;
        try {
            bytes = driver.pullFile("/data/local/tmp/smart_info_result");
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        driver.terminateApp("com.yeestor.tools");

        if (bytes == null) {
            return null;
        }

        log.debug("bytes:" + bytes.length);
        return bytes;
    }

    /**
     * 检查app 是否安装，如果没有安装的话，就安装
     *
     * @param bundleId app 的包名
     * @param appPath  需要安装的app的路径
     */
    public void preInstallApp(String bundleId, String appPath) {

        if (!driver.isAppInstalled(bundleId)) {
            log.info("start install app " + bundleId);
            driver.installApp(appPath);

            grantPermission(bundleId,"android.permission.READ_EXTERNAL_STORAGE");
            grantPermission(bundleId,"android.permission.WRITE_EXTERNAL_STORAGE");
        }


    }

    public void testBurnTest() {


        pullToForeground("com.silicongo.burntest",".activity.MainActivity");
        if (dim == null) {
            try {
                dim = driver.manage().window().getSize();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

        Optional.ofNullable(waitForPresence(1, "com.android.packageinstaller:id/permission_allow_button")).ifPresent(RemoteWebElement::click);
// com.example.sg.h2test:id/mainTitle
        MobileElement titleElement = waitForPresence(1, "com.silicongo.burntest:id/mainTitle");

        // if license was found , scroll to bottom and click agreen button
        if (titleElement != null && "License".equals(titleElement.getText())) {

            while (true) {
                (new TouchAction<AndroidTouchAction>(driver))
                        .press(PointOption.point(330, dim.height - 100))
                        .moveTo(PointOption.point(330, 200))
                        .release()
                        .perform();

                MobileElement btnAgree = waitForPresenceMS(100, "com.silicongo.burntest:id/btnAgree");
                if (btnAgree != null) {
                    btnAgree.click();
                    break;
                }
            }
        }
        // 等待应用启动完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.warn(e.getMessage());
        }
        log.info("driver.getCurrentPackage():" + driver.getCurrentPackage());
        if ("com.android.documentsui".equals(driver.getCurrentPackage())) {
            MobileElement okBtn = waitForPresence(1, "android:id/button1");
            okBtn.click();
        }
        MobileElement btnSet = waitForPresence(5, "com.silicongo.burntest:id/btnSet");
        btnSet.click();
        MobileElement etTestTime = waitForPresence(5, "com.silicongo.burntest:id/etTestTime");
        etTestTime.clear();
        etTestTime.setValue("24");

        MobileElement btnSetOk = waitForPresence(5, "com.silicongo.burntest:id/btnSetOk");
        btnSetOk.click();
        MobileElement btnStart = waitForPresence(5, "com.silicongo.burntest:id/btnStart");
        btnStart.click();


        //btnTestEnd
        // 一直等到结束
        while (true) {

            try {
                // 1秒钟检测一次
                WebDriverWait wait = new WebDriverWait(driver, 60, 1000);

                MobileElement btnResultOk = (MobileElement) wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.silicongo.burntest:id/btnResultOk")));
                if (btnResultOk != null) {
//                    screenshot("./burntest.png");
//                    screenshot(resultDir+"burnTest_"+simpleDateFormat.format(new Date())+".png");

                    btnResultOk.click();
                    break;
                }

            } catch (Exception e) {
                log.warn(e.getMessage());
            }

        }
    }


    public void testH2Test(long leftInMb) {
        pullToForeground("com.example.sg.h2test",".MainActivity");

        log.info("current application:" + driver.getCurrentPackage() + " - " + driver.currentActivity());
        try {
            Optional.ofNullable(waitForPresence(1, "com.android.packageinstaller:id/permission_allow_button")).ifPresent(RemoteWebElement::click);
            log.info("current application:" + driver.getCurrentPackage() + " - " + driver.currentActivity());


            // com.example.sg.h2test:id/mainTitle
            MobileElement titleElement = waitForPresence(1, "com.example.sg.h2test:id/mainTitle");

            // if license was found , scroll to bottom and click agreen button
            if (titleElement != null && "License".equals(titleElement.getText())) {
                log.info("find License title , try to scroll bottom and click agree btn!");

                while (true) {
                    log.info("scroll down!");
                    (new TouchAction<AndroidTouchAction>(driver))
                            .press(PointOption.point(330, 700))
                            .moveTo(PointOption.point(330, 200))
                            .release()
                            .perform();
                    log.info("try to find agree btn!");
                    MobileElement btnAgree = waitForPresenceMS(100, "com.example.sg.h2test:id/btnAgree");

                    if (btnAgree != null) {
                        log.info("find agree btn , click it!");
                        btnAgree.click();
                        break;
                    }
                }

            }

            //com.example.sg.h2test:id/btnRefresh
            MobileElement btnRefresh = waitForPresence(5, "com.example.sg.h2test:id/btnRefresh");
            btnRefresh.click();
            log.info("find refresh btn ,and click");

            // com.example.sg.h2test:id/radioSomeCap
            MobileElement radioSomeCap = waitForPresence(5, "com.example.sg.h2test:id/radioSomeCap");
            radioSomeCap.click();
            log.info("find radioSomeCap ,and click");

            // 获取剩余容量
            MobileElement remainCap = waitForPresence(5, "com.example.sg.h2test:id/textRemainCap");
            log.info("find remainCap ,and getText:" + remainCap.getText());
            String remainCapMBStr = remainCap.getText().replace("MB", "");
            int remainCaoMBNum = Integer.parseInt(remainCapMBStr);

            // 保留 多少由外部决定
            MobileElement editTestCap = waitForPresence(5, "com.example.sg.h2test:id/editTestCap");
            editTestCap.setValue(String.valueOf(remainCaoMBNum - leftInMb));
            log.info("find editTestCap ,and setValue to :" + editTestCap.getText());


            // 开始读写测试
            MobileElement btnWriteRead = waitForPresence(5, "com.example.sg.h2test:id/btnWriteRead");
            btnWriteRead.click();

            log.info("click WriteRead Btn!");

            //btnTestEnd
            // 一直等到结束
            while (true) {

                try {
                    // 1秒钟检测一次
                    WebDriverWait wait = new WebDriverWait(driver, 60, 1000);

                    MobileElement btnTestEnd = (MobileElement) wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.example.sg.h2test:id/btnTestEnd")));
                    if (btnTestEnd != null && "OK".equals(btnTestEnd.getText())) {
//                        screenshot(resultDir+"h2Test_"+simpleDateFormat.format(new Date())+".png");


                        MobileElement tvInfo = waitForPresence(5, "com.example.sg.h2test:id/tvInfo");
                        log.info("tvInfo.text:" + tvInfo.getText());

                        btnTestEnd.click();
                        break;
                    }

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                // 在循环的最后检查一下是不是还在本app,如果不在的话,就切回来.
                pullToForeground("com.example.sg.h2test",".MainActivity");
            }
            //read smart info
            readSmartInfo("/mnt/sdcard/");


        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    Rectangle rect;

    public void testAndrobench() {
        pullToForeground("com.andromeda.androbench2",".main");
        try {
            log.info("driver.getCurrentPackage():" + driver.getCurrentPackage());
            log.info("driver.currentActivity():" + driver.currentActivity());
            Thread.sleep(1000);

//            driver.startRecordingScreen();
//            log.info(driver.getPageSource());



//            MobileElement el1 = waitForPresence( 10, "android:id/tabs");
//
//            MobileElement tb1 = el1.findElementByXPath("//android.widget.LinearLayout[1]");
//            tb1.click();
//            log.info(" tb1--- " + tb1);

            Capabilities c = driver.getCapabilities();
            log.info(" Capabilities : {}" , c);

            MobileElement el1 = waitForPresence(100, "android:id/tabs");

            if (rect == null) {
                rect = el1.getRect();
            }
            int startX = rect.x;
            int startY = rect.y;
            int width = rect.width;
            int height = rect.height;


            log.info("rect:" + rect.getPoint() + " =" + rect.getDimension());

            new TouchAction<>(driver)

                    .tap(
                            TapOptions
                                    .tapOptions()
                                    .withPosition(
                                            PointOption.point(startX + (width / 8), startY + height / 2)
                                    )
                    ).perform();


            log.info("activateApp androbench2--- ");

            MobileElement el2 = waitForPresence(30, "com.andromeda.androbench2:id/btnStartingBenchmarking");
            el2.click();
            log.info(" found start button and click");

            MobileElement el3 = waitForPresence(30, "android:id/button1");
            el3.click();
            log.info(" found ok button and click");

            log.info("start wait for Send Result showing");
            MobileElement el4;
            do {
                el4 = waitForPresenceUISelector(30,
                        "new UiSelector().text(\"Send Results\").resourceId(\"android:id/alertTitle\")");

                // 在循环的最后检查一下是不是还在本app,如果不在的话,就切回来. 如果不切回来的话，也找不到对应的组件
                pullToForeground("com.andromeda.androbench2",".main");

            } while (el4 == null);
            log.info(" Send Result is found");

            MobileElement el5 = waitForPresence(30, "android:id/button2");
            el5.click();
            log.info(" found cancel button and click");
//            screenshot(resultDir+"androbench_"+simpleDateFormat.format(new Date())+".png");

            List<MobileElement> keys = driver.findElementsByAndroidUIAutomator("new UiSelector().resourceId(\"com.andromeda.androbench2:id/row_testing_name\")");
            List<MobileElement> values = driver.findElementsByAndroidUIAutomator("new UiSelector().resourceId(\"com.andromeda.androbench2:id/row_testing_status\")");

            Map<String, String> datas = new HashMap<>() ;
            for (int i = 0; i < Math.min(keys.size(), values.size()); i++) {
                String key = keys.get(i).getText() ;
                String value = values.get(i).getText() ;
                datas.put(key,value) ;
            }
            log.info("datas:{}",datas);

//            String base64 = driver.stopRecordingScreen();
//            saveBase64ToFile(base64, "test"+System.currentTimeMillis()+".mp4");

        } catch (Exception e) {
            log.warn(e.getMessage());
        }

    }
    @SuppressWarnings("unused")
    public void saveBase64ToFile(String base64, String filename) throws Exception {

        byte[] bytes = Base64.getDecoder().decode(base64);
        Files.write(Paths.get(filename), bytes, StandardOpenOption.CREATE);

    }

    public void pullToForeground(String bundleId,String activity) {

        if (driver.queryAppState(bundleId) != ApplicationState.RUNNING_IN_FOREGROUND) {
            log.info("启动 \"" + bundleId + "\"");
            try{
                driver.activateApp(bundleId);
            }
            catch (Exception e){
                driver.startActivity(new Activity(bundleId,activity));
            }


        }
    }

    @SuppressWarnings("unused")
    public void reboot() {
        Map<String, Object> args = new HashMap<>();
        args.put("command", "reboot");
        driver.executeScript("mobile:shell", args);

    }

    public void screenshot(String filepath) throws Exception {
        File file = driver.getScreenshotAs(OutputType.FILE);
        Files.copy(Paths.get(file.getPath()), Paths.get(filepath), StandardCopyOption.REPLACE_EXISTING);

    }

    public void screenshotNow(){

        try {
            screenshot( "."+File.separator+ "devices"+File.separator+device.getSerial() +File.separator + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date())+".png" );
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }


    public MobileElement waitForPresence(int timeLimitInSeconds, String targetResourceId) {
        return waitForPresence(timeLimitInSeconds, By.id(targetResourceId));
    }


    public MobileElement waitForPresenceMS(int timeLimitInSeconds, String targetResourceId) {
        try {
            driver.manage().timeouts().implicitlyWait(timeLimitInSeconds, TimeUnit.MILLISECONDS);
            return driver.findElementByAndroidUIAutomator("new UiSelector().resourceId(\"" + targetResourceId + "\")");
        } catch (Exception e) {
            log.warn("cannot find element using: {} : {}",targetResourceId, e.getMessage());
//            e.printStackTrace();
            screenshotNow();

            return null;
        }
    }

    public void dismiss(){

        try{
            Alert alert = driver.switchTo().alert() ;
            List<MobileElement> elements = driver.findElements(By.className("android.widget.Button"));
            elements.forEach(RemoteWebElement::click);
            log.debug("call dismiss!");
        }
        catch (Exception e){
            log.info("No Alert Present!");
        }

    }

    public MobileElement waitForPresence(int timeLimitInSeconds, By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, timeLimitInSeconds);

            return (MobileElement) wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            log.warn(e.getMessage());

            screenshotNow();
            return null;
        }
    }

    public MobileElement waitForPresenceUISelector(int timeLimitInSeconds, String selector) {
        // 设置等待时间
        try {
            driver.manage().timeouts().implicitlyWait(timeLimitInSeconds, TimeUnit.SECONDS);
            //"new UiSelector().text(\"Send Results\").resourceId(\"android:id/alertTitle\")"
            return driver.findElementByAndroidUIAutomator(selector);
        } catch (Exception e) {
            log.warn("cannot find element using:" + selector);

            screenshotNow();
            return null;
        }
    }

}
