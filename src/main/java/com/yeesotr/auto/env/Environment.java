package com.yeesotr.auto.env;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class Environment {
    public static final String INSTALL_DIR ;
    public static final String ADB ;
    public static final String ANDROID_HOME;
    public static final String JAVA_HOME ;
    public static final String APK_DIR ;

    public static final String APPIUM_ENTRY ;

    public static final String RESULTS_DIR ;

    static {
        INSTALL_DIR = System.getProperty("appdir");//  "C:\\Users\\BugsWan\\Desktop\\automation" ;
        log.debug("System init , appdir:{}", INSTALL_DIR);

        ANDROID_HOME = INSTALL_DIR+ File.separator+ "Android";
        JAVA_HOME = INSTALL_DIR+ File.separator+ "JDK";
        APK_DIR = INSTALL_DIR+ File.separator+ "apks";
        ADB = ANDROID_HOME + File.separator + "platform-tools" + File.separator + "adb.exe" ;
        APPIUM_ENTRY =  INSTALL_DIR + File.separator+"appium\\build\\lib\\main.js" ;
        RESULTS_DIR = INSTALL_DIR+File.separator+"results" + File.separator ;
        File resultsDir = new File(RESULTS_DIR);
        if(!resultsDir.exists()){
            resultsDir.mkdirs() ;
        }

    }
}
