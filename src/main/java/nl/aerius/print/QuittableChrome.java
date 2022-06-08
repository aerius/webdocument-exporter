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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuit.karate.Http;
import com.intuit.karate.Http.Response;
import com.intuit.karate.driver.DevToolsDriver;
import com.intuit.karate.driver.DriverOptions;
import com.intuit.karate.shell.Command;

public class QuittableChrome extends DevToolsDriver {
  private static final Logger LOG = LoggerFactory.getLogger(QuittableChrome.class);

  private final String id;

  public QuittableChrome(final Response res, final DriverOptions options, final Command command, final String webSocketUrl) {
    super(options, command, webSocketUrl);

    // Fetch the page ID
    id = res.jsonPath("$.id").asString();
    LOG.info("Page ID created: {}", id);

    activate();
    enablePageEvents();
    enableRuntimeEvents();
    enableTargetEvents();
    if (!options.headless) {
      initWindowIdAndState();
    }
  }

  public static QuittableChrome prepareAndStart() {
    return prepareAndStart(null);
  }

  public static QuittableChrome prepareAndStart(final Map<String, Object> map) {
    final Map<String, Object> props = new HashMap<>();
    if (map != null) {
      props.putAll(map);
    }

    final DriverOptions options = new DriverOptions(null, props, null, 9222, null);
    options.arg("--remote-debugging-port=" + options.port);
    options.arg("--no-first-run");
    options.arg("--user-data-dir=" + options.workingDirPath);
    options.arg("--disable-popup-blocking");

    if (options.headless) {
      options.arg("--headless");
    }
    // Create a page
    final Http http = options.getHttp();
    Command.waitForHttp(http.urlBase);
    final Http.Response res = http.path("json", "new").get();

    final String webSocketUrl = res.jsonPath("$.webSocketDebuggerUrl").asString();
    return new QuittableChrome(res, options, null, webSocketUrl);
  }

  @Override
  public void quit() {
    try {
      LOG.info("Closing page ID: {}", id);
      final Http http = options.getHttp();
      Command.waitForHttp(http.urlBase);
      http.path("json", "close", id).get();
    } finally {
      super.quit();
    }
  }
}
