package com.logic.system.functions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Azure Functions with Timer trigger.
 */
public class ReferralProgramFunction {
    private static final String SSO_BATCH_API_TOKEN;
    private static final String BATCH_REFERRAL_API_TO_CALL;

    static {
        SSO_BATCH_API_TOKEN = System.getenv("SSO_BATCH_API_TOKEN");
        BATCH_REFERRAL_API_TO_CALL = System.getenv("BATCH_REFERRAL_API_TO_CALL");
    }

    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("referralProgramScheduler-Java")
    public void run(@TimerTrigger(name = "timerInfo", schedule = "%REFERRAL_PROGRAM_CRON_EXP%") String timerInfo,
        final ExecutionContext context) throws IOException {

        context.getLogger().info("Starting service at " + this.getClass().getName());
        JSONObject json = new JSONObject();
        context.getLogger().info("Will call Scheduler to send Referral Programs to the Queu");
        json.put("programName", "Tech Referral Program");
        String response = callReferralScheduleAPI(json.toString(),context);
        JSONArray inputArray = new JSONArray(response);
        Iterator it = inputArray.iterator();
        while(it.hasNext()){
            Object e = it.next();
            if(e instanceof JSONObject){
                context.getLogger().info(((JSONObject)e).toString());
            }
        }
        context.getLogger().info("Successfully finished the BatchCall");
    }


    private String callReferralScheduleAPI(String rawData,ExecutionContext context) throws IOException {
        //**Get the Token*/
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/x-www-form-urlencoded");
        headers.put("charset","utf-8");
        String rawTokenResponse = sendRequest(new URL(SSO_BATCH_API_TOKEN),"POST",headers,"",context);
        JSONObject jsonObject = new JSONObject(rawTokenResponse);
        String token = jsonObject.getString("access_token");

        /**Execute the scheduler*/
        headers.put("Authorization",token);
        headers.put("Accept","application/json");
        headers.put("Content-Type", "application/json");
        return sendRequest(new URL(BATCH_REFERRAL_API_TO_CALL),"POST",headers,rawData,context);
    }

    private String sendRequest(URL url,String method, Map<String,String> headers, String params, ExecutionContext context){
        String response = "";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);

            if(headers!=null){
                Set<String> keys = headers.keySet();
                for(String k : keys)
                    conn.setRequestProperty(k,headers.get(k));
            }
            conn.setRequestProperty("Content-Length", Integer.toString(params.length()));
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            byte[] postData = params.getBytes(StandardCharsets.UTF_8);
            wr.write(postData);
            if (conn.getResponseCode() != 200) {
                conn = null;
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            response = inputStreamToString(conn);
            context.getLogger().info("Response retrieved successfully... "+response);
        } catch (Exception e) {
            context.getLogger().info("there was an error while getting the Token " + e.getMessage());
            context.getLogger().log(Level.SEVERE,e.getMessage(),e);
        }
        return response;
    }

    private String inputStreamToString(HttpURLConnection conn) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder sb = new StringBuilder();
        for (int c; (c = br.read()) >= 0;)
            sb.append((char) c);
        return sb.toString();
    }

}
