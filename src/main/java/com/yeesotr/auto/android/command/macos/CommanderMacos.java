package com.yeesotr.auto.android.command.macos;

import com.yeesotr.auto.android.command.CommandUtils;
import com.yeesotr.auto.android.command.Commander;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

@Slf4j
public class CommanderMacos implements Commander {


    private final BufferedWriter stdin;
    private Process p = null;
    private final List<CommanderOutputCallback> callbackList = new ArrayList<>();

    public CommanderMacos() {

        // init shell
        ProcessBuilder builder = new ProcessBuilder("sh");
        builder.redirectErrorStream(true);

        try {
            p = builder.start();
        } catch (IOException e) {
            System.out.println(e);
        }
        // get stdin of shell
        stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

        // execute commands

        // write stdout of shell (=output of all commands)
        Scanner scanner = new Scanner(p.getInputStream());
        Thread mThread = new Thread(()->{
            while (scanner.hasNextLine()) {
                String newLine = scanner.nextLine();
                log.info(newLine);

                callbackList.forEach(callback -> {
                    callback.onNewline(newLine);
                });

            }
            log.info("Thread stop!");
        });
        mThread.start();
    }

    public CommanderMacos close(){
        log.info("before scanner quit!");
        try {
            p.destroy();
        }catch (Exception e){
            log.warn(e.getMessage());
        }
        return this;
    }


    public CommanderMacos executeCommand(String command) {
        try {
            // single execution
            stdin.write(command);
            stdin.newLine();
            stdin.flush();
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
        return this ;
    }
    @Override
    public void addOutputCallback(CommanderOutputCallback callback) {
        callbackList.add(callback);
    }

    @Override
    public void removeOutputCallback(CommanderOutputCallback callback) {
        callbackList.remove(callback);
    }
}
