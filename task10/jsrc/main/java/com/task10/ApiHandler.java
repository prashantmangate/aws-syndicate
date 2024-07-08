package com.task10;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AddCustomAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AdminConfirmSignUpRequest;
import com.amazonaws.services.cognitoidp.model.AdminConfirmSignUpResult;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminCreateUserResult;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.AdminRespondToAuthChallengeRequest;
import com.amazonaws.services.cognitoidp.model.AdminRespondToAuthChallengeResult;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AssociateSoftwareTokenRequest;
import com.amazonaws.services.cognitoidp.model.AssociateSoftwareTokenResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.ChallengeNameType;
import com.amazonaws.services.cognitoidp.model.CreateUserPoolClientRequest;
import com.amazonaws.services.cognitoidp.model.ListUserPoolClientsRequest;
import com.amazonaws.services.cognitoidp.model.ListUserPoolsRequest;
import com.amazonaws.services.cognitoidp.model.ListUserPoolsResult;
import com.amazonaws.services.cognitoidp.model.SignUpRequest;
import com.amazonaws.services.cognitoidp.model.SignUpResult;
import com.amazonaws.services.cognitoidp.model.UserPoolDescriptionType;
import com.amazonaws.services.cognitoidp.model.VerifySoftwareTokenRequest;
import com.amazonaws.services.cognitoidp.model.VerifySoftwareTokenResult;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.xspec.ScanExpressionSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

// import software.amazon.awssdk.regions.Region;
// import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsResponse;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
// import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolDescriptionType;

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
			signUp(cognitoClient,  
					getClientId(), 
					user.getFirstName(), 
					user.getLastName(), 
					user.getEmail(), 
					user.getPassword());

			// Confirm Signup	

			//create user : start

            AdminConfirmSignUpRequest userRequest = new AdminConfirmSignUpRequest()
					.withUsername(user.getEmail())
                    .withUserPoolId(getUserPoolId());           

            AdminConfirmSignUpResult response = cognitoClient.adminConfirmSignUp(userRequest);

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
			cognitoClient.adminUpdateUserAttributes(reqAttr);


			System.out.println("user confirmed"+response.getSdkResponseMetadata());
			System.out.println("after confirmed signup "+user.toString());

			cognitoClient.shutdown();
			resultMap.put("statusCode", 200);
			resultMap.put("body", "{" +
					"    \"statusCode\": 200," +
					"    \"message\": \"User successfully signedup\"" +
					"}");
					
		}
		else if(rawPath.contains("signin")) { // POST
			
			System.out.println("resource name: "+rawPath);			
			JsonReader reader = new JsonReader(new StringReader(reqObj.get("body").toString()));
			reader.setLenient(true);
			User user = gson.fromJson(reader, User.class);
			System.out.println(user.toString());

			AdminInitiateAuthResult response = initiateAuth(cognitoClient,getClientId(),user.getEmail(), user.getPassword(),getUserPoolId());

			resultMap.put("statusCode", 200);
			resultMap.put("body", "{" +
					"    \"statusCode\": 200," +
				"    \"message\":\""+response.getAuthenticationResult().getIdToken()+"\"}");
					
			cognitoClient.shutdown();
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
					Table table = DYNAMO_DB.getTable(TABLE_TABLE);
					table.scan(rawPath, null, reqObj);
					AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(REGION).build();
					ScanRequest scanRequest = new ScanRequest()
						.withTableName(TABLE_TABLE);

					ScanResult result = client.scan(scanRequest);
					 for (Map<String, AttributeValue> item : result.getItems()){
						System.out.println(item.values());
					}
					result.getItems().listIterator();
					resultMap.put("statusCode", 200);					
					resultMap.put("body", "{" +
					"    \"statusCode\": 200," +
				"    \"message\":\""+gson.toJson(result.getItems())+"\"}");

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
								.withInt("minOrder", tableObj.getMinOrder()));

						Item item = table.getItem("id", tableObj.getId());

						System.out.println("inserted record "+item);
						System.out.println(item.get("id")); 

						resultMap.put("statusCode", 200);
						resultMap.put("body", "{" +
						"    \"statusCode\": 200," +
					"    \"message\":\""+item.get("id")+"\"}");		
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
				// Headers:
				// Authorization: Bearer $accessToken
				// Request:
				// {
				// "tableNumber": // int, number of the table
				// "clientName": //string
				// "phoneNumber": //string
				// "date": // string in yyyy-MM-dd format
				// "slotTimeStart": // string in "HH:MM" format, like "13:00",
				// "slotTimeEnd": // string in "HH:MM" format, like "15:00"
				// }
				// Response:
				// {
				// "reservationId": // string uuidv4
				// }
			} else if(reqObj.get("httpMethod").equals("GET")) {
				// Headers:
				// Authorization: Bearer $accessToken
				// Request: {}
				// Response:
				// {
				// "reservations": [
				// 	{
				// 		"tableNumber": // int, number of the table
				// 		"clientName": //string
				// 		"phoneNumber": //string
				// 		"date": // string in yyyy-MM-dd format
				// 		"slotTimeStart": // string in "HH:MM" format, like "13:00",
				// 		"slotTimeEnd": // string in "HH:MM" format, like "15:00"
				// 	}
				// ]
				// }
			}
		}
		return resultMap;
	}

	  public static void signUp(AWSCognitoIdentityProvider identityProviderClient, 
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
				identityProviderClient.signUp(signUpRequest);
	            System.out.println("User has been signed up ");
        } catch(Exception e) {
            System.err.println(e.getMessage());
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
