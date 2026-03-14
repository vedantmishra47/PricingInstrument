package com.example.sp.model;

import java.time.Instant;

public class PriceRecord {
    //All members are final to ensure thread safety
    private final String id;
    //without bothering about other timeZones
    private final Instant asOf;
    //since payload is flexible, so keeping it simple
    private final int payload;

    public PriceRecord(String id, Instant asOf, int payload) {
        this.id = id;
        this.asOf = asOf;
        this.payload = payload;
    }

    public String getId() { return id; }
    public Instant getAsOf() { return asOf; }
    public int getPayload() { return payload; }
}