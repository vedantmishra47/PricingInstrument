package com.example.sp.model;
import java.util.concurrent.ConcurrentHashMap;

public class BatchJobs {
    // temp price storage for this batch, opting for concurrent hashMap for thread safety
    private final ConcurrentHashMap<String, PriceRecord> tempPrices = new ConcurrentHashMap<>();
    // insert all records in temporary HashMap
    public void add(PriceRecord record) {
        tempPrices.merge(
                record.getId(),
                record,
                (oldV, newV) -> newV.getAsOf().isAfter(oldV.getAsOf()) ? newV : oldV
        );
    }
    // returns all the records to be inserted into permanent HashMap
    public ConcurrentHashMap<String, PriceRecord> getAll() {
        return tempPrices;
    }
}
