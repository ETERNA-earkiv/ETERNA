/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.browse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.roda.core.data.v2.index.IndexResult;
import org.roda.core.data.v2.ip.IndexedAIP;

/**
 * Slår upp AIP:er per ID i batchar. Servern översätter en ID-lista till
 * {@code (uuid:a OR uuid:b OR ...)} och Solr avvisar frågor med fler än 1024
 * OR-led (maxBooleanClauses). Ett enda stort anrop för alla akter under en
 * serie sprack därför, och katalogträdet tolkade felet som saknad behörighet
 * (#643). Här delas listan upp så att varje anrop håller sig under gränsen.
 */
public final class AncestorBatchResolver {

  /** Marginal under Solrs standardgräns på 1024 boolean clauses. */
  public static final int MAX_IDS_PER_REQUEST = 500;

  private AncestorBatchResolver() {
    // hjälpklass
  }

  public static List<List<String>> partition(Collection<String> ids, int batchSize) {
    List<List<String>> batches = new ArrayList<>();
    List<String> current = new ArrayList<>();
    for (String id : ids) {
      current.add(id);
      if (current.size() == batchSize) {
        batches.add(current);
        current = new ArrayList<>();
      }
    }
    if (!current.isEmpty()) {
      batches.add(current);
    }
    return batches;
  }

  /**
   * Hämtar alla ID:n via {@code finder}, ett batchanrop per delmängd, och slår
   * ihop resultaten till en karta ID → AIP. Misslyckas något batchanrop
   * misslyckas hela uppslagningen, så att anroparen kan visa ett fel i stället
   * för att felaktigt rita noderna som otillgängliga.
   */
  public static CompletableFuture<Map<String, IndexedAIP>> resolve(Collection<String> ids,
    Function<List<String>, CompletableFuture<IndexResult<IndexedAIP>>> finder) {
    List<List<String>> batches = partition(ids, MAX_IDS_PER_REQUEST);
    if (batches.isEmpty()) {
      return CompletableFuture.completedFuture(new HashMap<>());
    }

    List<CompletableFuture<IndexResult<IndexedAIP>>> futures = new ArrayList<>();
    for (List<String> batch : batches) {
      futures.add(finder.apply(batch));
    }

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).thenApply(v -> {
      Map<String, IndexedAIP> resolved = new HashMap<>();
      for (CompletableFuture<IndexResult<IndexedAIP>> future : futures) {
        for (IndexedAIP aip : future.join().getResults()) {
          resolved.put(aip.getId(), aip);
        }
      }
      return resolved;
    });
  }
}
