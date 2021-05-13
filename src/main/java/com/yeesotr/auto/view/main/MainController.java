package com.yeesotr.auto.view.main;

import com.yeesotr.auto.android.Automation;
import com.yeesotr.auto.android.CommandUtils;
import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.android.model.Record;
import com.yeesotr.auto.appium.Appium;
import com.yeesotr.auto.env.Environment;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class MainController implements Initializable {

    public Button startTestBtn;
    public Button refreshBtn;
    public Button serialBtn;
    public TableView<Device> devicesTableView;
    public TableColumn<Device, String> nameColumn;
    public TableColumn<Device, String> versionColumn;
    public TableColumn<Device, String> statusColumn;

    public AnchorPane rightPanel;
    public TableView<Record> testProgressTable;
    public Button stopTestBtn;

    private List<Appium> appiumList = new ArrayList<>();

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    private ObservableList<Device> deviceObservableList;
    private ObservableList<Record> recordObservableList = FXCollections.observableArrayList();

    private Device selectedDevice;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info(" -- stop all appium");
            executorService.shutdownNow();

            appiumList.forEach(Appium::stop);
        }));

        _initLeftView();
        _initRightView();
    }


    private void _initLeftView() {

        refreshBtn.setOnAction(e -> {


            List<Device> deviceList = Device.getConnectedDevices();
            log.info("deviceList:{}",deviceList);

            deviceList.forEach(d -> {
                if(!deviceObservableList.contains(d)){
                    deviceObservableList.add(d) ;
                }
            });
            deviceObservableList.forEach(d-> {
                if(!deviceList.contains(d)){
                    deviceObservableList.remove(d); ;
                }
            });

        });
/*
        serialBtn.setOnAction(event -> {
            try {
                Optional<Device> device = AddSerialDeviceController.showDialog();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/


        List<Device> deviceList = Device.getConnectedDevices();
        deviceObservableList = FXCollections.observableArrayList(deviceList) ;
        devicesTableView.setItems(deviceObservableList);

        devicesTableView.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("serial"));
        devicesTableView.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("androidVersion"));
        devicesTableView.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("state"));


        devicesTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            log.info("selectedItemProperty select newValue:{}", newValue);
            this.selectedDevice = newValue;
            startTestBtn.setDisable(this.selectedDevice != null && selectedDevice.getAppium() != null && selectedDevice.getAppium().isConnected());
            if (selectedDevice != null) {
                recordObservableList.addAll(selectedDevice.getRecordList());
            }
        });

        devicesTableView.setRowFactory(param -> {
            final TableRow<Device> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();

            MenuItem rebootItem = new MenuItem("重启");
            rebootItem.setOnAction(itemEvent -> {
                Device d = row.getItem();
                CommandUtils.rebootDevice(d.getSerial());
                Appium appium = d.getAppium();
                if(appium != null){
                    appium.stop();
                }
                appiumList.remove(appium);
            });
            rowMenu.getItems().addAll(rebootItem);

            // only display context menu for non-empty rows:
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu));
            return row;
        });

    }

    private void _initRightView() {

        testProgressTable.setItems(recordObservableList);

        testProgressTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("startTime"));
        testProgressTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("endTime"));
        testProgressTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("desc"));


        _initStartTestButton();

        stopTestBtn.setDisable(true);
        stopTestBtn.setOnAction(event -> {
            // 停止测试。

            Appium appium = selectedDevice.getAppium();
            appium.stop();
            appiumList.remove(appium);

            stopTestBtn.setDisable(true);
            startTestBtn.setDisable(false);
        });
    }


    private void _initStartTestButton() {
        startTestBtn.setDisable(true);
        startTestBtn.setOnAction(event -> {

            startTestBtn.setDisable(true);
            stopTestBtn.setDisable(false);

            testProgressTable.setVisible(true);


            executorService.execute(() -> {
                final Device device = selectedDevice;


                if(device.getAppium() == null){
                    try {
                        Appium appium = new Appium();
                        appium.start();
                        appium.connect(device);
                        appiumList.add(appium);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                startTest(device);

                startTestBtn.setDisable(false);
                stopTestBtn.setDisable(true);

            });

        });
    }


    public void startTest(Device device){
        Automation automation = new Automation(device);
        if (!device.getAppium().isConnected()) {
            try {
                device.getAppium().connect(device);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yy/MM/dd HH:mm:ss");

            String time = dateFormat.format(new Date());

            Record record = Record.builder()
                    .desc("重装应用！")
                    .startTime(dateFormat.format(new Date()))
                    .build();
            recordObservableList.add(record);
            automation.removeAllApp();

            Thread.sleep(1000);

            String sdk = device.platformNo();
            int platformNo = Integer.parseInt(sdk);

            automation.installNeededApp(platformNo < 22);
            log.info("install needed app!");
            record.setEndTime(dateFormat.format(new Date()));

//            automation.terminateApp("com.andromeda.androbench2");
//            automation.terminateApp("com.example.sg.h2test");
//            automation.terminateApp("com.silicongo.burntest");


            record = Record.builder()
                    .desc("删除测试文件夹")
                    .startTime(dateFormat.format(new Date()))
                    .build();
            recordObservableList.add(record);
            try {
//                Map<String, Object> args = new HashMap<>();
//                args.put("command", "rm");
//                args.put("args", Lists.newArrayList("-r", "/mnt/sdcard/h2Test", "/mnt/sdcard/burn"));
//                String output = String.valueOf(driver.executeScript("mobile:shell", args)).trim();
//
                CommandUtils.execCommandSync("cmd /c "+ Environment.ADB +" -s "+ device.getSerial() + " shell rm -r /mnt/sdcard/h2Test");
                CommandUtils.execCommandSync("cmd /c "+ Environment.ADB +" -s "+ device.getSerial() + " shell rm -r /mnt/sdcard/burn");

//                log.info(output);
            } catch (Exception e) {
                // e.printStackTrace();
            }
            record.setEndTime(dateFormat.format(new Date()));
//                    device.addRecord(Record.builder().startTime(time).endTime(dateFormat.format(new Date())).build());


            log.info("start test Androbench!");
            for (int i = 0; i < 5; i++) {

                time = dateFormat.format(new Date());
                log.info("start test Androbench ! 0 - {}", i);
                record = Record.builder()
                        .desc(" SLC 性能 - " + (i + 1) + "/5")
                        .startTime(time)
                        .build();

                recordObservableList.add(record);
                automation.testAndrobench();

                record.setEndTime(dateFormat.format(new Date()));
                log.info("test Androbench ! 0 - {} completed", i);
            }

            time = dateFormat.format(new Date());
            record = Record.builder()
                    .desc("满盘H2 ")
                    .startTime(time)
                    .build();
            recordObservableList.add(record);

            log.info("start test H2Test left 200MB!");
            automation.testH2Test(200);
            log.info("test H2Test left 200Mb completed!");
            record.setEndTime(dateFormat.format(new Date()));

            time = dateFormat.format(new Date());
            record = Record.builder()
                    .desc("删除一半H2文件")
                    .startTime(time)
                    .build();
            recordObservableList.add(record);

            automation.deleteHalfH2TestFile("/mnt/sdcard/h2Test");
            record.setEndTime(dateFormat.format(new Date()));


            for (int i = 0; i < 5; i++) {
                time = dateFormat.format(new Date());
                log.info("start test Androbench ! 0 - {}", i);
                record = Record.builder()
                        .desc(" TLC 性能 - " + (i + 1) + "/5")
                        .startTime(time)
                        .build();

                recordObservableList.add(record);
                automation.testAndrobench();

                record.setEndTime(dateFormat.format(new Date()));
                log.info("test Androbench ! 1 - {} completed", i);
            }

            record = Record.builder()
                    .desc(" H2数据填充 ")
                    .startTime(time)
                    .build();

            recordObservableList.add(record);
            log.info("start test H2Test left 3GB!");
            automation.testH2Test(3 * 1024);
            log.info("test H2Test left 3GB completed!");
            record.setEndTime(dateFormat.format(new Date()));


            record = Record.builder()
                    .desc(" BIT老化 ")
                    .startTime(time)
                    .build();

            recordObservableList.add(record);
            log.info("start test BurnTest! TIME: " + new Date());
            automation.testBurnTest();
            log.info("test BurnTest completed!" + new Date());

            record.setEndTime(dateFormat.format(new Date()));

            automation.terminateApp("com.silicongo.burntest");


            for (int i = 0; i < 5; i++) {
                time = dateFormat.format(new Date());
                log.info("start test Androbench ! 0 - {}", i);
                record = Record.builder()
                        .desc(" 老化后性能 - " + (i + 1) + "/5")
                        .startTime(time)
                        .build();

                recordObservableList.add(record);
                automation.testAndrobench();

                record.setEndTime(dateFormat.format(new Date()));
                log.info("test Androbench ! 1 - {} completed", i);
            }


        } catch (Exception e) {
            log.warn("Test with exception:{}",e.getMessage());
        }
    }
}
