package com.centria.azure_client_case_workato.service;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public class AzureStorageFileReader {

    public String getCSVBlobData(String filename, String connectionString, ExecutionContext context,String shareReport,String inFolder) throws URISyntaxException, InvalidKeyException, StorageException, IOException {
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
