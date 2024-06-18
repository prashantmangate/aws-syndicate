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

		       System.out.println("Inside handleRequest and received dynamoDBEvent " + dynamodbEvent);
       for (DynamodbEvent.DynamodbStreamRecord record : dynamodbEvent.getRecords()) {
          // Check if it's an updated record
          System.out.println("Iterating event records " + record);
          if (record.getEventName().equals("MODIFY")) {
             System.out.println("Event Name is Modify");
             // Extract the updated record
             // Here you'll have access to the new and old images of the record
             // Perform necessary processing on the updated record
             // Example: Accessing the new image
             Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();//.getDynamodb().getNewImage();
             System.out.println("New Updated Object " + newImage);
             // Example: Accessing the old image
             Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
             System.out.println("Old Object " + oldImage);
             System.out.println("Should be printed");
             // Add logic to process the updated record and add it to the new table
             // AddNewRecordToNewTable(newImage);
             System.out.println("Should be printed 2");
             Table table = DYNAMO_DB.getTable(TABLE_AUDIT);
             System.out.println("table name Object " + table.getTableName());
             // Create a map of attributes for the item
             String id = java.util.UUID.randomUUID().toString();
             String modificationDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
             if(oldImage.isEmpty()){
                Map<String,String> newKeyMap = new HashMap<>();
                newKeyMap.put("key", String.valueOf(newImage.get("key")));
                newKeyMap.put("value", String.valueOf(newImage.get("value")));
                System.out.println("newKeyMap " + newKeyMap);
                System.out.println("modificationDate " + modificationDate);
                System.out.println("itemKey " + newImage.get("key"));
                System.out.println("id " + id);
                PutItemOutcome outcome = table.putItem(new Item()
                      .withPrimaryKey("id", id)
                      .with("modificationDate", modificationDate)
                      .with("itemKey", newImage.get("key"))
                      .with("newValue", newKeyMap));
             }else {
                System.out.println("oldValue " + oldImage.get("value"));
                System.out.println("newValue " + newImage.get("value"));
                System.out.println("modificationDate " + modificationDate);
                System.out.println("itemKey " + newImage.get("key"));
                System.out.println("id " + id);
                PutItemOutcome outcome = table.putItem(new Item()
                      .withPrimaryKey("id", id)
                      .with("modificationDate", modificationDate)
                      .with("itemKey", newImage.get("key"))
                      .with("updatedAttribute", "value")
                      .with("oldValue", oldImage.get("value"))
                      .with("newValue", newImage.get("value")));
             }
          } else if (record.getEventName().equals("INSERT")) {
             System.out.println("Event Name is Modify");
             // Example: Accessing the new image
             Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
             System.out.println("New Updated Object " + newImage);
             // Example: Accessing the old image
             Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
             System.out.println("Old Object " + oldImage);
          } else if (record.getEventName().equals("REMOVE")) {
             // Process removed records
             System.out.println("Event Name is Remove");
             // Example: Accessing the new image
             Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
             System.out.println("New Updated Object " + newImage);
             // Example: Accessing the old image
             Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
             System.out.println("Old Object " + oldImage);
          } else {
             System.out.println("Event Name is Unknown " + record.getEventName() );
             // Handle other types of events, if necessary
          }
       }
	return null;

	}

}
