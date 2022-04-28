package com.logic.system.functions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.*;
import java.util.Properties;
import java.util.logging.Level;
import com.jcraft.jsch.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.*;

/**
 * Azure Functions with Timer trigger.
 */
public class SsrsNonBillableReportFTPTransferFunction {
    private static final String SSRSNONBREPORT_INBOUND_FOLDER_SSRS_REPORT;
    private static final String SSRSNONBREPORT_ARCHIVED_FOLDER_SSRS_REPORT;
    private static final String SSRSNONBREPORT_FILE_SHARE_SSRS_REPORTS;
    private static final String SSRSNONBREPORT_ACCOUNT_NAME;
    private static final String SSRSNONBREPORT_ACCOUNT_KEY;
    private static final String SSRSNONBREPORT_STORAGE_CONNECTION_STRING;

    private static final String SSRSNONBREPORT_SFTP_HOST;
    private static final Integer SSRSNONBREPORT_SFTP_PORT;
    private static final String SSRSNONBREPORT_SFTP_USERNAME;
    private static final String SSRSNONBREPORT_SFTP_PASSWORD;
    private static final String SSRSNONBREPORT_SFTP_DESTINY_FOLDER;
    private ChannelSftp sftp = null;
    static{
        SSRSNONBREPORT_INBOUND_FOLDER_SSRS_REPORT = System.getenv("SSRSNONBREPORT_INBOUND_FOLDER_SSRS_REPORT");
        SSRSNONBREPORT_ARCHIVED_FOLDER_SSRS_REPORT = System.getenv("SSRSNONBREPORT_ARCHIVED_FOLDER_SSRS_REPORT");
        SSRSNONBREPORT_FILE_SHARE_SSRS_REPORTS = System.getenv("SSRSNONBREPORT_FILE_SHARE_SSRS_REPORTS");
        SSRSNONBREPORT_ACCOUNT_NAME = System.getenv("SSRSNONBREPORT_ACCOUNT_NAME");
        SSRSNONBREPORT_ACCOUNT_KEY = System.getenv("SSRSNONBREPORT_ACCOUNT_KEY");
        SSRSNONBREPORT_STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=https;AccountName="+SSRSNONBREPORT_ACCOUNT_NAME+";AccountKey="+SSRSNONBREPORT_ACCOUNT_KEY+";EndpointSuffix=core.windows.net";

        SSRSNONBREPORT_SFTP_HOST = System.getenv("SSRSNONBREPORT_SFTP_HOST");
        SSRSNONBREPORT_SFTP_PORT = Integer.parseInt(System.getenv("SSRSNONBREPORT_SFTP_PORT"));
        SSRSNONBREPORT_SFTP_USERNAME = System.getenv("SSRSNONBREPORT_SFTP_USERNAME");
        SSRSNONBREPORT_SFTP_PASSWORD = System.getenv("SSRSNONBREPORT_SFTP_PASSWORD");
        SSRSNONBREPORT_SFTP_DESTINY_FOLDER = System.getenv("SSRSNONBREPORT_SFTP_DESTINY_FOLDER");
    }
    /**
     * This function will be invoked periodically according to the specified schedule.
     */

    //For DEV check your local.settings.json and add this key value "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    //Make sure you have running Azurite for local testing azurite --silent --location c:\azurite --debug c:\azurite\debug.log
    @FunctionName("SsrsNonBillableReportFTPTransferFunction-Java")
    public void run(                                 /*Every 5 minutes from 7AM to 9 PM*/
        @TimerTrigger(name = "timerInfo", schedule = "%SSRSNONBREPORT_CRON_EXP%") String timerInfo,
        final ExecutionContext context
    ) {
        context.getLogger().info("Java Timer trigger function executed started at: " + LocalDateTime.now());
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(SSRSNONBREPORT_STORAGE_CONNECTION_STRING);
            CloudFileClient fileClient = storageAccount.createCloudFileClient();
            CloudFileShare share = fileClient.getShareReference(SSRSNONBREPORT_FILE_SHARE_SSRS_REPORTS);
            if(share.exists()){
                CloudFileDirectory rootDir = share.getRootDirectoryReference();
                CloudFileDirectory inboundDir = rootDir.getDirectoryReference(SSRSNONBREPORT_INBOUND_FOLDER_SSRS_REPORT);
                Iterable<ListFileItem> results = inboundDir.listFilesAndDirectories();
                for (ListFileItem item : results) {
                    if (item instanceof CloudFile){
                        CloudFile sourceItem = (CloudFile) item;
                        context.getLogger().info("Processing file : " + sourceItem.getName() + " With URI : "+sourceItem.getUri());

                        context.getLogger().info("Starting sending FTP file : " + sourceItem.getName());
                        String fileText = sourceItem.downloadText();
                        /*SEND FTP FILE*/
                        String destiny = SSRSNONBREPORT_SFTP_DESTINY_FOLDER + sourceItem.getName();
                        sendFileOverFTP(fileText.getBytes(),destiny,context);
                        context.getLogger().info("FTP file : " + sourceItem.getName()+" sent successfully");
                        context.getLogger().info("End sending FTP file : " + sourceItem.getName());

                        context.getLogger().info("Start Moving File to Archive Folder : " + SSRSNONBREPORT_ARCHIVED_FOLDER_SSRS_REPORT);
                        CloudFileDirectory archivedDir = rootDir.getDirectoryReference(SSRSNONBREPORT_ARCHIVED_FOLDER_SSRS_REPORT);
                        CloudFile archivedFile = archivedDir.getFileReference(sourceItem.getName());
                        archivedFile.startCopy(sourceItem);
                        context.getLogger().info("End Moving File to Archive Folder : " + SSRSNONBREPORT_ARCHIVED_FOLDER_SSRS_REPORT);

                        context.getLogger().info("Start Deleting file: "+ sourceItem.getName()+ " from Folder : " + SSRSNONBREPORT_INBOUND_FOLDER_SSRS_REPORT);
                        sourceItem.delete();
                        context.getLogger().info("End Deleting file: "+ sourceItem.getName()+ " from Folder : " + SSRSNONBREPORT_INBOUND_FOLDER_SSRS_REPORT);
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
                Session sshSession = jsch.getSession(SSRSNONBREPORT_SFTP_USERNAME, SSRSNONBREPORT_SFTP_HOST, SSRSNONBREPORT_SFTP_PORT);
                sshSession.setPassword(SSRSNONBREPORT_SFTP_PASSWORD);
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
