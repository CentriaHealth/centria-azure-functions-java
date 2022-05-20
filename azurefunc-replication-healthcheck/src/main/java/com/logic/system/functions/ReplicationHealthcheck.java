package com.logic.system.functions;

import java.sql.*;
import java.time.*;
import java.util.logging.Level;

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


        try {
            context.getLogger().info("Getting connection with "+REPLICATION_DB_CONNECTION_STRING);

            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection con = DriverManager.getConnection("jdbc:sqlserver://10.140.0.11:1433;databaseName=centria_ams_replicated","ahernandez","W9P.igci$+");

            context.getLogger().info("Creating Statement.....");
            Statement statement = con.createStatement();
            context.getLogger().info("Executing Query.....");
            ResultSet rs = statement.executeQuery("select top 10 * from system_code ");
            while (rs.next()) {
                String codeDesc = rs.getString("code_desc_en");
                context.getLogger().info("Code Description: " + codeDesc);
            }

        } catch (SQLException | ClassNotFoundException e) {
            context.getLogger().info("Error while connection to DB: " + e.getMessage());
            context.getLogger().log(Level.SEVERE, "SQL Problem ...", e);
        }

        context.getLogger().info("Java Timer trigger function finieshed at: " + LocalDateTime.now());


    }
}
