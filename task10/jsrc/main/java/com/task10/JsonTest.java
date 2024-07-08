package com.task10;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;

public class JsonTest {

    @SuppressWarnings("unchecked")
    public static void main(String[] args){
        Gson gson = new Gson();
        String s = "{tableId=1}";
        Map<String, Double> jsonJavaRootObject = gson.fromJson(s, Map.class);
        System.out.println(Double.valueOf(jsonJavaRootObject.get("tableId")).intValue());
        
        List<Reservation> rL = new ArrayList<>();   
        Reservation r = new Reservation();
        r.setTableNumber(324);
        r.setClientName("asd");
        r.setClientName("dfgdfg");
        r.setClientName("dfgsdfg");
        r.setClientName("sdsdf");
        r.setClientName("sdsdf");

        rL.add(r);

        Reservation r1 = new Reservation();
        r1.setTableNumber(324);
        r1.setClientName("asd");
        r1.setClientName("dfgdfg");
        r1.setClientName("dfgsdfg");
        r1.setClientName("sdsdf");
        r1.setClientName("sdsdf");
        
        rL.add(r1);

 //       System.out.println(rL);
       System.out.println(gson.toJson(rL));
    }
}
