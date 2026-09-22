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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Integration test that verifies request routing against a real Chrome instance.
 * Skips automatically when Chrome or the test server are not available.
 *
 * <p>To run, start in order:
 * <ol>
 *   <li>{@code node test-server.js}</li>
 *   <li>{@code google-chrome --headless --remote-debugging-port=9222 --no-first-run --disable-gpu --remote-allow-origins=*}</li>
 *   <li>{@code mvn test -Dtest=RequestRoutingTest}</li>
 * </ol>
 *
 * <p>The page at /routed posts to its own /api/echo, which the test server on port 3456 does not serve.
 * The route sends it to the echo server on port 3457, and the page writes the answer and the URL it saw into its title.
 */
class RequestRoutingTest {

  @Test
  void requestIsRoutedWithoutThePageSeeingIt() throws InterruptedException {
    assumeTrue(isPortOpen(9222), "Chrome not running on port 9222");
    assumeTrue(isPortOpen(3456), "Test server not running on port 3456");
    assumeTrue(isPortOpen(3457), "Echo server not running on port 3457");

    final RequestRoutes routes = new RequestRoutes();
    routes.add("http://localhost:3456/api/", "http://localhost:3457/api/");
    final QuittableChrome chrome = QuittableChrome.prepareAndStart(Map.of("start", false, "headless", true), false, routes);
    try {
      chrome.setUrl("http://localhost:3456/routed");
      chrome.waitUntil("document.title.startsWith('done')");

      assertEquals("done POST /api/echo body=payload seen=http://localhost:3456/api/echo", chrome.script("document.title"));
    } finally {
      chrome.quit();
    }
  }

  private static boolean isPortOpen(final int port) {
    try (Socket socket = new Socket("localhost", port)) {
      return true;
    } catch (final IOException e) {
      return false;
    }
  }
}
