/*
 * Copyright the State of the Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package nl.aerius.print;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkFailureTracker {
  private static final Logger LOG = LoggerFactory.getLogger(NetworkFailureTracker.class);

  private record RequestData(String url, String method, String referer) {}
  private record FailureData(String requestId, String errorText, String resourceType, boolean canceled) {}

  private final Map<String, RequestData> requests = new ConcurrentHashMap<>();
  private final Map<String, Integer> responseStatuses = new ConcurrentHashMap<>();
  private final List<FailureData> failures = new CopyOnWriteArrayList<>();
  private final Set<String> httpErrorRequestIds = ConcurrentHashMap.newKeySet();

  public void onRequest(final String requestId, final String url, final String method, final String referer) {
    if (requestId != null) {
      requests.put(requestId, new RequestData(url, method, referer));
    }
  }

  public void onResponse(final String requestId, final Integer status) {
    if (requestId != null && status != null) {
      responseStatuses.put(requestId, status);
      if (status >= 400) {
        httpErrorRequestIds.add(requestId);
        final RequestData req = requests.get(requestId);
        LOG.warn("HTTP error response: url={} status={}",
            req != null ? req.url() : "unknown", status);
      }
    }
  }

  public void onLoadingFailed(final String requestId, final String errorText, final String resourceType, final Boolean canceled) {
    final boolean isCanceled = canceled != null && canceled;
    failures.add(new FailureData(requestId, errorText, resourceType, isCanceled));
    // Remove from httpErrorRequestIds to avoid duplicate reporting
    if (requestId != null) {
      httpErrorRequestIds.remove(requestId);
    }

    final RequestData req = requestId != null ? requests.get(requestId) : null;
    LOG.warn("Network request failed: url={} error={} type={} canceled={} referer={}",
        req != null ? req.url() : "unknown", errorText, resourceType, isCanceled,
        req != null ? req.referer() : "unknown");
  }

  public List<NetworkFailure> getFailures(final Function<String, String> bodyFetcher) {
    final List<NetworkFailure> result = new ArrayList<>();

    // Network-level failures (loadingFailed)
    for (final FailureData failure : failures) {
      final RequestData req = failure.requestId() != null ? requests.get(failure.requestId()) : null;
      final Integer status = failure.requestId() != null ? responseStatuses.get(failure.requestId()) : null;
      final String body = bodyFetcher.apply(failure.requestId());
      result.add(new NetworkFailure(
          req != null ? req.url() : "unknown",
          req != null ? req.method() : "unknown",
          failure.errorText(),
          failure.resourceType(),
          failure.canceled(),
          status,
          body,
          req != null ? req.referer() : null));
    }

    // HTTP error responses (4xx/5xx) that didn't also trigger loadingFailed
    for (final String requestId : httpErrorRequestIds) {
      final RequestData req = requests.get(requestId);
      final Integer status = responseStatuses.get(requestId);
      final String body = bodyFetcher.apply(requestId);
      result.add(new NetworkFailure(
          req != null ? req.url() : "unknown",
          req != null ? req.method() : "unknown",
          null,
          null,
          false,
          status,
          body,
          req != null ? req.referer() : null));
    }

    return result;
  }
}
