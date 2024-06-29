package com.task09;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import com.weather.report.HttpURLConnectionExample;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@LambdaHandler(lambdaName = "processor",
	roleName = "processor-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
	layers = "WeatherReportApi",
	tracingMode = TracingMode.Active
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

@DependsOn(name = "Weather", resourceType = ResourceType.DYNAMODB_TABLE)
@EnvironmentVariable(key = "table_weather", value = "${target_table}")
public class Processor implements RequestHandler<Object, Map<String, Object>> {

	private final Regions REGION = Regions.EU_CENTRAL_1;
	private static final String TABLE_AUDIT = System.getenv("table_weather");
	private final DynamoDB DYNAMO_DB = new DynamoDB(AmazonDynamoDBAsyncClientBuilder.standard().withRegion(REGION).build());

	public Map<String, Object> handleRequest(Object request, Context context) {

		Table table = DYNAMO_DB.getTable(TABLE_AUDIT);
		String id = java.util.UUID.randomUUID().toString();

		System.out.println("Start getting weather data");
		Map<String, Object> weatherData = getWeatherData();
		addWeatherData(table, id, weatherData);
		System.out.println("weather data saved");

		return weatherData;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getWeatherData(){
		
		Map<String, Object> response = null;
		try {
				Gson gson = new Gson();
				response = (Map<String, Object>)gson.fromJson(HttpURLConnectionExample.sendGET(), Map.class);
				for (Entry<String,Object> entry : response.entrySet()) {
					System.out.println(entry.getKey()+" : "+entry.getValue());
				}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return response;
		
	}
	
	private static void addWeatherData(Table table,String id, Map<String, Object> weatherData){
		Map<String,Object> forecast = new HashMap<>();
		Map<String,Object> hourly = new HashMap<>();

		System.out.println("Start saving weather data");
//		Gson gson = new Gson();
		Map<String, Object> tempHourly = (Map<String, Object>)weatherData.get("hourly");

		hourly.put("time", tempHourly.get("time"));
		hourly.put("temperature_2m", tempHourly.get("temperature_2m"));

		Map<String,Object> hourlyUnits = new HashMap<>();
		Map<String, Object> tempHourlyUnits = (Map<String, Object>)weatherData.get("hourly_units");

		hourlyUnits.put("temperature_2m",tempHourlyUnits.get("temperature_2m"));
		hourlyUnits.put("time",tempHourlyUnits.get("time"));

		forecast.put("elevation",weatherData.get("elevation"));
		forecast.put("generationtime_ms", weatherData.get("generationtime_ms"));
		
		forecast.put("hourly",hourly);
		forecast.put("hourly_units",hourlyUnits);

		forecast.put("latitude", weatherData.get("latitude"));
		forecast.put("longitude", weatherData.get("longitude"));
		
		forecast.put("timezone", weatherData.get("timezone"));
		forecast.put("timezone_abbreviation", weatherData.get("timezone_abbreviation"));
		
		forecast.put("utc_offset_seconds", weatherData.get("utc_offset_seconds"));

		table.putItem(new Item()
            .withPrimaryKey("id", id)
            .with("forecast", forecast));
		}

}
