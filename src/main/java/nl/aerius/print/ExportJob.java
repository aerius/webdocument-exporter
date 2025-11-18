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
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuit.karate.FileUtils;
import com.intuit.karate.core.Config;
import com.intuit.karate.driver.DevToolsDriver;

import nl.aerius.pdf.FailureIndicatorException;

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
  private Map<String, Object> driverOptions = new HashMap<>();

  private boolean saved;

  private DriverHook completeHook;
  private DriverHook failureHook;

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

    LOG.info("Exporting graphic from: {}", url);

    name = handle + ".png";
    exportResult = runExport(true, d -> d.screenshot(), "snapshot");

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

    name = handle + ".pdf";
    exportResult = runExport(false, d -> d.pdf(printParams), "print");

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
      chrome.waitFor("#complete-indicator");
      if (chrome.exists("#complete-indicator.failure")) {
        throw new FailureIndicatorException();
      }
    });
  }

  @Deprecated
  public ExportJob completeViaIndicator() {
    return completeOrFailViaIndicator();
  }

  public ExportJob completeViaSleep() {
    return waitForComplete(chrome -> {
      try {
        Thread.sleep(4000);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        // Eat
      }
    });
  }

  /**
   * Add more driver options, see {@link com.intuit.karate.driver.DriverOptions} for available options.
   * @param driverOptions additional driver options
   */
  public ExportJob driverOptions(final Map<String, Object> driverOptions) {
    this.driverOptions = driverOptions;
    return this;
  }

  public ExportJob completeHook(final DriverHook hook) {
    checkExported();
    this.completeHook = hook;
    return this;
  }

  public ExportJob failureHook(final DriverHook hook) {
    checkExported();
    this.failureHook = hook;
    return this;
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
    final Map<String, Object> options = new HashMap<>(driverOptions);
    options.put("start", false);
    options.put("headless", true);
    options.put("host", host);

    final QuittableChrome chrome = QuittableChrome.prepareAndStart(options);
    chrome.retry(retryCount);

    return chrome;
  }

  private byte[] runExport(final boolean useSleepWait, final Function<DevToolsDriver, byte[]> exporter,
      final String failurePhase) {
    final DevToolsDriver chrome = fetchChrome();
    try {
      chrome.setUrl(url);

      if (waitForComplete == null) {
        if (useSleepWait) {
          completeViaSleep();
        } else {
          completeViaIndicator();
        }
      }
      waitForComplete(chrome);

      if (completeHook != null) {
        try {
          completeHook.accept(chrome, url, "complete", null);
        } catch (final RuntimeException ignored) {
          LOG.warn("Failure during completeHook execution, ignoring.", ignored);
        }
      }

      return exporter.apply(chrome);
    } catch (final RuntimeException e) {
      if (failureHook != null) {
        try {
          failureHook.accept(chrome, url, failurePhase, e);
        } catch (final RuntimeException ignored) {
          LOG.warn("Failure during failureHook execution, ignoring.", ignored);
        }
      }
      LOG.error("Unrecoverable failure while executing export.", e);
      throw e;
    } finally {
      chrome.quit();
    }
  }

  public String outputDocument() {
    return outputDocument;
  }

  public boolean saved() {
    return saved;
  }
}
