package com.task10;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Table")
class TableData{

    private int id;
    private int number;
    private int places;
    private boolean isVip;
    private int minOrder;

    @DynamoDBHashKey(attributeName = "id")
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    @DynamoDBAttribute(attributeName = "number")
    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }

    @DynamoDBAttribute(attributeName = "places")
    public int getPlaces() {
        return places;
    }
    public void setPlaces(int places) {
        this.places = places;
    }

    @DynamoDBAttribute(attributeName = "isVip")
    public boolean isVip() {
        return isVip;
    }
    public void setVip(boolean isVip) {
        this.isVip = isVip;
    }

    @DynamoDBAttribute(attributeName = "minOrder")
    public int getMinOrder() {
        return minOrder;
    }
    public void setMinOrder(int minOrder) {
        this.minOrder = minOrder;
    }
    @Override
    public String toString() {
        return "TableData [id=" + id + ", number=" + number + ", places=" + places + ", isVip=" + isVip + ", minOrder="
                + minOrder + "]";
    }


}