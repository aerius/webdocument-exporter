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
import com.intuit.karate.Suite;
import com.intuit.karate.core.Config;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureCall;
import com.intuit.karate.core.FeatureRuntime;
import com.intuit.karate.core.FeatureSection;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.driver.DevToolsDriver;
import com.intuit.karate.driver.DriverOptions;
import com.intuit.karate.http.HttpClientFactory;
import com.intuit.karate.http.Response;
import com.intuit.karate.shell.Command;

public class QuittableChrome extends DevToolsDriver {
  private static final Logger LOG = LoggerFactory.getLogger(QuittableChrome.class);

  private final String id;

  public QuittableChrome(final Response res, final DriverOptions options, final Command command, final String webSocketUrl) {
    super(options, command, webSocketUrl);

    // Fetch the page ID
    id = res.json().get("$.id");
    LOG.info("Page ID created: {}", id);

    activate();
    enablePageEvents();
    enableRuntimeEvents();
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
    final ScenarioRuntime runtime = createRuntime();
    final DriverOptions options = new DriverOptions(props, runtime, 9222, null);
    options.arg("--remote-debugging-port=" + options.port);
    options.arg("--no-first-run");
    options.arg("--user-data-dir=" + options.workingDir.getAbsolutePath());
    options.arg("--disable-popup-blocking");

    if (options.headless) {
      options.arg("--headless");
    }
    // Create a page
    final Http http = options.getHttp();
    Command.waitForHttp(http.urlBase);
    final Response res = http.path("json", "new").put(null);

    final String webSocketUrl = res.json().get("$.webSocketDebuggerUrl");
    return new QuittableChrome(res, options, null, webSocketUrl);
  }

  private static synchronized ScenarioRuntime createRuntime() {
    if (ScenarioEngine.get() == null) {
      // Use the bare minimum to construct a runtime/engine.
      final Feature dummyFeature = Feature.read("classpath:/nl/aerius/print/dummy.feature");
      final FeatureCall dummyFeatureCall = new FeatureCall(dummyFeature);
      final FeatureRuntime featureRuntime = FeatureRuntime.of(Suite.forTempUse(HttpClientFactory.DEFAULT), dummyFeatureCall, null);
      final FeatureSection section = new FeatureSection();
      section.setIndex(-1);
      final Scenario dummyScenario = new Scenario(dummyFeature, section, -1);
      section.setScenario(dummyScenario);
      final ScenarioRuntime runtime = new ScenarioRuntime(featureRuntime, dummyScenario);
      final ScenarioEngine engine = new ScenarioEngine(new Config(), runtime, new HashMap<>(), new com.intuit.karate.Logger(QuittableChrome.class));
      ScenarioEngine.set(engine);
    }
    return ScenarioEngine.get().runtime;
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
