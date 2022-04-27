package com.logic.system.functions.cosmos.model;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class LogItem {
    private int id;
    private String type;
    private String datetime;
    private List<LogDetail> logDetails;
}
