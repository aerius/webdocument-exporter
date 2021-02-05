package nl.aerius.print;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuit.karate.FileUtils;
import com.intuit.karate.driver.DevToolsDriver;

public class ExportJob {
  private static final Logger LOG = LoggerFactory.getLogger(ExportJob.class);

  private static final String TMP = "/tmp/";

  private String host;
  private String url;
  private String handle;
  private String destination = TMP;

  private boolean exported;

  private String outputDocument;

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

  public SnapshotJob snapshot() {
    checkExported();
    ensureHandle();
    exported = true;

    final DevToolsDriver chrome = fetchChrome();
    try {
      chrome.setUrl(url);
      chrome.waitFor("#complete-indicator");

      final String name = handle + ".png";
      final byte[] libraryBytes = chrome.screenshot();

      outputDocument = destination + name;
      LOG.info("Writing file to: {}", outputDocument);
      FileUtils.writeToFile(new File(outputDocument), libraryBytes);
    } catch (final Exception e) {
      LOG.error("Unrecoverable failure while sending snapshot job to chrome instance.", e);
      throw new RuntimeException("Could not finish web document snapshot.", e);
    } finally {
      chrome.quit();
    }

    return new SnapshotJob(this);
  }

  public PrintJob print() {
    checkExported();
    ensureHandle();
    exported = true;

    final DevToolsDriver chrome = fetchChrome();
    try {
      chrome.setUrl(url);
      chrome.waitFor("#complete-indicator");

      final String name = handle + "_CONTENT.pdf";

      final HashMap<String, Object> printParams = new HashMap<>();
      printParams.put("printBackground", true);

      final byte[] libraryBytes = chrome.pdf(printParams);

      outputDocument = destination + name;
      LOG.info("Writing file to: {}", outputDocument);
      FileUtils.writeToFile(new File(outputDocument), libraryBytes);
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

  private DevToolsDriver fetchChrome() {
    final Map<String, Object> options = new HashMap<>();
    options.put("start", false);
    options.put("headless", true);
    options.put("host", host);

    final ChromeAttachment chrome = ChromeAttachment.start(options);

    return chrome;
  }

  public String outputDocument() {
    return outputDocument;
  }
}
