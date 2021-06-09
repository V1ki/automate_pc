package com.yeesotr.auto.lua;

import com.yeesotr.auto.android.model.Device;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class LogUtils {

    public interface LogDisplay {
        void info(String time, String tag, String msg);
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
    public static final String SEPARATOR = "|_:_|";
    private static final List<LogDisplay> displayList = new ArrayList<>();
    private static final HashMap<String, LogDisplay> displayMap = new HashMap<>();

    public static void writeLog( Device device, String tag, String msg) {
        File file = device.getCurrentLog();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        String time = dateFormat.format(new Date());

        String content = time + SEPARATOR + tag + SEPARATOR + msg + "\n";

        Optional.ofNullable(displayMap.get(device.getSerial()))
                .ifPresent(d -> {
                    d.info(time, tag, msg);
                });
        ;

//        FileInputStream fileInputStream = new FileInputStream(file);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
            fos.write(
                    content.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        log.info("writeLog device:{} - file:{}",device,file);

    }

    /**
     * 设置日志显示回调.
     */
    public static void addLogDisplay(String serial, LogDisplay display) {
        if (displayMap.get(serial) != null) {
            return;
        }
        displayMap.put(serial, display);
    }

    public static void removeLogDisplay(String serial) {
        displayMap.remove(serial);
    }

    public static boolean containDeviceDisplay(String serial) {
        return displayMap.get(serial) != null ;
    }
}
