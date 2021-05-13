package com.yeesotr.auto.android.command;

import com.yeesotr.auto.android.CommandUtils;
import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.env.Environment;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
public class ADBCommand {


    private String serial;
    private String host;
    private int port;
    private Device device;



    public ADBCommand(@NotNull String serial) {
        this.serial = serial;

        if (serial.contains(":")) {
            String[] tmps = serial.split(":");
            if (tmps.length >= 2) {
                host = tmps[0];
                port = Integer.parseInt(tmps[1]);
            }
        }
    }

    public ADBCommand(@NotNull Device device) {
        this(device.getSerial());
        this.device = device;
    }

    public ADBCommand(String host, int port) {
        this.host = host;
        this.port = port;
        this.serial = host + ":" + port;
    }


    public void connect(){
        try {
            String connectResult = CommandUtils.execCommandSync(Environment.ADB +" connect " + this.serial );
            log.info("result : {} ", connectResult);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected(){
        try {
            String stateResult = CommandUtils.execCommandSync("cmd /c "+ Environment.ADB+" -s " + this.serial + " get-state");
            log.info("stateResult:{} --",stateResult);
            if(!Device.STATE_DEVICE.equals(stateResult)){
                return false ;
            }

            String androidVersion = getAndroidBuildVersion() ;
            log.info("androidVersion:{} -- ",androidVersion);
            if(androidVersion.contains("error")){
                return false ;
            }
            return true ;
        } catch (IOException e) {
            log.warn("isConnected: {}" ,e.getMessage());
            return false;
        }
    }

    public File screenshot() throws Exception{
        //C:\Users\BugsWan\Desktop\automation\Android\platform-tools\adb.exe  exec-out screencap -p > C:\\Users\\BugsWan\\Desktop\\1.png
//        new CommandUtils().executeCommand("adb -s " + this.serial + " exec-out screencap -p > C:\\Users\\BugsWan\\Desktop\\1.png");

        String filename = "C:\\Users\\BugsWan\\Desktop\\1.png" ;
        File file = new File(filename);
        file.delete();
        CommandUtils.execCommandSync("cmd /c "+Environment.ADB+" -s " + this.serial + " exec-out screencap -p > "+filename);
        return file;
    }


    public String getAndroidBuildVersion() throws IOException {
        return CommandUtils.execCommandSync("cmd /c "+ Environment.ADB+" -s " + this.serial + " shell getprop ro.build.version.release");
    }

    public String getAndroidBuildVersionSDK() throws IOException {
        return CommandUtils.execCommandSync("cmd /c "+ Environment.ADB+" -s " + this.serial + " shell getprop ro.build.version.sdk");

    }

    public List<String> getPackages() throws IOException {
        String commandResult = CommandUtils.execCommandSync(Environment.ADB+" -s " + this.serial + " shell pm list packages");
        return Arrays.stream(commandResult.split("\n"))
                .filter(s -> s.contains("package:"))
                .map(s -> s.replace("package:", ""))
                .collect(Collectors.toList());
    }

}
