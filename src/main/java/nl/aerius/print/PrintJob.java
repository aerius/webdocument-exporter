package nl.aerius.print;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuit.karate.FileUtils;
import com.intuit.karate.driver.DevToolsDriver;

import nl.aerius.pdf.PdfProcessingHandle;

public class PrintJob {
  private static final Logger LOG = LoggerFactory.getLogger(PrintJob.class);

  private static final String TMP = "/tmp/";

  private String host;
  private ExportHandle handle;
  private String destination = TMP;

  private boolean printed;

  private String outputDocument;

  public static PrintJob create(final ExportHandle handle) {
    return new PrintJob()
        .handle(handle);
  }

  public PrintJob handle(final ExportHandle handle) {
    checkPrinted();
    this.handle = handle;
    return this;
  }

  public PrintJob destination(final String destination) {
    checkPrinted();
    this.destination = destination;
    return this;
  }

  public PrintJob chrome(final String host) {
    checkPrinted();
    this.host = host;
    return this;
  }

  public PrintJob print() {
    checkPrinted();
    printed = true;

    final DevToolsDriver chrome = fetchChrome();
    try {
      chrome.setUrl(handle.getUrl());
      chrome.waitFor("#complete-indicator");

      final String name = handle.getPrintCode() + "_CONTENT.pdf";

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

    return this;
  }

  public PdfProcessingHandle toProcessor() {
    if (outputDocument == null) {
      throw new IllegalStateException("Cannot move to processor without first printing a document.");
    }

    return PdfProcessingHandle.create(outputDocument);
  }

  private void checkPrinted() {
    if (printed) {
      throw new IllegalStateException("Cannot mutate an already-printed job.");
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
}
