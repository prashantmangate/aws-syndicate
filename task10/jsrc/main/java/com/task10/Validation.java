package com.task10;

import java.time.LocalTime;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

public class Validation {
    private static final Regions REGION = Regions.EU_CENTRAL_1;
    private static final String TABLE_TABLE = System.getenv("tables_table");
	private static final String TABLE_RESERVATION = System.getenv("reservations_table");
	private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(REGION).build();
    private static final DynamoDB DYNAMO_DB = new DynamoDB(AmazonDynamoDBAsyncClientBuilder.standard().withRegion(REGION).build());
	
    public static int checkTableExist(int tableNumber){ 
        int tableNum = 0;
        try{
            Table table =  DYNAMO_DB.getTable(TABLE_TABLE);       
            Item item = table.getItem("number", tableNumber);
            System.out.println(item.get("number")); 
            tableNum = item.getInt("number"); 
            return tableNum;
        }catch(Exception e){
            System.out.println(e.getMessage()+" table number "+tableNum);
            return tableNum;
        }
    }

    public static Boolean isReservationFreeOfOverlapping(Integer tableNumber,String date, String slotTimeStart, String slotTimeEnd) {
            System.out.println("isReservationFreeOfOverlapping date=" + date + " slotTimeStart=" + slotTimeStart + " slotTimeEnd=" + slotTimeEnd);
            ScanRequest scanRequest = new ScanRequest().withTableName(TABLE_RESERVATION);
            @SuppressWarnings("unchecked")
            ScanResult result = client.scan(scanRequest).withItems(Map.of(":date", new AttributeValue().withS(date),
                                                        ":tableNumber", new AttributeValue().withN(tableNumber.toString())));
            if (result.getItems().isEmpty()) {
                System.out.println("isReservationFreeOfOverlapping TRUE");
                return true;
            }

            for (Map<String, AttributeValue> item : result.getItems()) {
                String itemSlotTimeStart = item.get("slotTimeStart").getS();
                String itemSlotTimeEnd = item.get("slotTimeEnd").getS();
                //modify to check time overlapse

                if (isOverlapping(slotTimeStart, slotTimeEnd, itemSlotTimeStart, itemSlotTimeEnd)){
                    System.out.println("isReservationFreeOfOverlapping FALSE");
                    return false;
                }
            }

            return true;
        }

        public static boolean isOverlapping(String start0, String end0, String start1, String end1) {
            LocalTime startInterval0 = LocalTime.parse(start0);
            LocalTime endInterval0 = LocalTime.parse(end0);
            LocalTime startInterval1 = LocalTime.parse(start1);
            LocalTime endInterval1 = LocalTime.parse(end1);
            return startInterval0.isBefore(endInterval1) && startInterval1.isBefore(endInterval0);
        }

}
