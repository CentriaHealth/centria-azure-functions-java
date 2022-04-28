package com.logic.system.functions.cosmos.model;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
@Getter
@Setter
public class LogDetail {
    private String employeeId;
    private String uuid;
    private List<String> messages;
    private String upn;
    private String skeduloShiftId;
    private String ccuserId;
}
