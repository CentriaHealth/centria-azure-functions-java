package com.centria.azurefunc_compliance_workato;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueOutput;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ComplianceUserWorkato {

    private static final String STORAGE_CONNECTION_STRING;
    private static final String INBOUND_FOLDER_REPORT;
    private static final String FILE_SHARE_REPORTS;
    private static final String FILE_NAME;

    static{
        STORAGE_CONNECTION_STRING = System.getenv("STORAGE_CONNECTION_STRING") ;
        INBOUND_FOLDER_REPORT =  System.getenv("INBOUND_FOLDER_REPORT") ;
        FILE_SHARE_REPORTS =  System.getenv("FILE_SHARE_REPORTS") ;
        FILE_NAME =  System.getenv("FILE_NAME");
    }

    @FunctionName("ComplianceUserWorkato-Java")
    public void run(@TimerTrigger(name = "timerInfo", schedule = "%USER_CRON_EXP%") String timerInfo,
                    final ExecutionContext context,
                    @ServiceBusQueueOutput(name = "skedulo", queueName = "%SKEDULO_QUEUE_NAME%",connection = "ASBSSKEDULO_CONNECTION_STRING") OutputBinding<List<String>> outputItem) throws URISyntaxException, StorageException, InvalidKeyException, IOException {
        context.getLogger().info("Azure client timer started on: "+timerInfo.toString());
        try{
            List<String> list = getUserDataFromFile(context);
            /*Send the messages to the service bus*/
            outputItem.setValue(list);
            context.getLogger().info("Will send azure queue message for user compliance completion using CSV file.");
        } catch (Exception e) {
            context.getLogger().info("exception occurred while posting data"+ e);
        }
    }

    private List<String> getUserDataFromFile(ExecutionContext context) throws IOException, StorageException, InvalidKeyException, URISyntaxException {
        List<String> list=new ArrayList<>();
        List<Integer> userIdList = new ArrayList<>();
        String fileText = getCSVBlobData(FILE_NAME,STORAGE_CONNECTION_STRING,context,FILE_SHARE_REPORTS,INBOUND_FOLDER_REPORT);
        if (fileText != null && !fileText.isEmpty()) {
            Stream.of(fileText.split("\r\n")).forEach(userId-> {
                if(userId.charAt(0)==239)
                    userId=userId.substring(3,userId.length());
                userIdList.add(Integer.valueOf(userId));
            });
        }
        if(userIdList!=null && !userIdList.isEmpty())
            userIdList.forEach(userId->{
                JSONObject userJson = new JSONObject();
                userJson.put("type","case");
                userJson.put("subtype","compliance");
                JSONObject complianceDetailsJson = new JSONObject();
                complianceDetailsJson.put("userId",userId);
                complianceDetailsJson.put("syncskedulo",true);
                JSONObject mvr = new JSONObject();
                mvr.put("name","MVR");
                JSONObject driver = new JSONObject();
                driver.put("name","Driver License");
                JSONObject auto = new JSONObject();
                auto.put("name","Auto Insurance");
                JSONObject transport = new JSONObject();
                transport.put("name","Transportation Attestation");
                JSONArray subcategory = new JSONArray();
                subcategory.add(mvr);
                subcategory.add(driver);
                subcategory.add(auto);
                JSONArray subcategorynon = new JSONArray();
                subcategorynon.add(transport);
                JSONObject categoryJson = new JSONObject();
                categoryJson.put("name","Expirable Documents");
                categoryJson.put("subcategory",subcategory);
                JSONObject categoryJsonnon = new JSONObject();
                categoryJsonnon.put("name","Non-expirable Documents");
                categoryJsonnon.put("subcategory",subcategorynon);
                JSONArray categoryArray = new JSONArray();
                categoryArray.add(categoryJson);
                categoryArray.add(categoryJsonnon);
                complianceDetailsJson.put("category",categoryArray);
                userJson.put("complianceDetails",complianceDetailsJson);
                list.add(userJson.toString());
            });
        return list;
    }

    private String getCSVBlobData(String filename, String connectionString, ExecutionContext context,String shareReport,String inFolder) throws URISyntaxException, InvalidKeyException, StorageException, IOException {
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
            CloudFileClient fileClient = storageAccount.createCloudFileClient();
            CloudFileShare share = fileClient.getShareReference(shareReport);

            if (share.exists()) {
                CloudFileDirectory rootDir = share.getRootDirectoryReference();
                CloudFileDirectory inboundDir = rootDir.getDirectoryReference(inFolder);
                Iterable<ListFileItem> results = inboundDir.listFilesAndDirectories();
                for (ListFileItem item : results) {
                    if (item instanceof CloudFile) {
                        CloudFile sourceItem = (CloudFile) item;
                        context.getLogger().info("Processing file : " + sourceItem.getName() + " With URI : " + sourceItem.getUri());
                        if(filename != null && filename.equalsIgnoreCase(sourceItem.getName()))
                            return sourceItem.downloadText();
                    }
                }
            }
        }catch (Exception e){
            context.getLogger().info("Exception occurred, "+e.getMessage());
        }
        return null;
    }

}
