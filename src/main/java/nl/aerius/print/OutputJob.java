package nl.aerius.print;

public class OutputJob {
  protected final ExportJob job;

  public OutputJob(final ExportJob job) {
    this.job = job;
  }

  public String outputDocument() {
    return job.outputDocument();
  }

  public ExportJob save() {
    return job.save();
  }

  public byte[] result() {
    return job.result();
  }
}
