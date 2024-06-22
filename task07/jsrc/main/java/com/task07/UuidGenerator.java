package com.task07;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import com.syndicate.deployment.annotations.events.RuleEventSource;

@LambdaHandler(lambdaName = "uuid_generator",
	roleName = "uuid_generator-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)


@RuleEventSource(
targetRule="uuid_trigger"
)

@DependsOn(
	name = "uuid_trigger",
	resourceType = ResourceType.CLOUDWATCH_RULE
)

@EnvironmentVariable (key = "bucket_name", value = "${target_bucket}")
@EnvironmentVariable (key = "region", value = "${region}")
public class UuidGenerator implements RequestHandler<ScheduledEvent, Void> {
	
	private static final String BUCKET_NAME = System.getenv("bucket_name");
	private static final Region REGION = Region.EU_CENTRAL_1;
    private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Void handleRequest(ScheduledEvent event, Context context) {
	
		List<String> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(UUID.randomUUID().toString());
        }

        var result = new Result(ids);
        var json = convertObjectToJson(result);


        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S").format(Calendar.getInstance().getTime());
        System.out.println(timeStamp);

        File file = null;
        String tempDir = "";
         try {
             tempDir = System.getProperty("java.io.tmpdir");
            file = new File(tempDir + File.separator +timeStamp.replace(":", "_")+ ".json");
            Path newFilePath = Paths.get(file.toURI());
            Files.createFile(newFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        context.getLogger().log("File created");

		S3Client s3 = S3Client.builder()
                .region(REGION)
                .build();

        PutObjectRequest putObjectRequest = PutObjectRequest
		.builder()
		.bucket(BUCKET_NAME)
		.key(timeStamp)
		.build();//(BUCKET_NAME, timestamp + ".json", file);
        
		s3.putObject(putObjectRequest,RequestBody.fromFile(file));
        
		context.getLogger().log("Object put");
        try {
            Files.delete(Paths.get(tempDir + File.separator +timeStamp.replace(":", "_")+ ".json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        context.getLogger().log("File uploaded successfully: " + tempDir + File.separator +timeStamp.replace(":", "_")+ ".json");

        return null;
	}

	private static String convertObjectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Object cannot be converted to JSON: " + object);
        }
    }

    static class Result {
        private List<String> ids;

        public Result() {
        }

        public Result(List<String> ids) {
            this.ids = ids;
        }

        public List<String> getIds() {
            return ids;
        }

        public void setIds(List<String> ids) {
            this.ids = ids;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "ids=" + ids +
                    '}';
        }
    }
}
