package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.google.gson.Gson;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "hello_world-test",
	roleName = "hello_world-test-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
public class HelloWorldTest implements RequestHandler<Object, Map<String, Object>> {

	private static final int SC_OK = 200;
    private static final int SC_BAD_REQUEST = 400;
    private final Gson gson = new Gson();
/*
	@Override
	public  APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
		Map<String, Object> resultMap = new HashMap<String, Object>();

		context.getLogger().log(apiGatewayProxyRequestEvent.toString());
		String rawPath = "";
		APIGatewayProxyResponseEvent event = new APIGatewayProxyResponseEvent()
		if(apiGatewayProxyRequestEvent.getHeaders().containsKey("referer")){
			rawPath = apiGatewayProxyRequestEvent.getHeaders().get("referer");
			System.out.println("url in referer :-"+rawPath);
		}
		if(rawPath.contains("hello")) {
			System.out.println("url in if :-"+rawPath);
			return event
					.withStatusCode(SC_OK)
					.withBody(gson.toJson("Hello from Lambda"));

		}else if(!rawPath.contains("hello"))  {
//			System.out.println("url in elseif :-"+rawPath);
			return event
				.withStatusCode(SC_BAD_REQUEST)
				.withBody(gson.toJson("Bad request syntax or unsupported method. Request path: . HTTP method: Get"))
					.withHeaders(apiGatewayProxyRequestEvent.getHeaders());

		} else{
//			System.out.println("url in else :-"+rawPath);
			return event
					.withStatusCode(500)
					.withBody(gson.toJson("No query param found"))
					.withHeaders(apiGatewayProxyRequestEvent.getHeaders());

		}

	}*/

	@Override
	public  Map<String, Object> handleRequest(Object request, Context context) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String rawPath = "";
		context.getLogger().log(request.toString());
		LinkedHashMap<String, Object> reqObj = (LinkedHashMap) request;

		if(reqObj.get("rawPath") instanceof String)
			rawPath = reqObj.get("rawPath").toString();

		if(rawPath.contains("hello")) {
			resultMap.put("statusCode", 200);
			resultMap.put("body", "{" +
					"    \"statusCode\": 200," +
					"    \"message\": \"Hello from Lambda\"" +
					"}");
		} else {
			resultMap.put("statusCode", 400);
			resultMap.put("body", "{" +
					"    \"statusCode\": 400," +
					"    \"message\": \"Bad request syntax or unsupported method. Request path: "+rawPath+". HTTP method: Get\"" +
					"}");
		}
		resultMap.put("Content-Type", "application/json");
		return resultMap;
	}

}
