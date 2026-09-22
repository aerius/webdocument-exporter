/*
 * Copyright (c) Contributors to the project
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * URL prefixes whose requests Chrome sends to another URL prefix, without the page noticing.
 */
public class RequestRoutes {
  private final Map<String, String> routes = new LinkedHashMap<>();

  /**
   * Send every request whose URL starts with {@code fromPrefix} to {@code toPrefix} plus the rest of the URL.
   */
  public void add(final String fromPrefix, final String toPrefix) {
    if (fromPrefix.contains("*") || fromPrefix.contains("?") || fromPrefix.contains("\\")) {
      throw new IllegalArgumentException("A route prefix cannot contain '*', '?' or '\\': " + fromPrefix);
    }
    routes.put(fromPrefix, toPrefix);
  }

  public boolean isEmpty() {
    return routes.isEmpty();
  }

  /**
   * The request patterns to pause in Chrome, as the Fetch.enable command takes them.
   */
  public List<Map<String, Object>> patterns() {
    return routes.keySet().stream()
        .map(prefix -> Map.<String, Object>of("urlPattern", prefix + "*"))
        .toList();
  }

  public Optional<String> rewrite(final String url) {
    return routes.entrySet().stream()
        .filter(route -> url.startsWith(route.getKey()))
        .findFirst()
        .map(route -> route.getValue() + url.substring(route.getKey().length()));
  }
}
