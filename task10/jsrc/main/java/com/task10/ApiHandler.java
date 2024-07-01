package com.task10;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
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
		@EnvironmentVariable(key = "reservations_table", value = "${tables_table}"),
		@EnvironmentVariable(key = "booking_userpool", value = "${tables_table}")
	})
public class ApiHandler implements RequestHandler<Object, Map<String, Object>> {

	public Map<String, Object> handleRequest(Object request, Context context) {
		
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

		if(reqObj.get("rawPath") instanceof String)
			rawPath = reqObj.get("rawPath").toString();

		if(rawPath.contains("singup")) {// POST
			System.out.println("resource name: "+rawPath);
			// POST request will contain:
			// {
			// 	"firstName": // string
			// 	"lastName": // string
			// 	"email": // email validation
			// 	"password": // alphanumeric + any of "$%^*", 12+ chars
			// }

			resultMap.put("statusCode", 200);
			// resultMap.put("body", "{" +
			// 		"    \"statusCode\": 200" +
			// 		"}");
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
}
