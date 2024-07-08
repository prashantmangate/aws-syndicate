package com.task10;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.EmailValidator;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminConfirmSignUpRequest;
import com.amazonaws.services.cognitoidp.model.AdminConfirmSignUpResult;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.ListUserPoolClientsRequest;
import com.amazonaws.services.cognitoidp.model.ListUserPoolsRequest;
import com.amazonaws.services.cognitoidp.model.ListUserPoolsResult;
import com.amazonaws.services.cognitoidp.model.SignUpRequest;
import com.amazonaws.services.cognitoidp.model.SignUpResult;
import com.amazonaws.services.cognitoidp.model.UserPoolDescriptionType;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = true,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "tables_table", value = "${tables_table}"),
		@EnvironmentVariable(key = "reservations_table", value = "${reservations_table}"),
		@EnvironmentVariable(key = "userpool", value = "${booking_userpool}")
	})
public class ApiHandler implements RequestHandler<Object, Map<String, Object>> {

	private static final String USER_POOL = System.getenv("userpool");
	private final Regions REGION = Regions.EU_CENTRAL_1;
	private static final String TABLE_TABLE = System.getenv("tables_table");
	private static final String TABLE_RESERVATION = System.getenv("reservations_table");
	private final DynamoDB DYNAMO_DB = new DynamoDB(AmazonDynamoDBAsyncClientBuilder.standard().withRegion(REGION).build());
	private final Gson gson = new Gson();
	private AWSCognitoIdentityProvider cognitoClient;

	public Map<String, Object> handleRequest(Object request, Context context) {
		
		cognitoClient = AWSCognitoIdentityProviderClientBuilder.defaultClient();

		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		String rawPath = "";
		context.getLogger().log(request.toString());
		LinkedHashMap<String, Object> reqObj = (LinkedHashMap) request;

		if(reqObj.get("resource") instanceof String)
			rawPath = reqObj.get("resource").toString();

		if(rawPath.contains("/signup")) {// POST

			System.out.println("resource name: "+rawPath);
					
			System.out.println(reqObj.get("body").toString());
			System.out.println("System Env user pool "+USER_POOL);	
			JsonReader reader = new JsonReader(new StringReader(reqObj.get("body").toString()));
			reader.setLenient(true);
			User user = gson.fromJson(reader, User.class);
			System.out.println("before signup "+user.toString());

			boolean valid = EmailValidator.getInstance().isValid(user.getEmail());
			//String regx = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[$%^*]).{12,}$";
			//Pattern pattern = Pattern.compile(regx);
			if(valid && user.getFirstName()!=null && user.getLastName()!=null)
			{
				try{

					boolean issignup = signUp(cognitoClient,  
							getClientId(), 
							user.getFirstName(), 
							user.getLastName(), 
							user.getEmail(), 
							user.getPassword());
					
					System.out.println("is signup "+ issignup);


					// Confirm Signup	
					AdminConfirmSignUpRequest userRequest = new AdminConfirmSignUpRequest()
							.withUsername(user.getEmail())
							.withUserPoolId(getUserPoolId());           

					AdminConfirmSignUpResult response = cognitoClient.adminConfirmSignUp(userRequest);
					System.out.println("confirm signup response"+ response);

					AttributeType userAttrs = new AttributeType()
					.withName("email")
					.withValue(user.getEmail())
					.withName("email_verified")
					.withValue("true")
					;

					AdminUpdateUserAttributesRequest reqAttr = new AdminUpdateUserAttributesRequest()
					.withUserAttributes(userAttrs)
					.withUserPoolId(getUserPoolId())
					.withUsername(user.getEmail());
					AdminUpdateUserAttributesResult attRes = cognitoClient.adminUpdateUserAttributes(reqAttr);

					System.out.println("email verified updates"+attRes.toString());

					cognitoClient.shutdown();
					resultMap.put("statusCode", 200);
					System.out.println("valid data"+user.toString());
				}catch(Exception e){
					resultMap.put("statusCode", 400);					
				}
			}
			else{
				System.out.println("Invalid data"+user.toString());
				resultMap.put("statusCode", 400);
			}
		}
		else if(rawPath.contains("signin")) { // POST
			
			System.out.println("resource name: "+rawPath);			
			JsonReader reader = new JsonReader(new StringReader(reqObj.get("body").toString()));
			reader.setLenient(true);
			User user = gson.fromJson(reader, User.class);
			System.out.println(user.toString());

			boolean valid = EmailValidator.getInstance().isValid(user.getEmail());
			String regx = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[$%^*-_]).{12,}$";
			Pattern pattern = Pattern.compile(regx);

			if(valid && pattern.matcher("p12345T-048_Gru").matches())
			{
				try{
						AdminInitiateAuthResult response = initiateAuth(cognitoClient,getClientId(),user.getEmail(), user.getPassword(),getUserPoolId());

						if(response!=null)
						{
							resultMap.put("statusCode", 200);
							resultMap.put("body", "{" +
									"    \"statusCode\": 200," +
								"    \"accessToken\":\""+response.getAuthenticationResult().getIdToken()+"\"}");					
							cognitoClient.shutdown();
						}
						else{
							resultMap.put("statusCode", 400);							
						}
				}catch(Exception e){
					resultMap.put("statusCode", 400);
				}

			}else{
				resultMap.put("statusCode", 400);
			}
		} 
		else if(rawPath.contains("tables")) { //GET
			System.out.println("resource name: "+rawPath+" http method "+reqObj.get("httpMethod").toString());
			if(reqObj.get("httpMethod").toString().equals("GET")) {
				if(reqObj.get("pathParameters")!=null){
					System.out.println("in get path param "+reqObj.get("pathParameters"));
					Map<String, Double> jsonJavaRootObject = gson.fromJson(reqObj.get("pathParameters").toString(), Map.class);
					System.out.println(Double.valueOf(jsonJavaRootObject.get("tableId")).intValue());
					int tableId = Double.valueOf(jsonJavaRootObject.get("tableId")).intValue();
					System.out.println("table id "+tableId);
					if(tableId!=0){
							Table table = DYNAMO_DB.getTable(TABLE_TABLE);
							Item tableItem = table.getItem("id",tableId);
							resultMap.put("statusCode", 200);					
							resultMap.put("body", "{" +
							"    \"statusCode\": 200," +
						"    \"message\":\""+gson.toJson(tableItem.asMap())+"\"}");		
					}
					else{
							resultMap.put("statusCode", 400);
							resultMap.put("body", "{" +
							"    \"statusCode\": 400," +
						"    \"message\":\"Bad Request\"}");		
					}
				}
				else{
					AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(REGION).build();
					List<String> ls = new ArrayList<>();
					ls.add("id");
					ls.add("number");			
					ls.add("places");
					ls.add("minOrder");
					ls.add("isVip");
					ScanResult result = client.scan(TABLE_TABLE, ls);
					List<TableData> tList = new ArrayList<>();
					
					for(Map<String, AttributeValue> i : result.getItems()){	
						TableData t = new TableData();
						t.setId(Integer.parseInt(i.get("id").getN()));
						t.setNumber(Integer.parseInt(i.get("number").getN()));
						t.setPlaces(Integer.parseInt(i.get("places").getN()));
						t.setMinOrder(Integer.parseInt(i.get("minOrder").getN()));
						t.setVip(i.get("isVip").getBOOL());

						tList.add(t);
					}
					System.out.println(tList);
	
					// ScanRequest scanRequest = new ScanRequest()
					// 	.withTableName(TABLE_TABLE);

					// ScanResult result = client.scan(scanRequest);
					//  for (Map<String, AttributeValue> item : result.getItems()){
					// 	System.out.println(item.values());
					// }
					// result.getItems().listIterator();
					resultMap.put("statusCode", 200);					
					resultMap.put("body", "{" +
					"    \"statusCode\": 200," +
				"    \"message\":\""+gson.toJson(tList)+"\"}");

				}
			} else if(reqObj.get("httpMethod").toString().equals("POST")) {

				JsonReader reader = new JsonReader(new StringReader(reqObj.get("body").toString()));
				reader.setLenient(true);
				TableData tableObj = gson.fromJson(reader, TableData.class);

				if(tableObj.getId()!=0 && tableObj.getPlaces()!=0 && tableObj.getNumber()!=0){
						Table table = DYNAMO_DB.getTable(TABLE_TABLE);
						table.putItem(new Item()
								.withInt("id", tableObj.getId())
								.withInt("number", tableObj.getNumber())
								.withInt("places", tableObj.getPlaces())
								.withInt("minOrder", tableObj.getMinOrder())
								.withBoolean("isVip", tableObj.isVip()));

						Item item = table.getItem("id", tableObj.getId());

						System.out.println("inserted record "+item);
						System.out.println(item.get("id")); 

						resultMap.put("statusCode", 200);
						resultMap.put("body", "{" +
					"    \"id\":"+item.get("id")+"}");		
				}
				else{
						resultMap.put("statusCode", 400);
						resultMap.put("body", "{" +
						"    \"statusCode\": 400," +
					"    \"message\":\"Bad Request\"}");		
				}
			}


		}
		else if(rawPath.contains("reservations")) {
			System.out.println("resource name: "+rawPath);
			if(reqObj.get("httpMethod").equals("POST")) {
				JsonReader reader = new JsonReader(new StringReader(reqObj.get("body").toString()));
				reader.setLenient(true);
				Reservation reservationObj = gson.fromJson(reader, Reservation.class);
				if(reservationObj.getTableNumber()!=0 && reservationObj.getClientName()!=""){
						String id = java.util.UUID.randomUUID().toString();
						Table table = DYNAMO_DB.getTable(TABLE_RESERVATION);
						table.putItem(new Item()
						.withPrimaryKey("id",id)
								.withInt("tableNumber", reservationObj.getTableNumber())
								.withString("clientName", reservationObj.getClientName())
								.withString("phoneNumber", reservationObj.getPhoneNumber())
								.withString("date", reservationObj.getDate())
								.withString("slotTimeStart", reservationObj.getSlotTimeStart())
								.withString("slotTimeEnd", reservationObj.getSlotTimeEnd()));

						resultMap.put("statusCode", 200);
						resultMap.put("body", "{" +
						"    \"statusCode\": 200," +
					"    \"message\":\""+id+"\"}");		
				}
				else{
						resultMap.put("statusCode", 400);
						resultMap.put("body", "{" +
						"    \"statusCode\": 400," +
					"    \"message\":\"Bad Request\"}");		
				}
			} else if(reqObj.get("httpMethod").equals("GET")) {
				AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(REGION).build();
				// ScanRequest scanRequest = new ScanRequest()
				// 	.withTableName(TABLE_RESERVATION);
				
				List<String> ls = new ArrayList<>();
				ls.add("tableNumber");
				ls.add("clientName");			
				ls.add("phoneNumber");
				ls.add("date");
				ls.add("slotTimeStart");
				ls.add("slotTimeEnd");
				ScanResult result = client.scan(TABLE_RESERVATION, ls);
				List<Reservation> rList = new ArrayList<>();
				
				for(Map<String, AttributeValue> i : result.getItems()){	
					Reservation r = new Reservation();
					r.setTableNumber(Integer.parseInt(i.get("tableNumber").getN()));
					r.setClientName(i.get("clientName").getS());
					r.setPhoneNumber(i.get("phoneNumber").getS());
					r.setDate(i.get("date").getS());
					r.setSlotTimeStart(i.get("slotTimeStart").getS());
					r.setSlotTimeEnd(i.get("slotTimeEnd").getS());
					rList.add(r);
				}
				System.out.println(rList);
	
				resultMap.put("statusCode", 200);					
				resultMap.put("body", "{" +
				"    \"statusCode\": 200," +
			"    \"reservations\":\""+gson.toJson(rList)+"\"}");
			}
		}
		return resultMap;
	}

	  public static boolean signUp(AWSCognitoIdentityProvider identityProviderClient, 
	  String clientId, 
	  String firstName, 
	  String lastName,
      String email, 
	  String password) {
        AttributeType userAttrs = new AttributeType()
                .withName("firstName")
                .withValue(firstName)
				.withName("lastName")
                .withValue(lastName)
				.withName("email")
                .withValue(email);

        List<AttributeType> userAttrsList = new ArrayList<>();
        userAttrsList.add(userAttrs);
        try {
	            SignUpRequest signUpRequest = new SignUpRequest()
                    .withUserAttributes(userAttrsList)
                    .withUsername(email)
                    .withClientId(clientId)
                    .withPassword(password);
				SignUpResult result = identityProviderClient.signUp(signUpRequest);
				if(result.isUserConfirmed())
					System.out.println("User has been signed up ");
				return result.isUserConfirmed();

        } catch(Exception e) {
            System.err.println(e.getMessage());
			return false;
        }
    }

	private String getUserPoolId(){

		ListUserPoolsRequest request = new ListUserPoolsRequest();

		String userPoolId = "";
		ListUserPoolsResult response = cognitoClient.listUserPools(request);

		for(UserPoolDescriptionType s :  response.getUserPools()){
			if(s.getName().equals(USER_POOL))
			{
				userPoolId = s.getId();
				break;
			}
			
			}
			System.out.println("User pool Id"+userPoolId);
		
			return userPoolId;
	}

	private String getClientId() throws RuntimeException {
		ListUserPoolClientsRequest  req = 
				new ListUserPoolClientsRequest()
				.withUserPoolId(getUserPoolId());
	
    return cognitoClient.listUserPoolClients(req).getUserPoolClients().stream()
	         .filter(client -> client.getClientName().contains("client-app"))
             .findAny()
             .orElseThrow(() -> new RuntimeException("client 'client-app' not found"))
             .getClientId();
}
	
	public static AdminInitiateAuthResult initiateAuth(AWSCognitoIdentityProvider identityProviderClient,
			String clientId, String userName, String password, String userPoolId) {
			try {
					Map<String, String> authParameters = new HashMap<>();
					authParameters.put("USERNAME", userName);
					authParameters.put("PASSWORD", password);

					AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest()
					.withClientId(clientId)
					.withUserPoolId(userPoolId)
					.withAuthParameters(authParameters)
					.withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH);
					

					AdminInitiateAuthResult response = identityProviderClient.adminInitiateAuth(authRequest);
					System.out.println("Result access token is : " + response.getAuthenticationResult().getAccessToken());
					return response;

			} catch (Exception e) {
					System.err.println(e.getMessage());
					System.exit(1);
			}
			return null;
		}

}
