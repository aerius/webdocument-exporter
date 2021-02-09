package nl.aerius.print;

import nl.aerius.pdf.PdfProcessingHandle;

public class PrintJob extends OutputJob {
  public PrintJob(final ExportJob job) {
    super(job);
  }

  public PdfProcessingHandle toProcessor() {
    // The processor assumes the document is saved to disk
    if (!job.saved()) {
      job.save();
    }

    if (job.outputDocument() == null) {
      throw new IllegalStateException("Cannot move to processor without first printing a document.");
    }

    return PdfProcessingHandle.create(job.outputDocument());
  }
}
