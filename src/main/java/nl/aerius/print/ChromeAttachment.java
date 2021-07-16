/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nl.aerius.print;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuit.karate.Http;
import com.intuit.karate.LogAppender;
import com.intuit.karate.core.ScenarioContext;
import com.intuit.karate.driver.DevToolsDriver;
import com.intuit.karate.driver.DriverOptions;
import com.intuit.karate.shell.Command;

/**
 *
 * chrome devtools protocol - the "preferred" driver:
 * https://chromedevtools.github.io/devtools-protocol/
 *
 * @author pthomas3
 * This class is derived from {@link com.intuit.karate.driver.chrome.Chrome} class.
 */
public class ChromeAttachment extends DevToolsDriver {
  private static final Logger LOG = LoggerFactory.getLogger(ChromeAttachment.class);

  private static String id;

  public ChromeAttachment(final DriverOptions options, final Command command, final String webSocketUrl) {
    super(options, command, webSocketUrl);
  }

  public static ChromeAttachment start(final ScenarioContext context, final Map<String, Object> map, final LogAppender appender) {
    final DriverOptions options = new DriverOptions(context, map, appender, 9222, null);
    options.arg("--remote-debugging-port=" + options.port);
    options.arg("--no-first-run");
    options.arg("--user-data-dir=" + options.workingDirPath);
    options.arg("--disable-popup-blocking");
    if (options.headless) {
      options.arg("--headless");
    }
    final Http http = options.getHttp();
    Command.waitForHttp(http.urlBase);
    final Http.Response res = http.path("json", "new").get();

    id = res.jsonPath("$.id").asString();

    LOG.info("New page ID created: ", id);

    final String webSocketUrl = res.jsonPath("$.webSocketDebuggerUrl").asString();
    final ChromeAttachment chrome = new ChromeAttachment(options, null, webSocketUrl);
    chrome.activate();
    chrome.enablePageEvents();
    chrome.enableRuntimeEvents();
    chrome.enableTargetEvents();
    if (!options.headless) {
      chrome.initWindowIdAndState();
    }
    return chrome;
  }

  @Override
  public void quit() {
    LOG.info("Closing page ID " + id);

    final Http http = options.getHttp();
    Command.waitForHttp(http.urlBase);
    http.path("json", "close", id).get();
  }

  public static ChromeAttachment start(final String chromeExecutablePath, final boolean headless) {
    final Map<String, Object> options = new HashMap<>();
    options.put("executable", chromeExecutablePath);
    options.put("headless", headless);
    return ChromeAttachment.start(null, options, null);
  }

  public static ChromeAttachment start(Map<String, Object> options) {
    if (options == null) {
      options = new HashMap<>();
    }
    return ChromeAttachment.start(null, options, null);
  }

  public static ChromeAttachment start() {
    return start(null);
  }

  public static ChromeAttachment startHeadless() {
    return start(Collections.singletonMap("headless", true));
  }

}
