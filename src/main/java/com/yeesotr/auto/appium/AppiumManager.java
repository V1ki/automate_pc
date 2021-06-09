package com.yeesotr.auto.appium;

import com.yeesotr.auto.android.model.Device;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class AppiumManager {

    private final List<Appium> appiumList = new ArrayList<>();
    private static final AppiumManager instance ;
    static {
        instance = new AppiumManager();
    }

    public static AppiumManager getInstance() {
        return instance;
    }

    private AppiumManager(){}



    public void startAppium(Device device) {
        if (device == null || device.getAppium() != null) {
            return;
        }
        try {
            Appium appium = new Appium();
            appium.start();
            // 这里有一个隐患,如果还没启动Appium
            // 等待appium 启动
            int max_wait = 5000 ;
            while (true){
                Thread.sleep(100);
                if(appium.isStarted()){
                    break;
                }
                max_wait -= 100 ;
                if(max_wait <= 0) {
                    log.info("Appium start failed");
                    break;
                }
            }

            boolean isConnected = appium.connect(device);
            if(!isConnected) {
                // 检查设备是否存在?
                if(!device.isAlive()){
                    appium.stop();
                    return;
                }
                startAppium(device);
                return ;
            }
            appiumList.add(appium);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void stopAppium(Device device) {
        if (device == null || device.getAppium() == null) {
            return;
        }
        Appium appium = device.getAppium() ;
        appium.stop();
        appiumList.remove(appium);

    }

    public void stopAll(){
        appiumList.forEach(Appium::stop);
    }

}
