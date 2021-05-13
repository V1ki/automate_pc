package com.yeesotr.auto.android.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@Builder
@EqualsAndHashCode(of={"uuid"})
public class Record {

    private final String uuid = UUID.randomUUID().toString();
    private String startTime;
    private String endTime ;
    private String currentPackage ;
    private String desc ;



}

