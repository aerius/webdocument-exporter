package nl.aerius.print;

public class ExportHandle {
  private String printCode;
  private String url;

  public String getUrl() {
    return url;
  }

  public String getPrintCode() {
    return printCode;
  }

  public void setPrintCode(final String printCode) {
    this.printCode = printCode;
  }

  public void setUrl(final String url) {
    this.url = url;
  }
}
