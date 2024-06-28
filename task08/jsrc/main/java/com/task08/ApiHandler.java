package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import com.weather.report.HttpURLConnectionExample;

import java.io.IOException;
import java.util.Map;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = true,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
	layers = "WeatherReportApi"
)

@LambdaLayer(layerName = "WeatherReportApi",
		libraries = {"java/lib/commons-lang3-3.14.0.jar", "java/lib/gson-2.10.1.jar","java/lib/WeatherReportApi-1.0-SNAPSHOT.jar"},
		runtime = DeploymentRuntime.JAVA11,
		architectures = {Architecture.ARM64},
		artifactExtension = ArtifactExtension.ZIP)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
// Tomorrow take reference of this doc
// https://github.com/epam/aws-syndicate/blob/master/examples/java/demo-layer-url/pom.xml
// lambda function not deployed after changing in pom .xml

// build error fix and lambda deployed but not getting
// below issue
// {
//  "errorMessage": "Error loading class com.task08.ApiHandler: com/task08/ApiHandler has been compiled by a more recent version of the Java Runtime (class file version 61.0), this version of the Java Runtime only recognizes class file versions up to 55.0",
//  "errorType": "java.lang.UnsupportedClassVersionError"
//}
public class ApiHandler implements RequestHandler<Object, Map<String, Object>> {

	private final Gson gson = new Gson();
	public Map<String, Object> handleRequest(Object request, Context context) {
		Map response = null;
		try {
			Gson gson = new Gson();
			 response = gson.fromJson(HttpURLConnectionExample.sendGET(), Map.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return response;
	}
}
