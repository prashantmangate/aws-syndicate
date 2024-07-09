package com.task10;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

public class Validation {
    private static final Regions REGION = Regions.EU_CENTRAL_1;
    private static final String TABLE_TABLE = System.getenv("tables_table");
	private static final String TABLE_RESERVATION = System.getenv("reservations_table");
	private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(REGION).build();
    //private static final DynamoDB DYNAMO_DB = new DynamoDB(AmazonDynamoDBAsyncClientBuilder.standard().withRegion(REGION).build());
	
    public static boolean isTableNumberEmpty(int tableNumber){ 
        try{
            System.out.println("table table name "+TABLE_TABLE);

            ScanRequest scanRequest = new ScanRequest().withTableName(TABLE_TABLE);
            ScanResult result = client.scan(scanRequest).withItems(Map.of(":tableNumber", new AttributeValue().withN(String.valueOf(tableNumber))));
            System.out.println("Table record "+result.getItems());  
            System.out.println("is table empty "+result.getItems().isEmpty());
            System.out.println("is table count "+result.getCount());
            
            if(result.getCount()==0 || result.getItems().isEmpty())
                return  true;
            else
                return false;
        }catch(Exception e){
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static Boolean isReservationFreeOfOverlapping(int tableNumber,String date, String slotTimeStart, String slotTimeEnd) {
            System.out.println("isReservationFreeOfOverlapping : tableNumber="+tableNumber+" date=" + date + " slotTimeStart=" + slotTimeStart + " slotTimeEnd=" + slotTimeEnd);
                    List<String> ls = new ArrayList<>();
					ls.add("tableNumber");
					ls.add("date");
					ls.add("slotTimeStart");
					ls.add("slotTimeEnd");


             ScanResult result = client.scan(TABLE_RESERVATION, ls)
            //ScanRequest scanRequest = new ScanRequest().withTableName(TABLE_RESERVATION);
            //ScanResult result = client.scan(scanRequest)
            .withItems(Map.of(":date", new AttributeValue().withS(date),":tableNumber", new AttributeValue().withN(String.valueOf(tableNumber))))
            ;

            System.out.println("reservation table name "+TABLE_RESERVATION);
            System.out.println("reservation items "+result.getItems());
            System.out.println("is reservation count "+result.getCount());
            
            if(result.getCount()==0 || result.getItems().isEmpty()) {
                System.out.println("isReservationFreeOfOverlapping TRUE");
                return true;
            }

            for (Map<String, AttributeValue> item : result.getItems()) {
                System.out.println("reservation item "+item);    
                if(item.get("slotTimeStart")!=null && item.get("slotTimeEnd")!=null && Integer.parseInt(item.get("tableNumber").getN())==tableNumber){          
                    String itemSlotTimeStart = item.get("slotTimeStart").getS();
                    String itemSlotTimeEnd = item.get("slotTimeEnd").getS();
                    //modify to check time overlapse

                    if (isOverlapping(slotTimeStart, slotTimeEnd, itemSlotTimeStart, itemSlotTimeEnd)){
                        System.out.println("isReservationFreeOfOverlapping FALSE");
                        return false;
                    }
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
