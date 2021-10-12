package com.yeesotr.auto.android;

import com.yeesotr.auto.android.model.Device;
import com.yeesotr.auto.lua.command;
import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * 用于管理目前正在运行的任务
 * @author bugs.wan
 * @since 1.0.3
 * @version 1.0.3
 */
@Slf4j
public class TaskManager {

    private final ThreadPoolExecutor executor ;
    private DeviceManager deviceManager ;

    public TaskManager(){

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(availableProcessors, 10, 200, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(5));
    }


    public void startTask(Device device,final String luaPath){

        // TODO: 设备管理 更新设备的状态
        // TODO: Lua 引擎来处理这个设备的测试任务
        // 启动lua 引擎来解析

        executor.execute(() -> {
            deviceManager.runDeviceTask(device, luaPath);
        });

    }

}
