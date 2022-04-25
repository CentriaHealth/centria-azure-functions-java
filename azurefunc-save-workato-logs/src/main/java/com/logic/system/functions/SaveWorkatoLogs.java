package com.logic.system.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class SaveWorkatoLogs {
    /**
     * This function listens at endpoint "/api/HttpExample". 1 way to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     */
    @FunctionName("Save-Workato-JSON-Logs")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "Save-Workato-JSON-Logs/saveLog")
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Save-Workato-JSON-Logs/saveLog trigger processed a request.");
        Map<String, String> headers = request.getHeaders();

        final String jsonString = request.getBody().orElse(null);

        if (jsonString != null && headers.get("content-type").equals("application/json")) {
            try {
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(jsonString);
                return request.createResponseBuilder(HttpStatus.OK).body("JSON log object saved successfully").build();
            } catch (Exception e) {
                context.getLogger().log(Level.SEVERE,"Save-Workato-JSON-Logs/saveLog had a problem getting the JSON Object. Message = "+e.getMessage(), e);
            }
        }
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass the json object in the body").build();
    }

}
