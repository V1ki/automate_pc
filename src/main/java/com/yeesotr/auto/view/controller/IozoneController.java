package com.yeesotr.auto.view.controller;

import com.yeesotr.auto.android.Automation;
import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.appium.AppiumManager;
import com.yeesotr.auto.env.Environment;
import io.appium.java_client.MobileElement;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Dialog;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.File;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IozoneController implements Initializable,DeviceOperator {

    public TextArea consoleArea;
    public TextField optionText;

    private Device currentDevice ;
    private Socket mSocket = null;
    private ScheduledFuture<?> mScheduledFuture = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    private void session() {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(mSocket.getInputStream());

            while (true) {

                    String s = dis.readUTF();
                    Platform.runLater(()->{
                        consoleArea.appendText(s) ;
                    });
                Thread.sleep(10);
            }
        } catch (Exception e) {
//            e.printStackTrace();
        } finally {
            try {
                mSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mSocket = null;
        }
    }

    public void installApp(Device device){
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().clear();
        dialog.getDialogPane().setPrefWidth(300);
        dialog.getDialogPane().setPrefHeight(300);
        dialog.setTitle("???????????????????????????...");

        final VBox vb = new VBox();

        final ProgressIndicator pin = new ProgressIndicator();
        pin.setProgress(-1);

        vb.setAlignment(Pos.CENTER);
        vb.getChildren().add(pin);
        dialog.getDialogPane().setContent(vb);
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            public Boolean call() {
                // ????????????????????????.

                device.preInstallApp("com.yeestor.iozone",
                        Environment.APK_DIR+ File.separator+"iozone-release.apk");

                device.grantPermission("com.yeestor.iozone","android.permission.READ_EXTERNAL_STORAGE");
                device.grantPermission("com.yeestor.iozone","android.permission.WRITE_EXTERNAL_STORAGE");
                return true;
            }
        };

        task.setOnRunning((e) -> {
            log.info("OnRunning!");
            dialog.show();
        });
        task.setOnSucceeded((e) -> {
            log.info("OnSucceeded!");
            dialog.setResult(true);
            dialog.close();

            // process return value again in JavaFX thread
        });
        task.setOnFailed((e) -> {
            log.info("OnFailed:{}",e);

            dialog.setResult(false);
            dialog.close();
            // eventual error handling by catching exceptions from task.get()
        });
        new Thread(task).start();
    }


    public void setDevice(Device device){
        currentDevice = device ;
        // ????????????.
        // ????????????????????????iozone?????????App
        log.info("installedApp: {}", device.getInstalledApp());
        if(!device.getInstalledApp().contains("com.yeestor.iozone")){
            // install app
            installApp(currentDevice);
        }

        // ?????????????????????,??????????????????Device ???????????????,??????,??????????????????Device ???????????????PC??????????????????.
        // ??????????????????,?????????forward,
        // ??????socket

    }

    // ??????????????????
    public void onStartTestClicked(ActionEvent actionEvent) {


        AppiumManager.getInstance().startAppium(currentDevice);

        Automation automation = new Automation(currentDevice);
        automation.pullToForeground("com.yeestor.iozone", ".MainActivity");
        MobileElement iozoneOptionText = automation.waitForPresenceMS(1, "com.yeestor.iozone:id/iozoneOptionText");
        iozoneOptionText.clear();
        iozoneOptionText.setValue("-i0 -r4k -s10m -f /mnt/sdcard/test.bin");


        currentDevice.startIozoneLog();

//        mScheduledFuture = mScheduledThreadPoolExecutor.scheduleAtFixedRate( () -> {
//            if (mSocket == null || !mSocket.isConnected()) {
//                log.debug("??????????????????...");
//                try {
//                    mSocket = new Socket("localhost", currentDevice.getForwardPort());
//                    log.debug("???????????????:" + mSocket.toString());
//                    if(mSocket.isConnected() && !mSocket.isClosed()) {
//                        CompletableFuture.runAsync(this::session);
//                    }
//
//                } catch (Exception e) {
//                    log.info("????????????");
//                }
//            } else {
////                    mLogger.info("??????????????????:???????????????????????????????????????");
//            }
//        }, 0, 3, TimeUnit.SECONDS);


        MobileElement startBtn = automation.waitForPresence(1,"com.yeestor.iozone:id/startBtn");
        startBtn.click();

    }
}
