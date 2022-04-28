package com.logic.system.functions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jcraft.jsch.*;
import com.logic.system.functions.cosmos.model.LogItem;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;

public class CentriaFunctions {
    private static final String INBOUND_FOLDER_SSRS_REPORT;
    private static final String ARCHIVED_FOLDER_SSRS_REPORT;
    private static final String FILE_SHARE_SSRS_REPORTS;
    private static final String ACCOUNT_NAME;
    private static final String ACCOUNT_KEY;
    private static final String STORAGE_CONNECTION_STRING;

    private static final String SFTP_HOST;
    private static final Integer SFTP_PORT;
    private static final String SFTP_USERNAME;
    private static final String SFTP_PASSWORD;
    private static final String SFTP_DESTINY_FOLDER;

    private ChannelSftp sftp = null;
    static{
        INBOUND_FOLDER_SSRS_REPORT = System.getenv("INBOUND_FOLDER_SSRS_REPORT");
        ARCHIVED_FOLDER_SSRS_REPORT = System.getenv("ARCHIVED_FOLDER_SSRS_REPORT");
        FILE_SHARE_SSRS_REPORTS = System.getenv("FILE_SHARE_SSRS_REPORTS");
        ACCOUNT_NAME = System.getenv("ACCOUNT_NAME");
        ACCOUNT_KEY = System.getenv("ACCOUNT_KEY");
        STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName="+ACCOUNT_NAME+";AccountKey="+ACCOUNT_KEY+";EndpointSuffix=core.windows.net";

        SFTP_HOST = System.getenv("SFTP_HOST");
        SFTP_PORT = Integer.parseInt(System.getenv("SFTP_PORT"));
        SFTP_USERNAME = System.getenv("SFTP_USERNAME");
        SFTP_PASSWORD = System.getenv("SFTP_PASSWORD");
        SFTP_DESTINY_FOLDER = System.getenv("SFTP_DESTINY_FOLDER");
    }

    @FunctionName("Save-Workato-JSON-Logs")
    public HttpResponseMessage run(
            @HttpTrigger(    name      = "req",
                             methods   = {HttpMethod.POST},
                             authLevel = AuthorizationLevel.FUNCTION,
                             route     = "Save-Workato-JSON-Logs/saveLog")
            HttpRequestMessage<Optional<String>> request,
            @CosmosDBOutput( name                    = "databaseForErrorLogs",
                             databaseName            = "%COSMOS_ERROR_LOG_DB_NAME%",
                             collectionName          = "%COSMOS_ERROR_LOG_COLLECTION_NAME%",
                    connectionStringSetting          = "COSMOS_ERROR_LOG_CONNECTION_STRING_SETTING")
            OutputBinding<LogItem> outputItem,
            final ExecutionContext context) {
        context.getLogger().info("Save-Workato-JSON-Logs/saveLog trigger processed a request.");
        Map<String, String> headers = request.getHeaders();
        final String jsonString = request.getBody().orElse(null);
        if (jsonString != null && headers.get("content-type").equals("application/json")) {
            try {
                GsonBuilder builder = new GsonBuilder();
                builder.setPrettyPrinting();
                Gson gson = builder.create();
                LogItem item = gson.fromJson(jsonString, LogItem.class);
                item.setId(new StringBuffer().append(Math.abs(new Random().nextInt())).toString());
                outputItem.setValue(item);
                return request.createResponseBuilder(HttpStatus.OK).body("JSON log object saved successfully").build();
            } catch (Exception e) {
                context.getLogger().log(Level.SEVERE,"Save-Workato-JSON-Logs/saveLog had a problem getting the JSON Object. Message = "+e.getMessage(), e);
            }
        }
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass the json object in the body").build();
    }


    @FunctionName("SsrsNonBillableReportFTPTransferFunction-Java")
    public void run(                                 /*Every 5 minutes from 7AM to 9 PM*/
            @TimerTrigger(name = "timerInfo", schedule = "0 */5 7-21 * * *") String timerInfo,
            final ExecutionContext context
    ) {
        context.getLogger().info("Java Timer trigger function executed started at: " + LocalDateTime.now());
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(STORAGE_CONNECTION_STRING);
            CloudFileClient fileClient = storageAccount.createCloudFileClient();
            CloudFileShare share = fileClient.getShareReference(FILE_SHARE_SSRS_REPORTS);
            if(share.exists()){
                CloudFileDirectory rootDir = share.getRootDirectoryReference();
                CloudFileDirectory inboundDir = rootDir.getDirectoryReference(INBOUND_FOLDER_SSRS_REPORT);
                Iterable<ListFileItem> results = inboundDir.listFilesAndDirectories();
                for (ListFileItem item : results) {
                    if (item instanceof CloudFile){
                        CloudFile sourceItem = (CloudFile) item;
                        context.getLogger().info("Processing file : " + sourceItem.getName() + " With URI : "+sourceItem.getUri());

                        context.getLogger().info("Starting sending FTP file : " + sourceItem.getName());
                        String fileText = sourceItem.downloadText();
                        /*SEND FTP FILE*/
                        String destiny = SFTP_DESTINY_FOLDER + sourceItem.getName();
                        sendFileOverFTP(fileText.getBytes(),destiny,context);
                        context.getLogger().info("FTP file : " + sourceItem.getName()+" sent successfully");
                        context.getLogger().info("End sending FTP file : " + sourceItem.getName());

                        context.getLogger().info("Start Moving File to Archive Folder : " + ARCHIVED_FOLDER_SSRS_REPORT);
                        CloudFileDirectory archivedDir = rootDir.getDirectoryReference(ARCHIVED_FOLDER_SSRS_REPORT);
                        CloudFile archivedFile = archivedDir.getFileReference(sourceItem.getName());
                        archivedFile.startCopy(sourceItem);
                        context.getLogger().info("End Moving File to Archive Folder : " + ARCHIVED_FOLDER_SSRS_REPORT);

                        context.getLogger().info("Start Deleting file: "+ sourceItem.getName()+ " from Folder : " + INBOUND_FOLDER_SSRS_REPORT);
                        sourceItem.delete();
                        context.getLogger().info("End Deleting file: "+ sourceItem.getName()+ " from Folder : " + INBOUND_FOLDER_SSRS_REPORT);

                    }
                }
            }
        } catch (InvalidKeyException | URISyntaxException | StorageException | IOException exception) {
            context.getLogger().log(Level.SEVERE, "Java Timer trigger function had problems at: " + exception.getMessage(), exception);
        }
        context.getLogger().info("Java Timer trigger function executed ended at: " + LocalDateTime.now());
    }

    private void sendFileOverFTP(byte[] fileToMove,String destiny, final ExecutionContext context) {
        try {
            if(sftp==null || !sftp.isConnected()){
                JSch jsch = new JSch();
                Session sshSession = jsch.getSession(SFTP_USERNAME, SFTP_HOST, SFTP_PORT);
                sshSession.setPassword(SFTP_PASSWORD);
                Properties sshConfig = new Properties();
                sshConfig.put("StrictHostKeyChecking", "no");
                sshSession.setConfig(sshConfig);
                sshSession.connect(20000);
                Channel channel = sshSession.openChannel("sftp");
                channel.connect();
                sftp = (ChannelSftp) channel;
            }
            InputStream inputStreamFile = new ByteArrayInputStream(fileToMove);
            sftp.put(inputStreamFile, destiny);
        } catch (JSchException | SftpException e) {
            context.getLogger().log(Level.SEVERE, "sendFileOverFTP method has problems at: " + e.getMessage(), e);
        }finally {
            if (sftp != null) {
                if (sftp.isConnected()) {
                    sftp.disconnect();
                } else if (sftp.isClosed()) {
                    context.getLogger().info(" sftp is closed already");
                }
            }
        }
    }
}
