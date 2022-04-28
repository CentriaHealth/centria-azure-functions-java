package com.logic.system.functions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.logic.system.functions.cosmos.model.LogItem;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;

public class SaveWorkatoLogs {
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

}
