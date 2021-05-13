package com.yeesotr.auto.android.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class AndroidSerialPort implements SerialPortMessageListener {

    private String portID ;

    private SerialPort port ;

    private String command ;

    private StringBuffer commandResult ;

    private boolean isCommandCompleted ;


    public AndroidSerialPort(String portId) {
        this(SerialPort.getCommPort(portId));
    }


    public AndroidSerialPort(SerialPort port) {
        this.port = port ;
        this.portID = port.getSystemPortName() ;
        port.setBaudRate(115200);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        port.setFlowControl(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED);

        isCommandCompleted = false  ;
        commandResult = new StringBuffer();
        port.removeDataListener();
        port.addDataListener(this) ;
    }


    public boolean openPort(){
        return port.openPort() ;
    }

    public boolean closePort(){
        return port.closePort() ;
    }


    public String executeCommandSync(String command, long timeout){
        this.command = command ;
        isCommandCompleted = false ;
        commandResult = new StringBuffer();
        byte [] commands = (command+" \r").getBytes() ;
        port.writeBytes(commands, commands.length) ;
        port.writeBytes(commands, commands.length) ;
        long count = 0 ;
        while (!isCommandCompleted){
            if(count > timeout){
                log.warn("Time out!");
                break;
            }
            try {
                Thread.sleep(10);
                count += 10 ;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return commandResult.toString() ;
    }

    public void executeCommand(String command) {
        byte [] commands = (command+" \r").getBytes() ;
        port.writeBytes(commands, commands.length) ;
    }


    public String getIpAddr(){

        String commandResult = this.executeCommandSync("ip addr show wlan0",10000);

        String []lines = commandResult.split("\r\n");
        String pattern = "(((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))\\.){3}((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))" ;


//        Arrays.stream(lines)
//                .filter( line -> line.matches("^\\d+:.*$"))
//                .forEach(log::info);

        log.info("commandResult:{}", commandResult);
        String ipLine = Arrays.stream(lines)
                .filter(s -> s.contains("inet"))
                .findFirst()
                .orElse(null);
        String ip = null ;
        if(ipLine != null) {
            String[] tmps = ipLine.split("/");

            ip = tmps[0].replace("inet", "").trim() ;

            log.info("ip:{}", ip);
        }
        return ip ;
    }


    public void setAdbTcpPort(int tcpPort){
        this.executeCommand("setprop service.adb.tcp.port "+tcpPort);
        this.executeCommand("stop adbd");
        this.executeCommand("start adbd");
    }

    @Override
    public byte[] getMessageDelimiter() {
        return new byte[]{
                (byte)0x0a
        };
    }

    @Override
    public boolean delimiterIndicatesEndOfMessage() {
        return true;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }
    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] delimitedMessage = event.getReceivedData();
        String line = new String(delimitedMessage) ;
        log.debug("[" + event.getSerialPort().getSystemPortName() + "] - " + line);

        if(line.contains(command) && commandResult.indexOf(command) > -1){
            isCommandCompleted = true ;
//            log.debug("commandResult：{}",commandResult);
        }
        else if(!isCommandCompleted){
            commandResult.append(line);
        }

    }



}
