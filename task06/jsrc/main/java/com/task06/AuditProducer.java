package com.task06;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

@LambdaHandler(lambdaName = "audit_producer",
	roleName = "audit_producer-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 1)
@DependsOn(name = "Configuration", resourceType = ResourceType.DYNAMODB_TABLE)
@DependsOn(name = "Audit", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariables(value = {@EnvironmentVariable(key = "table_configuration", value = "${source_table}"), @EnvironmentVariable(key = "table_audit", value = "${target_table}")})

public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {

	private final Regions REGION = Regions.EU_CENTRAL_1;
//	private static final String TABLE_CONFIGURATION = System.getenv("table_configuration");
	private static final String TABLE_AUDIT = System.getenv("table_audit");
	private final DynamoDB DYNAMO_DB = new DynamoDB(AmazonDynamoDBAsyncClientBuilder.standard().withRegion(REGION).build());
	
	@Override
	public Void handleRequest(DynamodbEvent dynamodbEvent, Context context) {

	   System.out.println("dynamoDBEvent triggered event" + dynamodbEvent);

      Table table = DYNAMO_DB.getTable(TABLE_AUDIT);
      String id = java.util.UUID.randomUUID().toString();

      for (DynamodbEvent.DynamodbStreamRecord record : dynamodbEvent.getRecords()) {
          System.out.println("Iterating event records " + record);
  
          Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
          System.out.println("New Object " + newImage);
          Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
          System.out.println("Old Object "+oldImage);

          if (record.getEventName().equals("MODIFY")) {
            System.out.println("Event Name is Modify");
               if(oldImage==null) {
                  this.addItemtoAuditOnInsertEvent(table,id, newImage);
		   	   } else if(oldImage.isEmpty()) {
                  this.addItemtoAuditOnInsertEvent(table,id, newImage);
               } else {
                this.addItemtoAuditOnUpdateEvent(table,id, newImage,oldImage);
               }
          } else if (record.getEventName().equals("INSERT")) {
             System.out.println("Event Name is INSERT");
             this.addItemtoAuditOnInsertEvent(table,id, newImage);
          } else if (record.getEventName().equals("REMOVE")) {
             System.out.println("Old Object " + oldImage);
             this.addItemtoAuditOnUpdateEvent(table,id, newImage,oldImage);
          } 
       }
      return null;
	}
 
   private void addItemtoAuditOnUpdateEvent(Table table,String id, Map<String, AttributeValue> newImage,Map<String, AttributeValue> oldImage){

      String modificationDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
      table.putItem(new Item()
            .withPrimaryKey("id", id)
            .with("modificationDate", modificationDate)
            .with("itemKey", newImage.get("key").getS())
            .with("updatedAttribute", "value")
            .with("oldValue", Integer.parseInt(oldImage.get("value").getN()))
            .with("newValue", Integer.parseInt(newImage.get("value").getN())));
                      
   }

   private void addItemtoAuditOnInsertEvent(Table table,String id, Map<String, AttributeValue> newImage){
      
      String modificationDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
      Map<String,Object> newKeyMap = new HashMap<>();

      newKeyMap.put("key", newImage.get("key").getS());
      newKeyMap.put("value", Integer.parseInt(newImage.get("value").getS()));
      table.putItem(new Item()
            .withPrimaryKey("id", id)
            .with("modificationDate", modificationDate)
            .with("itemKey", newImage.get("key").getS())
            .with("newValue", newKeyMap));
   }
}
