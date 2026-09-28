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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RequestRoutesTest {

  @Test
  void rewritesTheMatchingPrefix() {
    final RequestRoutes routes = new RequestRoutes();
    routes.add("http://web:8080/api/", "http://api:8080/api/");
    routes.add("http://web:8080/geoserver/", "http://geo:8080/geoserver/");

    assertEquals(Optional.of("http://api:8080/api/v8/ui/context?x=1"), routes.rewrite("http://web:8080/api/v8/ui/context?x=1"));
    assertEquals(Optional.of("http://geo:8080/geoserver/wms"), routes.rewrite("http://web:8080/geoserver/wms"));
    assertEquals(Optional.empty(), routes.rewrite("http://web:8080/print"));
  }

  @Test
  void patternsPauseEveryUrlUnderThePrefix() {
    final RequestRoutes routes = new RequestRoutes();
    routes.add("http://web:8080/api/", "http://api:8080/api/");

    assertEquals(List.of(Map.of("urlPattern", "http://web:8080/api/*")), routes.patterns());
  }

  @Test
  void rejectsPatternCharactersInThePrefix() {
    final RequestRoutes routes = new RequestRoutes();

    assertThrows(IllegalArgumentException.class, () -> routes.add("http://web:8080/api?", "http://api:8080/api/"));
  }
}
