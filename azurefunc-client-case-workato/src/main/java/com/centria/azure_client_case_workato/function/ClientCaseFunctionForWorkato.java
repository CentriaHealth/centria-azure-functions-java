package com.centria.azure_client_case_workato.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueOutput;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.*;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ClientCaseFunctionForWorkato {
    private static final String STORAGE_CONNECTION_STRING;
    private static final String INBOUND_FOLDER_REPORT;
    private static final String FILE_SHARE_REPORTS;
    private static final String FILE_NAME;

    static{
        STORAGE_CONNECTION_STRING = System.getenv("STORAGE_CONNECTION_STRING") ;
        INBOUND_FOLDER_REPORT =  System.getenv("INBOUND_FOLDER_REPORT") ;
        FILE_SHARE_REPORTS =  System.getenv("FILE_SHARE_REPORTS") ;
        FILE_NAME =  System.getenv("CASE_FILE_NAME");
    }

    @FunctionName("ClientCaseFunctionForWorkato-Java")
    public void run(@TimerTrigger(name = "timerInfo", schedule = "%CLIENT_CASE_CRON_EXP%") String timerInfo,
                    final ExecutionContext context,
                    @ServiceBusQueueOutput(name = "skedulo", queueName = "%SKEDULO_QUEUE_NAME%",connection = "ASBSSKEDULO_CONNECTION_STRING") OutputBinding<List<String>> outputItem) throws URISyntaxException, StorageException, InvalidKeyException, IOException {
        context.getLogger().info("Azure client timer started on: "+timerInfo.toString());
        try{
            List<String> list = getCaseDataFromFile(context);
            /*Send the messages to the service bus*/
            outputItem.setValue(list);
            context.getLogger().info("Will send azure queue message for client case completion using CSV file.");
        } catch (Exception e) {
            context.getLogger().info("exception occurred while posting data"+ e);
        }
    }

    private List<String> getCaseDataFromFile(ExecutionContext context) throws IOException, StorageException, InvalidKeyException, URISyntaxException {
        List<String> list=new ArrayList<>();
        List<Integer> caseIdList = new ArrayList<>();
        String fileText = getCSVBlobData(FILE_NAME,STORAGE_CONNECTION_STRING,context,FILE_SHARE_REPORTS,INBOUND_FOLDER_REPORT);
        if (fileText != null && !fileText.isEmpty()) {
            Stream.of(fileText.split("\r\n")).forEach(caseId->caseIdList.add(Integer.valueOf(caseId)));
        }
        if(caseIdList!=null && !caseIdList.isEmpty())
            caseIdList.forEach(caseId->{
                JSONObject caseJson=new JSONObject();
                caseJson.put("type","case");
                JSONObject caseDetailJson = new JSONObject();
                caseDetailJson.put("caseId",caseId);
                caseJson.put("caseDetails",caseDetailJson);
                list.add(caseJson.toString());
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
