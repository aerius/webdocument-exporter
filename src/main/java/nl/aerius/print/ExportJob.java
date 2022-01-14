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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuit.karate.Config;
import com.intuit.karate.FileUtils;
import com.intuit.karate.driver.DevToolsDriver;
import com.intuit.karate.driver.MissingElement;

public class ExportJob {
  private static final Logger LOG = LoggerFactory.getLogger(ExportJob.class);

  private static final String TMP = "/tmp/";

  private String host;
  private String url;
  private String handle;
  private String destination = TMP;
  private int retryCount = Config.DEFAULT_RETRY_COUNT;

  private boolean exported;

  private byte[] exportResult;
  private String outputDocument;

  private String name;

  private Consumer<DevToolsDriver> waitForComplete;

  private boolean saved;

  public static ExportJob create() {
    return new ExportJob();
  }

  public static ExportJob create(final String url) {
    return new ExportJob()
        .url(url);
  }

  public static ExportJob create(final String url, final String handle) {
    return new ExportJob()
        .url(url)
        .handle(handle);
  }

  public ExportJob url(final String url) {
    checkExported();
    this.url = url;
    return this;
  }

  public ExportJob waitForComplete(final Consumer<DevToolsDriver> runner) {
    checkExported();
    this.waitForComplete = runner;
    return this;
  }

  public ExportJob handle(final String handle) {
    checkExported();
    this.handle = handle;
    return this;
  }

  public ExportJob destination(final String destination) {
    checkExported();
    this.destination = destination;
    return this;
  }

  public ExportJob chromeHost(final String host) {
    checkExported();
    this.host = host;
    return this;
  }

  public ExportJob retry(final int retryCount) {
    checkExported();
    this.retryCount = retryCount;
    return this;
  }

  public SnapshotJob snapshot() {
    final Map<String, Object> dimensions = new HashMap<>();
    dimensions.put("x", 0);
    dimensions.put("y", 0);
    dimensions.put("width", 1920);
    dimensions.put("height", 1080);
    return snapshot(dimensions);
  }

  public SnapshotJob snapshot(final Map<String, Object> dimensions) {
    checkExported();
    ensureHandle();
    exported = true;

    LOG.info("Exporting graphic from: " + url);

    final DevToolsDriver chrome = fetchChrome();
    try {
      chrome.setUrl(url);

      // Set a default wait
      if (waitForComplete == null) {
        completeViaSleep();
      }
      waitForComplete(chrome);

      name = handle + ".png";
      exportResult = chrome.screenshot();
    } catch (final Exception e) {
      LOG.error("Unrecoverable failure while sending snapshot job to chrome instance.", e);
      throw new RuntimeException("Could not finish web document snapshot.", e);
    } finally {
      chrome.quit();
    }

    return new SnapshotJob(this);
  }

  public PrintJob print() {
    final HashMap<String, Object> printParams = new HashMap<>();
    printParams.put("printBackground", true);
    return print(printParams);
  }

  public PrintJob print(final Map<String, Object> printParams) {
    checkExported();
    ensureHandle();
    exported = true;

    final DevToolsDriver chrome = fetchChrome();
    try {
      chrome.setUrl(url);

      if (waitForComplete == null) {
        completeViaIndicator();
      }
      waitForComplete(chrome);

      name = handle + ".pdf";
      exportResult = chrome.pdf(printParams);
    } catch (final Exception e) {
      LOG.error("Unrecoverable failure while sending print job to chrome instance.", e);
      throw new RuntimeException("Could not finish PDF export.", e);
    } finally {
      chrome.quit();
    }

    return new PrintJob(this);
  }

  private void ensureHandle() {
    // Set the handle if none is set
    if (handle == null) {
      handle(UUID.randomUUID().toString());
    }
  }

  private void checkExported() {
    if (exported) {
      throw new IllegalStateException("Cannot mutate an already-exported job.");
    }
  }

  public ExportJob completeOrFailViaIndicator() {
    return waitForComplete(chrome -> {
      chrome.waitForAny("#complete-indicator", "#failure-indicator");
      if (!(chrome.exists("#failure-indicator") instanceof MissingElement)) {
        // hard crash (for now)
        throw new RuntimeException();
      }
    });
  }

  public ExportJob completeViaIndicator() {
    return waitForComplete(chrome -> chrome.waitFor("#complete-indicator"));
  }

  public ExportJob completeViaSleep() {
    return waitForComplete(chrome -> {
      try {
        Thread.sleep(4000);
      } catch (final InterruptedException e) {
        // Eat
      }
    });
  }

  /**
   * TODO Provide graceful failure functionality
   */
  private void waitForComplete(final DevToolsDriver driver) {
    waitForComplete.accept(driver);
  }

  /**
   * Save the export output to disk (once)
   */
  public ExportJob save() {
    if (saved) {
      return this;
    }

    saved = true;
    outputDocument = destination + name;
    LOG.info("Writing file to: {}", outputDocument);
    FileUtils.writeToFile(new File(outputDocument), exportResult);
    return this;
  }

  public byte[] result() {
    return exportResult;
  }

  private DevToolsDriver fetchChrome() {
    final Map<String, Object> options = new HashMap<>();
    options.put("start", false);
    options.put("headless", true);
    options.put("host", host);

    final QuittableChrome chrome = QuittableChrome.prepareAndStart(options);
    chrome.retry(retryCount);

    return chrome;
  }

  public String outputDocument() {
    return outputDocument;
  }

  public boolean saved() {
    return saved;
  }
}
