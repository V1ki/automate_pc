package com.yeesotr.auto.android.command;

import com.yeesotr.auto.android.command.macos.CommanderMacos;

import java.util.Optional;

public class CommandFactory {


    public static Commander getCommander(){
        boolean isWindows = Optional.ofNullable(System.getProperty("os.name")).orElse("Windows").contains("Windows");
        if(isWindows){
            return new CommandUtils();
        }
        return new CommanderMacos();
    }

}
