package nl.aerius;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;

public class PdfProcessingHandle {
  private static final Logger LOG = LoggerFactory.getLogger(PdfProcessingHandle.class);

  private static final String FONT = "NotoSansTC-Regular.otf";

  private static final float MARGIN_HOR = 56F;
  private static final float FOOTER_TITLE_VER = 28F;
  private static final float FOOTER_SUBTITLE_VER = 18F;

  private boolean finalized;

  private String font = FONT;

  private String source;
  private String target;

  private final List<Consumer<Document>> documentProcessors = new ArrayList<>();
  private final List<TriConsumer<Document, PdfPage, Integer>> pageProcessors = new ArrayList<>();

  private PdfFont pdfFont;

  public void process() {
    finalized = true;

    try (final InputStream is = PdfProcessingHandle.class.getClassLoader().getResourceAsStream(font)) {
      final byte[] fontBytes = IOUtils.toByteArray(is);

      pdfFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, true);
    } catch (final IOException e) {
      LOG.info("Could not fetch font while processing PDF.", e);
      throw new RuntimeException(e);
    }

    final PdfDocument pdfDoc;
    try {
      pdfDoc = new PdfDocument(new PdfReader(source), new PdfWriter(target));
    } catch (final IOException e) {
      LOG.info("Could not fetch PDF to mutate: {}", source, e);
      throw new RuntimeException(e);
    }

    final Document document = new Document(pdfDoc);
    final int numberOfPages = pdfDoc.getNumberOfPages();

    for (int i = 1; i <= numberOfPages; i++) {
      final int number = i;
      final PdfPage page = pdfDoc.getPage(i);
      pageProcessors.forEach(v -> v.accept(document, page, number));
    }

    documentProcessors.forEach(v -> v.accept(document));

    document.close();
  }

  public static PdfProcessingHandle create() {
    return new PdfProcessingHandle();
  }

  public PdfProcessingHandle metaData(final Map<String, String> meta) {
    return documentProcessor(document -> {
      final PdfDocumentInfo info = document.getPdfDocument().getDocumentInfo();
      info.setMoreInfo(meta);
    });
  }

  public PdfProcessingHandle frontPage(final String source) {
    return documentProcessor(document -> {
      try (PdfReader reader = new PdfReader(source)) {
        final PdfDocument titlePdf = new PdfDocument(reader);
        titlePdf.copyPagesTo(1, 1, document.getPdfDocument(), 1);
        titlePdf.close();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public PdfProcessingHandle documentSubtitle(final String subtitle) {
    return pageProcessor((document, page, number) -> {
      final Paragraph documentNameSubtitle = new Paragraph(subtitle)
          .setFont(pdfFont)
          .setFontColor(new DeviceRgb(225, 119, 49))
          .setFontSize(8F);
      document.showTextAligned(documentNameSubtitle,
          MARGIN_HOR, FOOTER_SUBTITLE_VER,
          number, TextAlignment.LEFT, VerticalAlignment.BOTTOM, 0);
    });
  }

  public PdfProcessingHandle documentTitle(final String title) {
    return pageProcessor((document, page, number) -> {
      final Paragraph documentName = new Paragraph(title)
          .setFont(pdfFont)
          .setFontColor(new DeviceRgb(225, 119, 49))
          .setFontSize(14F);
      document.showTextAligned(documentName,
          MARGIN_HOR, FOOTER_TITLE_VER,
          number, TextAlignment.LEFT, VerticalAlignment.BOTTOM, 0);
    });
  }

  public PdfProcessingHandle pageNumbers() {
    return pageProcessor((document, page, number) -> {
      final Rectangle pageSize = page.getPageSize();

      final Paragraph pageNumber = new Paragraph(String.format("%d/%d", number + 1, document.getPdfDocument().getNumberOfPages() + 1))
          .setFont(pdfFont)
          .setFontSize(8F);

      document.showTextAligned(pageNumber,
          pageSize.getWidth() - MARGIN_HOR, FOOTER_SUBTITLE_VER,
          number, TextAlignment.RIGHT, VerticalAlignment.BOTTOM, 0);
    });
  }

  public static PdfProcessingHandle create(final String source, final String target) {
    return PdfProcessingHandle.create()
        .source(source)
        .target(target);
  }

  public PdfProcessingHandle source(final String source) {
    checkFinalized();
    this.source = source;
    return this;
  }

  public PdfProcessingHandle target(final String target) {
    checkFinalized();
    this.target = target;
    return this;
  }

  public PdfProcessingHandle font(final String font) {
    checkFinalized();
    this.font = font;
    return this;
  }

  public PdfProcessingHandle documentProcessor(final Consumer<Document> consumer) {
    checkFinalized();
    documentProcessors.add(consumer);
    return this;
  }

  public PdfProcessingHandle pageProcessor(final TriConsumer<Document, PdfPage, Integer> consumer) {
    checkFinalized();
    pageProcessors.add(consumer);
    return this;
  }

  private void checkFinalized() {
    if (finalized) {
      throw new IllegalStateException("Cannot mutate on finalized document.");
    }
  }
}
