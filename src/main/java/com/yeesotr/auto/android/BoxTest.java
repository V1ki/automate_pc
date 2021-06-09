package com.yeesotr.auto.android;

import com.fazecast.jSerialComm.SerialPort;
import com.yeesotr.auto.android.serial.AndroidSerialPort;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class BoxTest {
    public void main(String[] strs) throws  Exception{

        SerialPort[] ports = SerialPort.getCommPorts();


        for (int i = 0; i < ports.length; i++) {
            SerialPort port = ports[i] ;
            log.info("port.getSystemPortName:"+port.getSystemPortName());
            log.info("port.getDescriptivePortName:"+port.getDescriptivePortName());;
            log.info("port.getPortDescription:"+port.getPortDescription());
        }
        AndroidSerialPort port = new AndroidSerialPort("COM3");

        port.openPort() ;


        String commandResult = port.executeCommandSync("ip addr show wlan0",1000);

        String []lines = commandResult.split("\r\n");
        String pattern = "(((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))\\.){3}((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))" ;


        Arrays.stream(lines)
//                .filter( line -> line.matches("^\\d+:.*$"))
                .forEach(log::info);




        String ipLine = Arrays.stream(lines)
                .filter(s -> s.contains("inet"))
                .findFirst()
                .orElse(null);
        String ip = "" ;
        if(ipLine != null) {
            String[] tmps = ipLine.split("/");

            ip = tmps[0].replace("inet", "").trim() ;

            log.info("ip:{}", ip);
        }

        port.executeCommand("setprop service.adb.tcp.port 5555");
        port.executeCommand("stop adbd");
        port.executeCommand("start adbd");

        Thread.sleep(1000);

        String portResult = port.executeCommandSync("getprop service.adb.tcp.port",100);
        log.info("port：{}",portResult);
//
//        ADBCommand remoteDevice = new ADBCommand(ip, 5555);
//        boolean isConnected = remoteDevice.isConnected();
//
//        log.info("isConnected:" + isConnected);
//
//        if(!isConnected){
//            remoteDevice.connect() ;
//        }
//
        Thread.sleep(2000);

        port.closePort();

    }
}
