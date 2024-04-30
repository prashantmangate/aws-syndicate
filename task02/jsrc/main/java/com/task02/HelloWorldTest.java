package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "hello_world-test",
	roleName = "hello_world-test-role",
	isPublishVersion = true,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaUrlConfig(
        authType = AuthType.NONE,
        invokeMode = InvokeMode.BUFFERED
)
public class HelloWorldTest implements RequestHandler<Object, Map<String, Object>> {

	public Map<String, Object> handleRequest(Object request, Context context) {

		System.out.println(request);
		System.out.println(request.getClass());
		LinkedHashMap<Object,Map<String,Object>> req1 = request;
		//request.getClass().entrySet().forEach(System.out::println);
/* 
		System.out.println(" raw path value - "+req.get("rawPath"));
		LinkedHashMap<String,Object> req1 = request;
		LinkedHashMap<String,Object> req2 = req1.get("requestContext");
		LinkedHashMap<String,Object> req3 = req2.get("http");
		
		System.out.println(" htt path method - "+req3.get("method"));
*/
		
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("statusCode", 200);
		resultMap.put("message", "Hello from Lambda");
		return resultMap;

		/*
		if(reqObj.get("rawPath").equals("/hello")) {

			resultMap.put("statusCode", 200);
			resultMap.put("message", "Hello from Lambda");
			return resultMap;
		}
		else{
			LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap>> reqCon = reqObj.get("requestContext");
			LinkedHashMap<K,V> http = reqCon.get("http");
			String  str = "Bad request syntax or unsupported method. Request path: "+reqObj.get("rawPath")+". HTTP method:"
			+http.get("method");	
			resultMap.put("statusCode", 400);
			resultMap.put("message", str);
			return resultMap;
		}*/
	}
}
