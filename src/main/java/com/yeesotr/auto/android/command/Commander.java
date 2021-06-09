package com.yeesotr.auto.android.command;

import java.io.IOException;

public interface Commander {

    interface CommanderOutputCallback {
        void onNewline(String newLine);
    }


    Commander executeCommand(String command) ;
    Commander close() ;

    void addOutputCallback(CommanderOutputCallback callback);
    void removeOutputCallback(CommanderOutputCallback callback);

}
