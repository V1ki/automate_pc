package com.yeesotr.auto.view.main;

import com.yeesotr.auto.android.Automation;
import com.yeesotr.auto.android.DeviceManager;
import com.yeesotr.auto.android.command.CommandUtils;
import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.appium.Appium;
import com.yeesotr.auto.appium.AppiumManager;
import com.yeesotr.auto.env.Environment;
import com.yeesotr.auto.view.controller.EntryController;
import com.yeesotr.auto.view.controller.IozoneController;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class MainController implements Initializable {

    public Button refreshBtn;
    public Button serialBtn;
    public TableView<Device> devicesTableView;
    public TableColumn<Device, String> nameColumn;
    public TableColumn<Device, String> versionColumn;
    public TableColumn<Device, String> statusColumn;
    public AnchorPane testPlan;
    public AnchorPane empty;
    public AnchorPane entry;
    public Pane iozone;

    @FXML
    EntryController entryController;

    @FXML
    TestPlanController testPlanController;

    @FXML
    IozoneController iozoneController;

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    private ObservableList<Device> deviceObservableList;

    private Device selectedDevice;

    private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;
    private ScheduledFuture<?> mScheduledFuture = null;
    Disposable disposable;

    private DeviceManager deviceManager = new DeviceManager();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info(" -- stop all appium");
            executorService.shutdownNow();
            AppiumManager.getInstance().stopAll();
            disposable.dispose();
        }));
        entryController.init(this);
        _initLeftView();
        _initDeviceRefresh();

    }

    private void _initDeviceRefresh() {
        deviceManager.setListener(new DeviceManager.DeviceChangeListener() {
            @Override
            public void onDeviceAttached(Device d) {

                Platform.runLater(() -> deviceObservableList.add(d));
            }

            @Override
            public void onDeviceDetached(Device device) {

                Platform.runLater(() -> {
                    deviceObservableList.remove(device);
                    if (device.getAppium() != null) {
                        device.getAppium().stop();
                    }
                });

            }
        });

        refreshBtn.setOnAction(e -> {
           deviceManager.refresh();
        });


    }

    private <T> void addTooltipToColumnCells(TableColumn<Device, T> column) {

        Callback<TableColumn<Device, T>, TableCell<Device, T>> existingCellFactory
                = column.getCellFactory();

        column.setCellFactory(c -> {
            TableCell<Device, T> cell = existingCellFactory.call(c);

            Tooltip tooltip = new Tooltip();
            // can use arbitrary binding here to make text depend on cell
            // in any way you need:
            tooltip.textProperty().bind(cell.itemProperty().asString());

            cell.setTooltip(tooltip);
            return cell;
        });
    }

    private void _resetRightView() {
        testPlan.setVisible(false);
        empty.setVisible(false);
        entry.setVisible(false);
        iozone.setVisible(false);
    }


    public void showTestPlan(Device device) {
        _resetRightView();
        testPlanController.selectDevice(device);
        testPlan.setVisible(true);
    }

    public void showIozone(Device device) {
        _resetRightView();
        iozoneController.setDevice(device);
        iozone.setVisible(true);
    }

    public void showLoadingDialog(final Device device) {

        Dialog<Device> dialog = new Dialog<>();
        dialog.getDialogPane().getButtonTypes().clear();
        dialog.getDialogPane().setPrefWidth(300);
        dialog.getDialogPane().setPrefHeight(300);
        dialog.setTitle("????????????????????????...");

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
                log.info("device:{}", device);
                device.init();
                log.info("installedApp: {}", device.getInstalledApp());
                if (!device.getInstalledApp().contains("com.yeestor.iozone")) {
                    // install app
                    device.preInstallApp("com.yeestor.iozone",
                            Environment.APK_DIR + File.separator + "iozone-release.apk");

                    device.grantPermission("com.yeestor.iozone", "android.permission.READ_EXTERNAL_STORAGE");
                    device.grantPermission("com.yeestor.iozone", "android.permission.WRITE_EXTERNAL_STORAGE");
                }

                Platform.runLater(() -> dialog.setTitle("?????????????????????..."));
                entryController.setDevice(device);
                return true;
            }
        };

        task.setOnRunning((e) -> {
            log.info("OnRunning!");
            dialog.show();
        });
        task.setOnSucceeded((e) -> {
            log.info("OnSucceeded!");
            dialog.setResult(device);
            dialog.close();

            // process return value again in JavaFX thread
        });
        task.setOnFailed((e) -> {
            log.info("OnFailed:{}", e);

            dialog.setResult(device);
            dialog.close();
            // eventual error handling by catching exceptions from task.get()
        });
        new Thread(task).start();


    }

    private void _initLeftView() {

        deviceObservableList = FXCollections.observableArrayList(new ArrayList<>());
        devicesTableView.setItems(deviceObservableList);

        devicesTableView.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("serial"));

        this.addTooltipToColumnCells(devicesTableView.getColumns().get(0));

        devicesTableView.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("androidVersion"));
        devicesTableView.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("state"));

        devicesTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            log.info("selectedItemProperty select observable:{}  -- oldValue: {} -- newValue: {}", observable, oldValue, newValue);
            if (newValue == null) {
                _resetRightView();
                empty.setVisible(true);
                return;
            }

            showLoadingDialog(newValue);
            _resetRightView();
            entry.setVisible(true);

        });

        devicesTableView.setRowFactory(param -> {
            final TableRow<Device> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();

            MenuItem rebootItem = new MenuItem("??????");
            rebootItem.setOnAction(itemEvent -> {
                Device d = row.getItem();
                d.rebootDevice();
                AppiumManager.getInstance().stopAppium(d);
            });


            MenuItem screenShotItem = new MenuItem("??????");
            screenShotItem.setOnAction(itemEvent -> {
                Device d = row.getItem();
                new Automation(d).screenshotNow();

            });

            rowMenu.getItems().addAll(rebootItem, screenShotItem);

            // only display context menu for non-empty rows:
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu));
            return row;
        });

    }

}
