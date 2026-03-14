package com.example.sp.service;

import com.example.sp.model.PriceRecord;
import java.util.Collection;
import java.util.Map;

public interface PriceService {

    void startBatch(String batchId);

    void uploadPrices(String batchId, Collection<PriceRecord> records);

    void completeBatch(String batchId);

    void cancelBatch(String batchId);

    Map<String, PriceRecord> getLatestPrices(Collection<String> instrumentIds);
}
