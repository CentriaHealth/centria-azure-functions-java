package com.logic.system.functions;

import java.sql.*;
import java.time.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Timer trigger.
 */
public class ReplicationHealthcheck {

    private static final String REPLICATION_DB_CONNECTION_STRING;
    static {
        REPLICATION_DB_CONNECTION_STRING = System.getenv("REPLICATION_DB_CONNECTION_STRING");
    }
    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("ReplicationHealthcheck")
    public void run(
        @TimerTrigger(name = "timerInfo", schedule = "%REPLICATION_HC_CRON_EXP%") String timerInfo,
        final ExecutionContext context
    ) {
        context.getLogger().info("Java Timer trigger function executed at: " + LocalDateTime.now());

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(REPLICATION_DB_CONNECTION_STRING);
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery("select top 10 * from system_code ");
            while (rs.next()) {
                String codeDesc = rs.getString("code_desc_en");
                context.getLogger().info("Code Description: " + codeDesc);
            }

        } catch (SQLException throwables) {
                throwables.printStackTrace();
        }


    }
}
