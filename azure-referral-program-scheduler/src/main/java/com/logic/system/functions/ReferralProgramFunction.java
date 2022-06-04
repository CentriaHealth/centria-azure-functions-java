package com.logic.system.functions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import org.json.JSONObject;

/**
 * Azure Functions with Timer trigger.
 */
public class ReferralProgramFunction {

    private static final String SSO_BASE_URL;
    private static final String SSO_BATCH_URL;
    private static final String BATCH_URL_TO_CALL;

    static {
        SSO_BASE_URL = System.getenv("SSO_BASE_URL");
        SSO_BATCH_URL = System.getenv("SSO_BATCH_URL");
        BATCH_URL_TO_CALL = System.getenv("BATCH_URL_TO_CALL");
    }

    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("referralProgramScheduler-Java")
    public void run(@TimerTrigger(name = "timerInfo", schedule = "%REFERRAL_PROGRAM_CRON_EXP%") String timerInfo,
        final ExecutionContext context


    ) throws IOException {
        context.getLogger().info("Starting service at " + this.getClass().getName());
        JSONObject json = new JSONObject();
        context.getLogger().info("Will call Scheduler to send Referral Programs to the Queu");
        json.put("programName", "Tech Referral Program");
        URL url = new URL(BATCH_URL_TO_CALL);
        String response = sendRequest(url, "POST", json.toString(),context);
        context.getLogger().info("Successfully finished the BatchCall");
        context.getLogger().info("Sent following results: " + response.toString());
    }

    private String inputStreamToString(HttpURLConnection conn) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder sb = new StringBuilder();
        for (int c; (c = br.read()) >= 0;)
            sb.append((char) c);
        return sb.toString();
    }
    public String sendRequest(URL url, String method, String rawData,ExecutionContext context) throws IOException {
        String token = getToken(context);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("Content-Type", "application/json");
            if (method.equals("POST") && rawData!=null && rawData.length()>0) {
                try (OutputStream wr = conn.getOutputStream()) {
                    byte[] postData = rawData.getBytes(StandardCharsets.UTF_8);
                    wr.write(postData, 0, postData.length);
                }
            }

            if (conn.getResponseCode() != 200) {
                String message = "Failed : HTTP error code : " + conn.getResponseCode() + " " + conn.getResponseMessage()
                        + " url: " + url.getPath();
                throw new RuntimeException(message);
            }
            context.getLogger().info("Request successful, returned 200 " + rawData);
            context.getLogger().info(inputStreamToString(conn));
            return inputStreamToString(conn);
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    private String getToken(ExecutionContext context){
        String token = "";
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
            token = jsonObject.getString("access_token");
            context.getLogger().info("Token retrieved successfully... "+token);
        } catch (Exception e) {
            context.getLogger().info("there was an error while getting the Token " + e.getMessage());
            context.getLogger().log(Level.SEVERE,e.getMessage(),e);
        }

        return token;
    }
}
