package com.task10;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.print.DocFlavor.READER;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolDescriptionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest.Builder;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsResponse;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = true,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
        @EnvironmentVariable(key = "tables_table", value = "${tables_table}"),
		@EnvironmentVariable(key = "reservations_table", value = "${reservations_table}"),
		@EnvironmentVariable(key = "booking_userpool", value = "${booking_userpool}")
	})
public class ApiHandler implements RequestHandler<Object, Map<String, Object>> {

	private static final String USER_POOL = System.getenv("booking_userpool");
	private static final Region REGION = Region.EU_CENTRAL_1;
	private final Gson gson = new Gson();

	private CognitoIdentityProviderClient identityProviderClient;

	public Map<String, Object> handleRequest(Object request, Context context) {
		
		identityProviderClient = CognitoIdentityProviderClient.builder().region(REGION).build();

		Map<String, Object> resultMap = new HashMap<String, Object>();
		
		String rawPath = "";
		context.getLogger().log(request.toString());
		LinkedHashMap<String, Object> reqObj = (LinkedHashMap) request;

		/* 
		/signup POST
		/signin POST
		/tables POST
		/tables GET
		/reservations POST
		/reservations GET
		*/

		if(reqObj.get("resource") instanceof String)
			rawPath = reqObj.get("resource").toString();

		if(rawPath.contains("/signup")) {// POST

			System.out.println("resource name: "+rawPath);
			 Builder createUserRequest = AdminCreateUserRequest.builder().messageAction(MessageActionType.SUPPRESS);
			
			 // createUserRequest
			// .username(reqObj.get("email").toString())
			// .temporaryPassword("Aws1#AwsCloud2#")
			// .userPoolId(userPoolId);
			System.out.println(reqObj.get("body").toString());

			JsonReader reader = new JsonReader(new StringReader(reqObj.get("body").toString()));
			reader.setLenient(true);
			User user = gson.fromJson(reader, User.class);
			System.out.println(user.toString());
			signUp(identityProviderClient,  
					getClientId(), 
					user.getFirstName(), 
					user.getLastName(), 
					user.getEmail(), 
					user.getPassword());

			identityProviderClient.close();
			resultMap.put("statusCode", 200);			
		}
		else if(rawPath.contains("singin")) { // POST
			System.out.println("resource name: "+rawPath);
			// POST request will contain : 
			// {
			// 	"email": // email
			// 	"password": // alphanumeric + any of "$%^*", 12+ chars
			// }
			String accessTocken="";
			resultMap.put("statusCode", 200);
			resultMap.put("accessToken",accessTocken);
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

	  public static void signUp(CognitoIdentityProviderClient identityProviderClient, 
	  String clientId, 
	  String firstName, 
	  String lastName,
      String email, 
	  String password) {
        AttributeType userAttrs = AttributeType.builder()
                .name("firstName")
                .value(firstName)
				.name("lastName")
                .value(lastName)
				.name("email")
                .value(email)
                .build();

        List<AttributeType> userAttrsList = new ArrayList<>();
        userAttrsList.add(userAttrs);
        try {
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .userAttributes(userAttrsList)
                    .username(email)
                    .clientId(clientId)
                    .password(password)
                    .build();

            identityProviderClient.signUp(signUpRequest);
            System.out.println("User has been signed up ");

        } catch (CognitoIdentityProviderException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
    }

	private String getClientId() throws RuntimeException {

		    ListUserPoolsRequest request = ListUserPoolsRequest.builder().
                    build();
			String userPoolId = "";
        	ListUserPoolsResponse response = identityProviderClient.listUserPools(request);
           	for(UserPoolDescriptionType s :  response.userPools()){
				System.out.println("user pool name "+s.name());
				// Try printing USER_POOL as in log user pool id not print
				if(s.name().equals("cmtr-add4fd60-"+USER_POOL))
				{
					userPoolId = s.id();
					break;
				}
			}
			System.out.println("User pool Id"+userPoolId);
    return identityProviderClient.listUserPoolClients(ListUserPoolClientsRequest.builder()
                    .userPoolId(userPoolId)
                    .maxResults(1)
                    .build())
            .userPoolClients().stream()
            .filter(client -> client.clientName().contains("client-app"))
            .findAny()
            .orElseThrow(() -> new RuntimeException("client 'client-app' not found"))
            .clientId();
}
}
