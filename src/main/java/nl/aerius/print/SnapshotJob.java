package nl.aerius.print;

public class SnapshotJob {
  private final ExportJob job;

  public SnapshotJob(final ExportJob job) {
    this.job = job;
  }

  public String outputDocument() {
    return job.outputDocument();
  }

  public byte[] result() {
    return job.result();
  }
}
