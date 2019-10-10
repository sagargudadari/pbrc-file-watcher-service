package com.mastercard.filewatcherservice.model;

import lombok.Data;

@Data
public class TableNameCheckResult {

    private String type;
    private String targetTableName;

}
