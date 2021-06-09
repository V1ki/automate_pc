package com.yeesotr.auto.services;

import com.yeesotr.auto.android.model.Device;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DeviceLogSocket {

    private Socket socket ;
    private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor = null;
    private ScheduledFuture<?> mScheduledFuture = null;
    private int port = 18000 ;


    DeviceLogSocket(Device device) throws IOException {
        initSchedule();
    }

    private void initSchedule(){
        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        mScheduledFuture = mScheduledThreadPoolExecutor.scheduleAtFixedRate(this::breath, 0, 3, TimeUnit.SECONDS);
    }

    private void breath(){
        if (socket == null || !socket.isConnected()) {
            log.info("尝试建立连接...");
            try {
                socket = new Socket("localhost", port);
                log.info("建立新连接:" + socket.toString());
                if(socket.isConnected() && !socket.isClosed()) {
                    CompletableFuture.runAsync(this::handleReceiveMsg);
                }

            } catch (Exception e) {
                log.info("连接异常");
            }
        } else {
//                    log.info("连接心跳检测:当前已经建立连接，无需重连");
        }
    }

    private void handleReceiveMsg(){
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(socket.getInputStream());

            while (true) {

                String s = dis.readUTF();
                System.out.print(s);
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            socket = null;
        }
    }


}
