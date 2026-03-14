package com.example.sp.service;

import com.example.sp.exception.BatchException;
import com.example.sp.model.BatchJobs;
import com.example.sp.model.PriceRecord;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

@Service
public class PriceServiceImpl implements PriceService {

    private final ConcurrentHashMap<String, PriceRecord> committed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BatchJobs> batches = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock commitLock = new ReentrantReadWriteLock();
    public static final int MAX_CHUNK_SIZE = 1000;
    @Override
    public void startBatch(String batchId) {
        if (batches.putIfAbsent(batchId, new BatchJobs()) != null) {
            throw new BatchException("Batch already started: " + batchId);
        }
    }

    @Override
    public void uploadPrices(String batchId, Collection<PriceRecord> records) {
        final BatchJobs job = batches.get(batchId);
        if (job == null) {
            throw new BatchException("Batch not started: " + batchId);
        }
        List<PriceRecord> list = (records instanceof List)
                ? (List<PriceRecord>) records
                : new ArrayList<>(records);

        int n = list.size();
        int chunkSize = 1000;

        //Create a stream of starting indices (0, 1000, 2000...)
        IntStream.range(0, (n + chunkSize - 1) / chunkSize)
                .parallel()  //Parallelize the chunks
                .forEach(chunkIndex -> {
                    int start = chunkIndex * chunkSize;
                    int end = Math.min(start + chunkSize, n);

        // Process this specific 1000-record chunk
                    List<PriceRecord> chunk = list.subList(start, end);
                    for (PriceRecord r : chunk) {
                        job.add(r);
                    }
                });
    }

    @Override
    public void completeBatch(String batchId) {
        BatchJobs job = batches.remove(batchId);
        if (job == null)
            throw new BatchException("Batch does not exist: " + batchId);

        commitLock.writeLock().lock();//write lock to perform writing one thread at a time, this segment of hashmap wouldn't be available for read
        try {
            //publish records in permanent concurrentHashMap, visible to consumers
            for (PriceRecord r : job.getAll().values()) {
                committed.merge(
                        r.getId(),
                        r,
                        (oldV, newV) -> newV.getAsOf().isAfter(oldV.getAsOf()) ? newV : oldV
                );
            }
        } finally {
            commitLock.writeLock().unlock();
        }
    }

    @Override
    public void cancelBatch(String batchId) {
        batches.remove(batchId); // discard temp data
    }

    @Override
    public Map<String, PriceRecord> getLatestPrices(Collection<String> ids) {
        commitLock.readLock().lock();//read lock to perform reads from multiple threads, at this moment no write operation will be possible
        try {
            Map<String, PriceRecord> result = new HashMap<>();
            for (String id : ids) {
                PriceRecord r = committed.get(id);
                if (r != null) result.put(id, r);
            }
            return result;
        } finally {
            commitLock.readLock().unlock();
        }
    }
}
