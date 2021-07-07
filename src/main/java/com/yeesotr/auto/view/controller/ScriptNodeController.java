package com.yeesotr.auto.view.controller;

import com.yeesotr.auto.android.Automation;
import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.appium.AppiumManager;
import com.yeesotr.auto.lua.LogUtils;
import com.yeesotr.auto.lua.command;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class ScriptNodeController implements Initializable, DeviceOperator {
    public TextField pathText;
    public Button detailBtn;
    public Button startBtn;
    public Button stopBtn;



    private Device currentDevice;
    private Automation automation ;
    private Globals globals;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        _toggleStatus(false);
    }

    @Override
    public void setDevice(Device device) {
        /*
         * 理论上来说,不会存在空的设备传入,如果出现了空设备传入,则其他地方有重大bug ,所以此处不做空处理.
         */

        currentDevice = device;
        // 对于脚本测试这个Controller 来说,这个方法被调用就表示已经展示出来了.
        // 在展示的时候,首先判断 当前Device的状态, 如果当前device 的状态已经在测试了,并且有对应的测试脚本了,应该显示出来.
        // 否则需要选择脚本开始测试
        if (device.getScriptPath() != null) {
            pathText.setText(device.getScriptPath());
        }

        if(device.isStarted()) {
            _toggleStatus(true);
        }
        else {

            _toggleStatus(false);
        }


        // 启动对应设备的Appium 服务.
        AppiumManager.getInstance().startAppium(currentDevice);

        automation = new Automation(currentDevice);
        // 启动lua 引擎来解析

        globals = JsePlatform.standardGlobals();
        globals.load(new command(automation));
    }


    /* -------- event callback  start ------------*/

    /**
     * 选择文件的事件回调
     *
     * @param actionEvent
     */
    public void onSelectFileClicked(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Lua Scripts", "*.lua")
        );
        File selectedFile = fileChooser.showOpenDialog(pathText.getScene().getWindow());
        if (selectedFile != null) {
//            mainStage.display(selectedFile);
            pathText.setText(selectedFile.getAbsolutePath());
        }

    }

    /**
     * 开始按钮回调, 在这里启动Appium .
     *
     * @param actionEvent
     */
    public void onStartClicked(ActionEvent actionEvent) {
        /*
         * 首先判断文件是否存在, 如果可以的话,最好检测下lua脚本是否合法
         */
        String path = pathText.getText();
        File scriptFile = new File(path);
        if (!scriptFile.exists()) {
            new Alert(Alert.AlertType.ERROR, "找不到配置测试脚本文件,请检查文件是否存在!").show();
            return;
        }
        if (!automation.checkStatus()) {
            // driver 异常,需要重新连接Appium.
            automation.reconnect();

            // 重连之后,需要重新获取 当前的Driver
            globals = JsePlatform.standardGlobals();
            globals.load(new command(automation));
        }

        _toggleStatus(true);

        new Thread(){
            @Override
            public void run() {

                currentDevice.setStarted(true);
                try {
                    currentDevice.setScriptPath(path);
                    // 启动lua 引擎来解析
                    LuaValue chunk = globals.loadfile(path);
                    log.info("chunk:{}",chunk);
                    chunk.call(LuaValue.valueOf(path));
                    currentDevice.setStarted(false);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                _toggleStatus(false);
                currentDevice.setStarted(false);
            }
        }.start();

//        Task<Boolean> task = new Task<Boolean>() {
//            @Override
//            protected Boolean call() {
//
//                try {
//                    currentDevice.setScriptPath(path);
//                    currentDevice.setStarted(true);
//                    // 启动lua 引擎来解析
//                    LuaValue chunk = globals.loadfile(path);
//                    log.info("chunk:{}",chunk);
//                    chunk.call(LuaValue.valueOf(path));
//                    currentDevice.setStarted(false);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return true;
//            }
//        };
//        task.setOnSucceeded((e) -> {
//            _toggleStatus(false);
//            currentDevice.setStarted(false);
//
//        });
//        task.setOnFailed((e) -> {
//            log.info("OnFailed: {}", e);
//            _toggleStatus(false);
//            currentDevice.setStarted(false);
//        });

//        new Thread(task).start();
    }

    public void onDetailClicked(ActionEvent actionEvent) {
        // 弹出窗口?
        // 一个设备只能允许一个日志窗口
        final String serial = currentDevice.getSerial() ;
        if(LogUtils.containDeviceDisplay(serial)){
            new Alert(Alert.AlertType.WARNING, "一个设备只能打开一个日志窗口!").show();
            return;
        }
        Stage primaryStage = new Stage();
        primaryStage.setTitle(serial+" 's log");
        try {
//            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/views/ConsoleView.fxml")));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ConsoleView.fxml"));
            Parent root = loader.load();
            ConsoleViewController controller = loader.getController();
            Scene scene = new Scene(root,1024,768);
            primaryStage.setScene(scene);
            primaryStage.show();
            controller.setDevice(currentDevice);

            LogUtils.addLogDisplay(serial,controller);

            primaryStage.setOnCloseRequest(e ->{
                LogUtils.removeLogDisplay(serial);
            });

        }catch (Exception e){
            e.printStackTrace();
        }


    }

    /**
     * 结束按钮回调, 在这里关闭Appium
     *
     * @param actionEvent
     */
    public void onStopClicked(ActionEvent actionEvent) {
        currentDevice.getAppium().disconnect();
        _toggleStatus(false);
    }


    /* -------- event callback end ------------*/

    /*---  view appearance ---*/

    /**
     * 将所有的按钮都禁用,如果有新增按钮的话,这里也需要追加处理.
     */
    private void _disableButtons() {
        startBtn.setDisable(true);
        detailBtn.setDisable(false);
        stopBtn.setDisable(true);
    }


    private void _toggleStatus(boolean isStarted) {
        _disableButtons();
        if (isStarted) {
            stopBtn.setDisable(false);
            detailBtn.setDisable(false);
        } else {
            startBtn.setDisable(false);
        }

    }


}
