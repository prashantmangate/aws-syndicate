package com.task10;

import java.util.Map;
import com.google.gson.Gson;

public class JsonTest {

    @SuppressWarnings("unchecked")
    public static void main(String[] args){
        Gson gson = new Gson();
        String s = "{tableId=1}";
        Map<String, Double> jsonJavaRootObject = gson.fromJson(s, Map.class);
        System.out.println(Double.valueOf(jsonJavaRootObject.get("tableId")).intValue());

    }
}
