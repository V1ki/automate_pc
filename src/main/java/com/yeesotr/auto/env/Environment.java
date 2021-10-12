package com.yeesotr.auto.env;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Optional;

@Slf4j
public class Environment {
    public static final String INSTALL_DIR ;
    public static final String ADB ;
    public static final String ANDROID_HOME;
    public static final String JAVA_HOME ;
    public static final String APK_DIR ;

    public static final String APPIUM_ENTRY ;

    public static final String RESULTS_DIR ;

    public static final String OS ;

    public static final String APPIUM_MAIN_FILE = "appium"+File.separator+"build"+File.separator+"lib"+File.separator+"main.js" ;

    static {
        INSTALL_DIR = Optional.ofNullable(System.getProperty("appdir")).orElse(".");//  "C:\\Users\\BugsWan\\Desktop\\automation" ;
        log.debug("System init , appdir:{}", INSTALL_DIR);

        ANDROID_HOME = Optional.ofNullable(System.getenv("ANDROID_HOME")).orElse(INSTALL_DIR+ File.separator+ "Android");
        JAVA_HOME = Optional.ofNullable(System.getenv("JAVA_HOME")).orElse(INSTALL_DIR+ File.separator+ "JDK");
        APK_DIR = INSTALL_DIR+ File.separator+ "apks";

        log.debug("ANDROID_HOME:"+ANDROID_HOME);
        log.debug("JAVA_HOME:"+JAVA_HOME);

        String osName = System.getProperty("os.name");
        OS = osName ;
        String appium_prefix = isMacos() ? "/Applications/Appium.app/Contents/Resources/app/node_modules/" : INSTALL_DIR + File.separator ;

        ADB = ANDROID_HOME + File.separator + "platform-tools" + File.separator + "adb"+ (Optional.ofNullable(osName).orElse("Windows").contains("Windows")  ? ".exe" : "") ;
        APPIUM_ENTRY =  appium_prefix + APPIUM_MAIN_FILE ;
        RESULTS_DIR = INSTALL_DIR+File.separator+"results" + File.separator ;

        log.debug("ADB:"+ADB);
//        File resultsDir = new File(RESULTS_DIR);
//        if(!resultsDir.exists()){
//            if(!resultsDir.mkdirs()){
//                log.warn("Mkdir Results failed");
//            }
//        }

    }

    public static boolean isWindows(){
        return Optional.ofNullable(OS).orElse("Windows").contains("Windows");
    }
    public static boolean isMacos(){
        return Optional.ofNullable(OS).orElse("").contains("Mac OS");
    }
}
