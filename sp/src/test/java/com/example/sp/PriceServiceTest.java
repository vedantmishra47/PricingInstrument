package com.example.sp;

import com.example.sp.model.PriceRecord;
import com.example.sp.service.PriceServiceImpl;
import com.example.sp.service.PriceService;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PriceServiceTest {

	private PriceService service;

	@BeforeEach
	void setup() {
		service = new PriceServiceImpl();
	}
	//testing atomicity
	@Test
	void testAtomicBatchCommit() {
		service.startBatch("B1");

		service.uploadPrices("B1", List.of(
				new PriceRecord("AAPL", Instant.parse("2023-10-10T10:00:00Z"), 150),
				new PriceRecord("GOOG", Instant.parse("2023-10-10T10:00:00Z"), 2500))
		);

		// Before commit -> should not see data
		assertTrue(service.getLatestPrices(List.of("AAPL")).isEmpty());

		service.completeBatch("B1");

		assertEquals(150, service.getLatestPrices(List.of("AAPL")).get("AAPL").getPayload());
	}
	//price will be updated on the basis if timestamped,not on basis of order in the batch file
	@Test
	void testOrderCheck() {
		service.startBatch("B2");

		service.uploadPrices("B2", List.of(
				new PriceRecord("AAPL", Instant.parse("2023-10-10T11:00:00Z"), 152),
				new PriceRecord("AAPL", Instant.parse("2023-10-10T09:00:00Z"), 154)
		));

		service.completeBatch("B2");

		assertEquals(152,
				service.getLatestPrices(List.of("AAPL")).get("AAPL").getPayload());
	}
	// if cancelBatch is invoked before completeBatch then whole transaction of that batch will be rolled back
	@Test
	void testCancelBatch() {
		service.startBatch("B3");
		service.uploadPrices("B3", List.of(
				new PriceRecord("MSFT", Instant.now(), 300)
		));

		service.cancelBatch("B3");

		assertTrue(service.getLatestPrices(List.of("MSFT")).isEmpty());
	}
	//1001 record check
	@Test
	void shouldProcessTailChunkIncludingRecord1001() {
		service.startBatch("CHK");
		List<PriceRecord> recs = new ArrayList<>(1001);
		for (int i = 0; i < 1001; i++) {
			recs.add(new PriceRecord("ID-" + i, Instant.parse("2023-10-10T10:00:00Z"), i));
		}

		// This uses your chunked uploadPrices implementation
		service.uploadPrices("CHK", recs);
		service.completeBatch("CHK");

		var last = service.getLatestPrices(List.of("ID-1000")).get("ID-1000"); // 0-based index 1000
		assertNotNull(last);               // proves record #1001 is present
		assertEquals(1000, last.getPayload());
	}
}
