package nl.aerius.print;

import nl.aerius.pdf.PdfProcessingHandle;

public class PrintJob {
  private final ExportJob job;

  public PrintJob(final ExportJob job) {
    this.job = job;
  }

  public PdfProcessingHandle toProcessor() {
    if (job.outputDocument() == null) {
      throw new IllegalStateException("Cannot move to processor without first printing a document.");
    }

    return PdfProcessingHandle.create(job.outputDocument());
  }

  public String outputDocument() {
    return job.outputDocument();
  }
}
