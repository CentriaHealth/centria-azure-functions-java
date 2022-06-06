package com.centria.azure_client_case_workato.function;

import com.centria.azure_client_case_workato.service.AzureBusQueueService;
import com.centria.azure_client_case_workato.service.AzureStorageFileReader;
import com.centria.azure_client_case_workato.service.KeyVaultService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.azure.storage.StorageException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ClientCaseFunctionForWorkato {

    private static final String ACCOUNT_NAME;
    private static final String ACCOUNT_KEY;
    private static final String STORAGE_CONNECTION_STRING;
    private static final String INBOUND_FOLDER_REPORT;
    private static final String FILE_SHARE_REPORTS;
    private static final String FILE_NAME;
    private static final String KEY_VAULT_NAME;


    static{
        ACCOUNT_NAME =  System.getenv("ACCOUNT_NAME") ;
        ACCOUNT_KEY =  System.getenv("ACCOUNT_KEY") ;
        STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName="+ACCOUNT_NAME+";AccountKey="+ACCOUNT_KEY+";EndpointSuffix=core.windows.net";
        INBOUND_FOLDER_REPORT =  System.getenv("INBOUND_FOLDER_REPORT") ;
        FILE_SHARE_REPORTS =  System.getenv("FILE_SHARE_REPORTS") ;
        FILE_NAME =  System.getenv("CASE_FILE_NAME") ;
        KEY_VAULT_NAME = System.getenv("KEY_VAULT_NAME") ;
    }

    @FunctionName("ClientCaseFunctionForWorkato-Java")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 0 10 * * *") String timerInfo,
            final ExecutionContext context
    ) throws URISyntaxException, StorageException, InvalidKeyException, IOException {
        context.getLogger().info("Azure client timer started on: "+timerInfo.toString());
        List<String> list=getCaseDataFromFile(context);
        try{
            String secret=new KeyVaultService().getSecretValue(KEY_VAULT_NAME);
            new AzureBusQueueService().postData(list, secret,"skedulo",context);
            context.getLogger().info("Will send azure queue message for client case completion using CSV file.");
        } catch (Exception e) {
            context.getLogger().info("exception occurred while posting data"+ e);
        }
    }



    private List<String> getCaseDataFromFile(ExecutionContext context) throws IOException, StorageException, InvalidKeyException, URISyntaxException {
        List<String> list=new ArrayList<>();
        List<Integer> caseIdList = new ArrayList<>();
        String fileText=new AzureStorageFileReader().getCSVBlobData(FILE_NAME,STORAGE_CONNECTION_STRING,context,FILE_SHARE_REPORTS,INBOUND_FOLDER_REPORT);
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
}
