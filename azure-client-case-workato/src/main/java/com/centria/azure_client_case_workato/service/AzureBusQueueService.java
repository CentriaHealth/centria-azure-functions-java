package com.centria.azure_client_case_workato.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusMessageBatch;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

public class AzureBusQueueService {

    public void postData(List<String> messages, String connectionString, String queueName, ExecutionContext context) throws Exception {
        ServiceBusSenderClient senderClient = null;
        try {

            senderClient = getQueueConnection(connectionString,queueName,context);
            if(null == senderClient){
                context.getLogger().info("service bus queue connection does not exist");
                return;
            }

            if (messages==null || messages.isEmpty()) {
                context.getLogger().info("empty messages List for posting to service bus queue");
                return;
            }
            context.getLogger().info("Number of messages to be posted to service bus queue {}"+ messages.size());
            List<ServiceBusMessage> messageList = new ArrayList<>();
            messages.forEach(msg -> messageList.add(new ServiceBusMessage(msg)));

            ServiceBusMessageBatch messageBatch = senderClient.createMessageBatch();

            messageList.forEach(serviceBusMessage -> {
                if (messageBatch.tryAddMessage(serviceBusMessage)) {
                    return;
                }

            });
            senderClient.sendMessages(messageBatch);
            context.getLogger().info("Actual Number of messages posted to service bus queue {}"+ messageBatch.getCount());
        } catch (Exception e) {
            context.getLogger().info("Exception occurred in posting batch messages to service bus queue"+ e);
            throw e;
        } finally {
            if (null != senderClient) {
                senderClient.close();
            }
        }

    }


    ServiceBusSenderClient getQueueConnection(String connectionString,String queueName,ExecutionContext context) throws Exception {
        ServiceBusSenderClient senderClient =  null;
        try {
            senderClient = new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .sender()
                    .queueName(queueName)
                    .buildClient();
        } catch (Exception e) {
            context.getLogger().info("exception occurred in queue connection creation "+ e);
            throw e;
        }
        return senderClient;

    }

}
