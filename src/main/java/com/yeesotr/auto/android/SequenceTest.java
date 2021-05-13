package com.yeesotr.auto.android;

import com.google.common.collect.Lists;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SequenceTest {
    // 0 for install dir
    public static void main(String[] s) throws  Exception{

        log.info("args: {}", Arrays.toString(s));
        String javaHome = System.getProperty("JAVA_HOME");
        String androidHome = System.getProperty("ANDROID_HOME") ;

        if( (javaHome == null || androidHome == null) && (s == null || s.length < 1)){
            if(javaHome == null){
                log.error("please set JAVA_HOME first!");
                return;
            }
            log.error("please set ANDROID_HOME first!");
            return;
        }

        String installDir = s[0] ;
        javaHome = installDir + File.separator + "JDK";
        androidHome = installDir + File.separator + "Android";


    }
}
