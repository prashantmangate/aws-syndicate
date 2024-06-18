package com.task06.model;

import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

@DynamoDBTable(tableName = "Event")
public class Audit {

    private String id;
    private String itemKey;
    private String updatedAttribute;
    private Map<String, AttributeValue> oldValue;
    private Map<String, AttributeValue> newValue;

    @DynamoDBHashKey(attributeName = "id")
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;

    }

    @DynamoDBAttribute(attributeName = "itemKey")
    public String getItemKey() {
        return itemKey;
    }
    public void setItemKey(String itemKey) {
        this.itemKey = itemKey;
    }

    @DynamoDBAttribute(attributeName = "updatedAttribute")
    public String getUpdatedAttribute() {
        return updatedAttribute;
    }
    public void setUpdatedAttribute(String updatedAttribute) {
        this.updatedAttribute = updatedAttribute;
    }

    @DynamoDBAttribute(attributeName = "oldValue")
    public Map<String, AttributeValue> getOldValue() {
        return oldValue;
    }
    public void setOldValue(Map<String, AttributeValue> oldValue) {
        this.oldValue = oldValue;
    }

    @DynamoDBAttribute(attributeName = "newValue")
    public Map<String, AttributeValue> getNewValue() {
        return newValue;
    }
    public void setNewValue(Map<String, AttributeValue> newValue) {
        this.newValue = newValue;
    }

    
}
