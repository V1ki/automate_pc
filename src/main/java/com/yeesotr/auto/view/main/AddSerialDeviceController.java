package com.yeesotr.auto.view.main;

import com.fazecast.jSerialComm.SerialPort;
import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.android.serial.AndroidSerialPort;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class AddSerialDeviceController implements Initializable {

    @FXML
    private ComboBox<SerialPort> serialPortComboBox;

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

    private ObservableList<SerialPort> observableList;
    private SerialPort selectedPort;

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        SerialPort[] ports = SerialPort.getCommPorts();

//        List<Device> deviceList = Device.getConnectedDevices();


        observableList = FXCollections.observableArrayList(ports);
        serialPortComboBox.itemsProperty().set(observableList);

        serialPortComboBox.setConverter(new StringConverter<SerialPort>() {
            @Override
            public String toString(SerialPort device) {
                if (device == null) {
                    return "unknown";
                } else {
                    return device.getDescriptivePortName();
                }
            }

            @Override
            public SerialPort fromString(String string) {
                return null;
            }
        });

        serialPortComboBox.setOnAction(event -> {
            SerialPort port = serialPortComboBox.getSelectionModel().getSelectedItem();
            this.selectedPort = port;
            if (selectedPort == null) {
                return;
            }
            AndroidSerialPort androidSerialPort = new AndroidSerialPort(selectedPort);
            androidSerialPort.openPort();
            String ip = androidSerialPort.getIpAddr();
            log.info("ip:" + ip);
            androidSerialPort.setAdbTcpPort(5555);
//            ADBCommand remoteDevice = new ADBCommand(ip, 5555);
//            boolean isConnected = remoteDevice.isConnected();
//
//            log.info("isConnected:" + isConnected);
//
//            if (!isConnected) {
//                remoteDevice.connect();
//            }
            androidSerialPort.closePort() ;



//
//            if(Device.STATE_UNAUTHORIZED.equals(selectedDevice.getState())) {
//                Alert alert = new Alert(Alert.AlertType.WARNING) ;
//                alert.setTitle("设备状态异常");
//                alert.setContentText("您选择的设备["+selectedDevice.getSerial()+"] 尚未授权电脑访问,请先在手机上授权后,再点击刷新按钮!");
//                alert.show();
//            }
//            else{
//                manufacturerTF.setText(selectedDevice.manufacture());
//                try {
//                    sdkVersionTF.setText(selectedDevice.platformNo());
//                    androidVersionTF.setText(selectedDevice.getAndroidVersion());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                modelTF.setText(selectedDevice.getModel());
//
//                productTF.setText(selectedDevice.getProduct());
//
//            }

        });
    }


//    public Device getSelectedDevice() {
//        return selectedDevice;
//    }


    public static Optional<Device> showDialog() throws IOException {

        Dialog<Device> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setTitle("添加USB设备");
        FXMLLoader loader = new FXMLLoader(AddSerialDeviceController.class.getResource("/views/AddSerialDevice.fxml"));

        Parent root = loader.load();
        AddSerialDeviceController controller = loader.getController();


        dialog.setResultConverter(b -> {
            if (b == ButtonType.OK) {


            }
            return null;
        });

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(480);
        return dialog.showAndWait();
    }

}
