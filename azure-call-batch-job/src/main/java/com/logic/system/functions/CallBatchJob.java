package com.logic.system.functions;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import org.json.JSONObject;

/**
 * Azure Functions with Timer trigger.
 */
public class CallBatchJob {
    private static final String SSO_BASE_URL;
    private static final String SSO_BATCH_URL;

    static {
        SSO_BASE_URL = System.getenv("SSO_BASE_URL");
        SSO_BATCH_URL = System.getenv("SSO_BATCH_URL");
    }

    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("callBatchJob-Java")
    public void run(
        @TimerTrigger(name = "callBatchJob", schedule = "0 */5 * * * *") String timerInfo,
        final ExecutionContext context
    ){
        context.getLogger().info("Starting service at "+this.getClass().getName());
        HttpURLConnection conn = null;
        String params = null;
        try {
            URL url = new URL(SSO_BASE_URL+ SSO_BATCH_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            params = "";
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(params.length()));
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            byte[] postData = params.getBytes(StandardCharsets.UTF_8);
            wr.write(postData);
            if (conn.getResponseCode() != 200) {
                conn = null;
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            String response = inputStreamToString(conn);
            JSONObject jsonObject = new JSONObject(response);
            String accessToken = jsonObject.getString("access_token");
            context.getLogger().info("Token retrieved successfully... "+accessToken);

        } catch (Exception e) {
            context.getLogger().info("there was an error while getting the Token " + e.getMessage());
            context.getLogger().log(Level.SEVERE,e.getMessage(),e);
        }
        context.getLogger().info("Successfully finished the BatchCall");
    }

    private String inputStreamToString(HttpURLConnection conn) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder sb = new StringBuilder();
        for (int c; (c = br.read()) >= 0;)
            sb.append((char) c);
        return sb.toString();
    }
}
