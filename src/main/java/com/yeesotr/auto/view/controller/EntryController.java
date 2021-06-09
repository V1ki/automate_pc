package com.yeesotr.auto.view.controller;

import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.view.main.MainController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class EntryController implements Initializable {
    public Label serialNoLabel;
    public Label ipLabel;
    public Label androidVersionLabel;
    public Label apiVersionLabel;
    public Label productLabel;
    public Label modelLabel;
    public VBox scriptNode;

    @FXML
    ScriptNodeController scriptNodeController ;

    private Device currentDevice;
    private MainController mainController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
    }

    public void init(MainController main){
        mainController = main ;
    }

    public void setDevice(Device device){
        if(device == null){
            return;
        }
        currentDevice = device ;
        scriptNodeController.setDevice(currentDevice);
        Optional<Device> deviceOptional = Optional.of(currentDevice);

        Platform.runLater(()->{

            deviceOptional.map(Device::getSerial).ifPresent(serialNoLabel::setText);
            deviceOptional.map(Device::getIp).ifPresent(ipLabel::setText);
            deviceOptional.map(Device::getAndroidVersion).ifPresent(androidVersionLabel::setText);
            deviceOptional.map(Device::getApiVersion).ifPresent(apiVersionLabel::setText);
            deviceOptional.map(Device::getProduct).ifPresent(productLabel::setText);
            deviceOptional.map(Device::getModel).ifPresent(modelLabel::setText);
        });
    }


    public void onInnerTestClicked(ActionEvent actionEvent) {
        mainController.showTestPlan(currentDevice);
    }

    public void onIozoneClicked(ActionEvent actionEvent) {
        mainController.showIozone(currentDevice);
    }
}
