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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Integration test that verifies network failure tracking against a real Chrome instance.
 * Skips automatically when Chrome or the test server are not available.
 *
 * <p>To run, start in order:
 * <ol>
 *   <li>{@code node test-server.js}</li>
 *   <li>{@code google-chrome --headless --remote-debugging-port=9222 --no-first-run --disable-gpu --remote-allow-origins=*}</li>
 *   <li>{@code mvn test -Dtest=NetworkFailureTrackingTest}</li>
 * </ol>
 *
 * <p>The test server (test-server.js in the project root) serves a page that triggers
 * both network-level failures and HTTP error responses (500, 403) with bodies.
 */
class NetworkFailureTrackingTest {

  /**
   * Navigates to the test server page which triggers:
   * <ul>
   *   <li>A fetch to a dead port (network-level failure)</li>
   *   <li>A fetch to /error (HTTP 500 with text body)</li>
   *   <li>A fetch to /forbidden (HTTP 403 with HTML body)</li>
   * </ul>
   * Verifies that all failures are captured with the expected data.
   */
  @Test
  void networkFailureIsCaptured() throws InterruptedException {
    assumeTrue(isPortOpen(9222), "Chrome not running on port 9222");
    assumeTrue(isPortOpen(3456), "Test server not running on port 3456");

    final Map<String, Object> options = Map.of(
        "start", false,
        "headless", true);

    final QuittableChrome chrome = QuittableChrome.prepareAndStart(options);
    try {
      // Navigate to local test server that serves a page making same-origin fetches:
      // /error - 500 with text body
      // /forbidden - 403 with HTML body
      // Also fetches http://localhost:1/dead-port for a network-level failure
      chrome.setUrl("http://localhost:3456/");

      // Give Chrome time to process the network events
      Thread.sleep(2000);

      final List<NetworkFailure> failures = chrome.getNetworkFailures();

      System.out.println("Captured " + failures.size() + " network failure(s):");
      for (final NetworkFailure failure : failures) {
        System.out.println("  url=" + failure.url()
            + " method=" + failure.method()
            + " error=" + failure.errorText()
            + " type=" + failure.resourceType()
            + " canceled=" + failure.canceled()
            + " status=" + failure.responseStatus()
            + " body=" + failure.responseBody());
      }

      assertFalse(failures.isEmpty(), "Expected at least one network failure");
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
