/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.browse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import org.roda.core.data.common.RodaConstants;
import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.ip.IndexedAIP;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tester för batchad uppslagning av förfäder i katalogträdet (#643).
 * Solr avvisar frågor med fler än 1024 OR-led, så ID-listan måste delas upp.
 */
@Test(groups = {RodaConstants.TEST_GROUP_ALL})
public class AncestorBatchResolverTest {

  private static List<String> ids(int n) {
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      ids.add("id-" + i);
    }
    return ids;
  }

  private static IndexResult<IndexedAIP> resultFor(List<String> batch) {
    List<IndexedAIP> aips = new ArrayList<>();
    for (String id : batch) {
      IndexedAIP aip = new IndexedAIP();
      aip.setId(id);
      aip.setTitle("Titel " + id);
      aips.add(aip);
    }
    return new IndexResult<>(0, batch.size(), batch.size(), aips, Collections.emptyList());
  }

  @Test
  public void partitionSplits1100IdsInto500And500And100() {
    List<List<String>> batches = AncestorBatchResolver.partition(ids(1100), 500);

    Assert.assertEquals(batches.size(), 3);
    Assert.assertEquals(batches.get(0).size(), 500);
    Assert.assertEquals(batches.get(1).size(), 500);
    Assert.assertEquals(batches.get(2).size(), 100);
    Assert.assertEquals(batches.get(0).get(0), "id-0");
    Assert.assertEquals(batches.get(2).get(99), "id-1099");
  }

  @Test
  public void partitionOfEmptyCollectionGivesNoBatches() {
    Assert.assertTrue(AncestorBatchResolver.partition(Collections.emptyList(), 500).isEmpty());
  }

  @Test
  public void resolveNeverSendsMoreThanMaxIdsPerRequestAndMergesAllResults() {
    List<Integer> requestSizes = new ArrayList<>();
    Function<List<String>, CompletableFuture<IndexResult<IndexedAIP>>> finder = batch -> {
      requestSizes.add(batch.size());
      return CompletableFuture.completedFuture(resultFor(batch));
    };

    Map<String, IndexedAIP> resolved = AncestorBatchResolver.resolve(ids(1100), finder).join();

    Assert.assertEquals(resolved.size(), 1100);
    Assert.assertEquals(resolved.get("id-1099").getTitle(), "Titel id-1099");
    Assert.assertFalse(requestSizes.isEmpty());
    for (int size : requestSizes) {
      Assert.assertTrue(size <= AncestorBatchResolver.MAX_IDS_PER_REQUEST,
        "batch med " + size + " ID:n överskrider gränsen");
    }
    int total = requestSizes.stream().mapToInt(Integer::intValue).sum();
    Assert.assertEquals(total, 1100, "alla ID:n ska skickas exakt en gång");
  }

  @Test
  public void resolveWithNoIdsCompletesWithEmptyMapWithoutCallingFinder() {
    List<Integer> calls = new ArrayList<>();
    Function<List<String>, CompletableFuture<IndexResult<IndexedAIP>>> finder = batch -> {
      calls.add(batch.size());
      return CompletableFuture.completedFuture(resultFor(batch));
    };

    Map<String, IndexedAIP> resolved = AncestorBatchResolver.resolve(Collections.emptyList(), finder).join();

    Assert.assertTrue(resolved.isEmpty());
    Assert.assertTrue(calls.isEmpty());
  }

  @Test
  public void resolveFailsWhenAnyBatchFails() {
    int[] call = {0};
    Function<List<String>, CompletableFuture<IndexResult<IndexedAIP>>> finder = batch -> {
      call[0]++;
      if (call[0] == 2) {
        CompletableFuture<IndexResult<IndexedAIP>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("too many boolean clauses"));
        return failed;
      }
      return CompletableFuture.completedFuture(resultFor(batch));
    };

    CompletableFuture<Map<String, IndexedAIP>> future = AncestorBatchResolver.resolve(ids(1100), finder);

    Assert.assertTrue(future.isCompletedExceptionally(), "ett misslyckat batchanrop ska fälla hela uppslagningen");
    try {
      future.join();
      Assert.fail("join() ska kasta");
    } catch (CompletionException e) {
      Assert.assertTrue(e.getCause().getMessage().contains("too many boolean clauses"));
    }
  }
}
