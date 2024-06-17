package com.task05;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Attribute;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.task05.model.Event;;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariable(key = "target_events", value = "${target_table}")
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

//	private static final Logger logger = Logger.getLogger(AuditProducer.class.getName());

private AmazonDynamoDB amazonDynamoDB;
	private final Regions REGION = Regions.EU_CENTRAL_1;
	private static final String TABLE_EVENTS = System.getenv("target_events");
//	private final DynamoDB DYNAMO_DB = new DynamoDB(AmazonDynamoDBAsyncClientBuilder.standard().withRegion(System.getenv("region")).build());
private final DynamoDB DYNAMO_DB = new DynamoDB(AmazonDynamoDBAsyncClientBuilder.standard().withRegion(REGION).build());

	public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
//			initDynamoDbClient();
			System.out.println("Complete Request " + request);
//			Map<String, AttributeValue> resultMap = new HashMap<String, AttributeValue>();	
			String id = java.util.UUID.randomUUID().toString();
			int principalId = Integer.parseInt(request.get("principalId").toString());
//			AttributeValue pId = new AttributeValue();

//			pId.setN(String.valueOf(principalId));			

			String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

			Map<String, String> content = (Map<String, String>) request.get("content");

			// StringBuilder contentString = new StringBuilder("any map '{'");
			// for (String key : content.keySet()) {
			// 	contentString.append(key + "=" + content.get(key) + ", ");
			// }
			// contentString.append("'}'");
			// AttributeValue contentStr = new AttributeValue();
			// contentStr.setM(content);
//			Table table = DYNAMO_DB.getTable(TABLE_EVENTS);
			// resultMap.put("id",  new AttributeValue(id));	
			// resultMap.put("principalId",pId);
			// resultMap.put("createdAt", new AttributeValue(createdAt));
			// resultMap.put("body", contentStr);
			Event event = new Event();
			event.setBody(content);
			event.setId(id);
			event.setCreatedAt(createdAt);
			event.setPrincipalId(principalId);

			try{
				Table table = DYNAMO_DB.getTable(TABLE_EVENTS);
//				amazonDynamoDB.putItem(TABLE_EVENTS, resultMap);
				PutItemOutcome outcome = table.putItem(new Item()
								.withPrimaryKey("id", id)
						.with("principalId", principalId)
						.with("createdAt", createdAt)
						.with("body", content));
				System.out.println("Item event saved in dynamodb");
				Map<String, Object> responseMap = new HashMap<String, Object>();
				responseMap.put("statusCode", 201);
				responseMap.put("event", event);//amazonDynamoDB.getItem(TABLE_EVENTS, resultMap).getItem().get("body")
				return responseMap;	
			}
			catch(Exception e){
				System.out.println(e);
				e.printStackTrace();
				return Map.of("Error","Event not saved");
			}
		}

		// private void initDynamoDbClient() {
	    //      this.amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(REGION).build();	
     	// }
}
