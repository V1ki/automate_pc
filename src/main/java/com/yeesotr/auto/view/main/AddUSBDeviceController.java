package com.yeesotr.auto.view.main;

import com.yeesotr.auto.android.model.Device;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.fxml.Initializable;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


public class AddUSBDeviceController implements Initializable {

    @FXML
    private ComboBox<Device> devicesComboBox;

    @FXML
    private TextField manufacturerTF;

    @FXML
    private TextField sdkVersionTF;

    @FXML
    private TextField modelTF;

    @FXML
    private TextField androidVersionTF;

    @FXML
    private TextField productTF;

    private ObservableList<Device> deviceObservableList;
    private Device selectedDevice ;
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        List<Device> deviceList = Device.getConnectedDevices();


        deviceObservableList = FXCollections.observableList(deviceList);
        devicesComboBox.itemsProperty().set(deviceObservableList);

        devicesComboBox.setConverter(new StringConverter<Device>() {
            @Override
            public String toString(Device device) {
                if(device == null){
                    return "unknown" ;
                }
                else{
                    return device.getSerial() ;
                }
            }

            @Override
            public Device fromString(String string) {
                return null;
            }
        });

        devicesComboBox.setOnAction(event -> {
            Device selectedDevice = devicesComboBox.getSelectionModel().getSelectedItem();
            this.selectedDevice = selectedDevice ;
            if(selectedDevice == null){
                return;
            }


            if(Device.STATE_UNAUTHORIZED.equals(selectedDevice.getState())) {
                Alert alert = new Alert(Alert.AlertType.WARNING) ;
                alert.setTitle("设备状态异常");
                alert.setContentText("您选择的设备["+selectedDevice.getSerial()+"] 尚未授权电脑访问,请先在手机上授权后,再点击刷新按钮!");
                alert.show();
            }
            else{
                manufacturerTF.setText(selectedDevice.getManufacture());
                sdkVersionTF.setText(selectedDevice.getApiVersion());
                androidVersionTF.setText(selectedDevice.getAndroidVersion());
                modelTF.setText(selectedDevice.getModel());

                productTF.setText(selectedDevice.getProduct());

            }

        });
    }


    public Device getSelectedDevice() {
        return selectedDevice;
    }



    public static Optional<Device> showDialog() throws IOException{

        Dialog<Device> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setTitle("添加USB设备");
        FXMLLoader loader = new FXMLLoader(AddUSBDeviceController.class.getResource("/views/AddUSBDevice.fxml"));

        Parent root = loader.load();
        AddUSBDeviceController controller = loader.getController();


        dialog.setResultConverter(b -> {
            if (b == ButtonType.OK) {
                return controller.getSelectedDevice() ;
            }
            return null ;
        });

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(480);
        return dialog.showAndWait();
    }

}
