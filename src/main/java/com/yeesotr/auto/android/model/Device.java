package com.yeesotr.auto.android.model;


import com.yeesotr.auto.android.CommandUtils;
import com.yeesotr.auto.appium.Appium;
import com.yeesotr.auto.env.Environment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@ToString(exclude = {"appium"})
@EqualsAndHashCode(of = "serial")
@Slf4j
public class Device {

    public static final String STATE_UNAUTHORIZED = "unauthorized" ;
    public static final String STATE_DEVICE = "device" ;


    private String serial ;
    private String state ;
    private String product ;
    private String model ;
    private String device ;
    private int transportId ;
    private Appium appium ;

    private List<Record> recordList = new ArrayList<>();

    public static Device convert2Device(String line) {
        String[] datas= line.split("\\s+");
        log.info("line:{}",line);

        Device device = new Device() ;

        device.serial = datas[0] ;
        device.state = datas[1] ;
        if(datas.length == 3){
            String transport_id = datas[2].replace("transport_id:","") ;
            device.transportId = Integer.parseInt(transport_id) ;
            return device ;
        }

        device.product = datas[2].replace("product:","") ;
        device.model = datas[3].replace("model:","") ;
        device.device = datas[4].replace("device:","") ;
        if(datas.length > 5) {
            String transport_id = datas[5].replace("transport_id:", "");
            device.transportId = Integer.parseInt(transport_id);
        }
        return device ;
    }


    public static List<Device> convert2List(String commandResult) {

        String [] lines = commandResult.split("\n");

        List<Device> devices  = Arrays.stream(lines)
                .filter(s -> s.contains("device:"))
                .map(Device::convert2Device)
                .collect(Collectors.toList());
        return devices;
    }

    public static List<Device> getConnectedDevices(){
        try {
            String result = CommandUtils.execCommandSync(Environment.ADB+" devices -l");
            return Device.convert2List(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public String manufacture(){
        try {
            return CommandUtils.execCommandSync(Environment.ADB + " -s "+ this.serial + " shell getprop ro.product.vendor.manufacturer");
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }



    public String getAndroidVersion() throws IOException {
        return CommandUtils.execCommandSync(Environment.ADB+" -s " + this.serial + " shell getprop ro.build.version.release");
    }

    public String platformNo() throws IOException {
        return CommandUtils.execCommandSync(Environment.ADB+" -s " + this.serial + " shell getprop ro.build.version.sdk");
    }

    public void addRecord(Record record){
        recordList.add(record) ;
    }

}
