package com.task10;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.regions.Region;
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
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
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
	//private static final Region REGION = ;
	private final Gson gson = new Gson();

	//private CognitoIdentityProviderClient identityProviderClient;
	private AWSCognitoIdentityProvider cognitoClient;
	public Map<String, Object> handleRequest(Object request, Context context) {
		
	//	identityProviderClient = CognitoIdentityProviderClient.builder().region(REGION).build();
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


			//create user : ENd
			System.out.println("user confirmed"+response.getSdkResponseMetadata());
			System.out.println("after confirmed signup "+user.toString());
			Map<String,String> initialParams = new HashMap<String,String>();
            initialParams.put("USERNAME", user.getEmail());
            initialParams.put("PASSWORD", user.getPassword());
	
	

			AdminInitiateAuthRequest authRequest = 
			new AdminInitiateAuthRequest()
                    .withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH.name())
                    .withAuthParameters(initialParams)
                    .withClientId(getClientId())
                    .withUserPoolId(getUserPoolId());


			AdminInitiateAuthResult initialResponse = null;
			System.out.println("Client Id"+getClientId()+" user pool id "+getUserPoolId());
//			try{
		  			// System.out.print("Confirm signup : admin initiate auth called after signup");

			        //  initialResponse = cognitoClient.adminInitiateAuth(authRequest);
					//  System.out.println("response after admin auth: "+initialResponse.toString());
					//  System.out.println("adminInitiateAuth challenge session "+initialResponse.getSession());
					//  System.out.println("adminInitiateAuth challenge name "+initialResponse.getChallengeName());

					//  Map<String,String> challengeResponses = new HashMap<String,String>();
					//  challengeResponses.put("USERNAME", user.getEmail());
					//  challengeResponses.put("PASSWORD", user.getPassword());
					//  challengeResponses.put("NEW_PASSWORD", user.getPassword());
 
					//  AdminRespondToAuthChallengeRequest finalRequest = new AdminRespondToAuthChallengeRequest()
					//  .withChallengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
					//  .withChallengeResponses(challengeResponses)
					//  .withClientId(getClientId())
					//  .withUserPoolId(getUserPoolId())
					//  .withSession(initialResponse.getSession());
 

					//  AdminRespondToAuthChallengeResult challengeResponse = cognitoClient.adminRespondToAuthChallenge(finalRequest);
					//  System.out.println("Challenge Response "+challengeResponse.getAuthenticationResult().getAccessToken());
 
					//  cognitoClient.shutdown();
					//  resultMap.put("statusCode", 200);		

					// }catch(Exception e){
					// e.printStackTrace();
					// System.out.println(e.getMessage());
			//}	
			cognitoClient.shutdown();
			resultMap.put("statusCode", 200);	
		}
		else if(rawPath.contains("signin")) { // POST
			
			System.out.println("resource name: "+rawPath);			
			JsonReader reader = new JsonReader(new StringReader(reqObj.get("body").toString()));
			reader.setLenient(true);
			User user = gson.fromJson(reader, User.class);
			System.out.println(user.toString());

			AdminInitiateAuthResult response = initiateAuth(cognitoClient,getClientId(),user.getEmail(), user.getPassword(),getUserPoolId());
			resultMap.put("statusCode", 200);
			resultMap.put("accessToken",response.getAuthenticationResult().getAccessToken());
			cognitoClient.shutdown();
		} 
		else if(rawPath.contains("tables")) { //GET
			System.out.println("resource name: "+rawPath);
			if(resultMap.get("httpMethod").equals("GET")) {
				// Headers:
				// Authorization: Bearer $accessToken
				// Request: {}
				// return Response:
				// 	{
				// 		"tables": [
				// 			{
				// 				"id": // int
				// 				"number": // int, number of the table
				// 				"places": // int, amount of people to sit at the table
				// 				"isVip": // boolean, is the table in the VIP hall
				// 				"minOrder": // optional. int, table deposit required to book it
				// 			},
				// 			...
				// 		]
				// 	}

				// if any get parameter then /table/{tableId} 
				// Headers:
				// Authorization: Bearer $accessToken
				// Request: {}
				// return Response:
				// {
				// "id": // int
				// "number": // int, number of the table
				// "places": // int, amount of people to sit at the table
				// "isVip": // boolean, is the table in the VIP hall
				// "minOrder": // optional. int, table deposit required to book it
				// }

			} else if(resultMap.get("httpMethod").equals("POST")) {
				// Headers:
				// Authorization: Bearer $accessToken
				// POST data in Request:
				// 	{
				// 		"id": // int
				// 		"number": // int, number of the table
				// 		"places": // int, amount of people to sit at the table
				// 		"isVip": // boolean, is the table in the VIP hall
				// 		"minOrder": // optional. int, table deposit required to book it
				// 	}

				// process data logic 
				// return below response 
				// {
				// "id": $table_id // int, id of the created table
				// }
			}

			resultMap.put("statusCode", 200);
	
		}
		else if(rawPath.contains("reservations")) {
			System.out.println("resource name: "+rawPath);
			if(resultMap.get("httpMethod").equals("POST")) {
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
			} else if(resultMap.get("httpMethod").equals("GET")) {
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
			resultMap.put("statusCode", 200);
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
                    .withPassword(password)
                    ;

            SignUpResult resp = identityProviderClient.signUp(signUpRequest);
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

	// public static void confirmSignUp(CognitoIdentityProviderClient identityProviderClient, String clientId, String code,
    //         String userName) {
	// 			try {
	// 			ConfirmSignUpRequest signUpRequest = ConfirmSignUpRequest.builder()
	// 					.clientId(clientId)
	// 					.confirmationCode(code)
	// 					.username(userName)
	// 					.build();

	// 			identityProviderClient.confirmSignUp(signUpRequest);
	// 			System.out.println(userName + " was confirmed");

	// 		} catch (CognitoIdentityProviderException e) {
	// 			System.err.println(e.awsErrorDetails().errorMessage());
	// 			System.exit(1);
	// 		}
	// 	}
}
