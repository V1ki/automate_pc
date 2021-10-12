package com.yeesotr.auto.android.model;


import com.yeesotr.auto.android.command.CommandUtils;
import com.yeesotr.auto.appium.Appium;
import com.yeesotr.auto.appium.AppiumManager;
import com.yeesotr.auto.env.Environment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Data
@ToString(exclude = {"appium", "ip", "installedApp", "scriptPath", "currentLog", "forwardPort"})
@EqualsAndHashCode(of = { "serial","transportId" })
@Slf4j
public class Device {

    public boolean isAlive() {
        String result = execAdbShell("getprop ro.build.version.release");
        return androidVersion.equals(result);
    }

    enum DeviceFieldType {
        unknown,
        usb,
        product,
        model,
        device,
        transport
    }


    public static final String STATE_UNAUTHORIZED = "unauthorized";
    public static final String STATE_DEVICE = "device";
    public static final String UNKNOWN = "unknown";


    /*
        设备的信息,在设备初始化的时候,从adb 中去获取.
     */
    private String serial;
    private String state;
    private String usb;
    private String product;
    private String model;
    private String device;
    private String ip = UNKNOWN;
    private int transportId;
    private String androidVersion = UNKNOWN;
    private String apiVersion = UNKNOWN;
    private String manufacture;
    private List<String> installedApp = new ArrayList<>();
    private Globals globals = JsePlatform.standardGlobals(); ;


    /**
     * 每个设备都对应着一个单独的Appium server
     */
    private Appium appium;

    /**
     * adb forward的端口.
     * 这个指的是PC上的端口,而不是Android端的,因为Android端的端口是固定的19000
     */
    private Integer forwardPort;

    /**
     * 对于每个设备来说,单次测试只需要执行一个脚本. 只有当开始测试的时候,脚本才会被设置.
     */
    private String scriptPath;

    private boolean isStarted;

    /**
     * 当前测试的log 文件.每次断开后,或者重新启动应用后,就需要重新配置一个文件
     */
    private File currentLog;

    private List<Record> recordList = new ArrayList<>();

    public void connect()throws Exception{
        AppiumManager.getInstance().startAppium(this);
    }

    public static Device convert2Device(String line) {
        String[] datas = line.split("\\s+");
        log.info("line:{}", line);
        Device device = new Device();

        device.serial = datas[0];
        device.state = datas[1];

        for (int i = 2; i < datas.length; i++) {
            device.setValue(datas[i]);
        }

        return device;
    }


    public static List<Device> convert2List(String commandResult) {

        String[] lines = commandResult.split("\n");

        return Arrays.stream(lines)
                .filter(s -> s.contains("device:"))
                .map(Device::convert2Device)
                .collect(Collectors.toList());
    }

    public static List<Device> getConnectedDevices() {
        try {
            String result = CommandUtils.execCommandSync(Environment.ADB + " devices -l");
            return Device.convert2List(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public String getManufacture() {
        if (UNKNOWN.equals(manufacture)) {
            manufacture = this.execAdbShell(" getprop ro.product.vendor.manufacturer");
        }
        return manufacture;
    }

    private void setValue(String value) {
        DeviceFieldType type = getFieldType(value);
        switch (type) {
            case device:
                this.device = value.replace("device:", "");
                break;
            case usb:
                this.usb = value.replace("usb:", "");
                break;
            case product:
                this.product = value.replace("product:", "");
                break;
            case model:
                this.model = value.replace("model:", "");
                break;
            case transport:
                String str = value.replace("transport_id:", "");
                if (str.matches("\\d+")) {
                    this.transportId = Integer.parseInt(str);
                }
                break;
            default:
                break;
        }
    }


    public String getAndroidVersion() {
        if (UNKNOWN.equals(androidVersion)) {
            androidVersion = this.execAdbShell("getprop ro.build.version.release");
        }
        return androidVersion;
    }

    public String getApiVersion() {
        if (UNKNOWN.equals(apiVersion)) {
            apiVersion = this.execAdbShell("getprop ro.build.version.sdk");
        }
        return apiVersion;
    }

    public String getIp() {

        if (UNKNOWN.equals(ip)) {
            String str = this.execAdbShell("ifconfig");
            log.info("ip: {}", str);

            String[] interfaces = str.split("\n\n");
            log.info("interfaces: {}", Arrays.toString(interfaces));

            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < interfaces.length; i++) {
                String interfaceInfo = interfaces[i];
                if (!interfaceInfo.contains("inet addr:")) {
                    break;
                }
                String[] infoLines = interfaceInfo.split("\n");
                Arrays.stream(infoLines)
                        .filter(s -> s.contains("inet addr:"))
                        .findFirst()
                        .map(s -> s.replace("inet addr:", "").trim())
                        .map(s -> s.split(" ")[0])
                        .ifPresent(s -> {
                            stringBuilder.append(s).append("/");
                        });

            }

            if (stringBuilder.length() > 1) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }

            ip = stringBuilder.toString();
        }
        return ip;
    }

    public String execAdb(String command) {

        try {
            return CommandUtils.execCommandSync(Environment.ADB + " -s " + this.serial + " -t " + this.transportId + " " + command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String execAdbShell(String shell) {
        return execAdb("shell " + shell);
    }

    public void fetchListPackages() {
        String result = execAdbShell("pm list packages -f");
        installedApp = Optional.ofNullable(result)
                .map(s -> Arrays.stream(s.split("\n")))
                .map(stream ->
                        stream.map(s -> s.replace("package:", ""))
                                .map(s -> {
                                    String[] strs = s.split("=");
                                    return strs[strs.length - 1];
                                })
                                .collect(Collectors.toList())

                )
                .orElse(new ArrayList<>())
        ;


    }

    public void addRecord(Record record) {
        recordList.add(record);
    }

    public void init() {

        if(currentLog == null){
            currentLog = new File("devices/" + this.serial + "/logs/" + System.currentTimeMillis() + ".log");
        }

        getManufacture();
        getAndroidVersion();
        getApiVersion();
        getIp();
        fetchListPackages();
        // 初始化文件
    }

    //Environment.APK_DIR+File.separator+"H2Test.apk"
    public void preInstallApp(String bundleID, String appPath) {
        log.info("preInstallApp bundleID:{}", bundleID);
        if (installedApp.contains(bundleID)) {
            return;
        }
        this.execAdb(" install -r " + appPath);
    }

    public void grantPermission(String packageName, String permission) {
        this.execAdbShell("pm grant " + packageName + " " + permission);
    }


    public void startIozoneLog() {
        if (this.forwardPort != null) {
            String list = this.execAdb(" forward --list");
            if (list.contains("tcp:" + forwardPort + " tcp:19000")) {
                return;
            }
        }
        try {
            int port = CommandUtils.findAvailablePort(18000);
            this.forwardPort = port;
            this.execAdb(" forward tcp:" + port + " tcp:19000");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void rebootDevice() {
        this.execAdb(" reboot") ;
    }

    private static DeviceFieldType getFieldType(String str) {
        if (str == null) {
            return DeviceFieldType.unknown;
        }
        if (str.contains("device:")) {
            return DeviceFieldType.device;
        }
        if (str.contains("usb:")) {
            return DeviceFieldType.usb;
        }
        if (str.contains("product:")) {
            return DeviceFieldType.product;
        }
        if (str.contains("model:")) {
            return DeviceFieldType.model;
        }
        if (str.contains("transport_id:")) {
            return DeviceFieldType.transport;
        }
        return DeviceFieldType.unknown;
    }
}
