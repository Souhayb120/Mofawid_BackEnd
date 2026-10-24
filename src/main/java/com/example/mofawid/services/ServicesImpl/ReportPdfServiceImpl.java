package com.example.mofawid.services.ServicesImpl;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Document;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.services.ReportPdfService;
import com.example.mofawid.services.StorageService;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Builds the PDF of an approved report (محضر), modeled on the real reports of the office:
// letterhead (french left, scales in the middle, arabic right) + double line,
// title in a box, مرجعنا / بتاريخ, the text right-to-left, the signature, then one framed photo per page.
@Service
public class ReportPdfServiceImpl implements ReportPdfService {

    private static final Logger log = LoggerFactory.getLogger(ReportPdfServiceImpl.class);

    // A4 page, sizes are in points (1 cm = 28.35 points)
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float LEFT = 42;
    private static final float RIGHT = PAGE_WIDTH - 42;
    private static final float TEXT_WIDTH = RIGHT - LEFT;
    private static final float TOP = PAGE_HEIGHT - 40;
    private static final float BOTTOM = 56;

    private static final float BODY_SIZE = 13;
    private static final float LINE_HEIGHT = 1.6f;

    // a PDF attachment can have many pages: we print the first ones only
    private static final int MAX_PDF_PAGES = 10;

    private static final ZoneId MOROCCO = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // "أولا: معاينة مدخل العمارة" ... are centered titles in the real reports
    private static final String[] SECTION_WORDS = {
            "أولا", "ثانيا", "ثالثا", "رابعا", "خامسا", "سادسا", "سابعا", "ثامنا", "تاسعا", "عاشرا"
    };

    private enum Align { RIGHT, CENTER, LEFT, JUSTIFY }

    private final StorageService storageService;

    public ReportPdfServiceImpl(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public byte[] buildPdf(Document document, List<Attachment> attachments) {

        try (PDDocument pdf = new PDDocument()) {

            Writer w = new Writer(
                    pdf,
                    loadFont(pdf, "fonts/Amiri-Regular.ttf"),
                    loadFont(pdf, "fonts/Amiri-Bold.ttf")
            );

            Case caseEntity = document.getCaseEntity();

            w.newPage();
            drawLetterhead(w, caseEntity.getTenant());
            drawTitle(w, titleOf(document.getKind()));
            drawRefAndDate(w, caseEntity.getRef(), document.getApprovedAt());
            drawBody(w, document.getContent());
            drawAnnex(w, attachments);
            w.close();

            drawPageNumbers(pdf, w.regular);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to build the PDF of the report", e);
        }
    }

    // =====================================================================
    // 1. letterhead (رأسية المحضر)
    // =====================================================================

    private void drawLetterhead(Writer w, Tenant tenant) throws IOException {

        float top = w.y;

        // arabic block on the right (lines centered on the same point, like the real ones)
        float arabicCenter = RIGHT - 85;
        textAt(w, valueOr(tenant.getHuissierNameAr(), tenant.getName()), w.bold, 17, arabicCenter, top - 13, Align.CENTER);
        textAt(w, "مفوض قضائي", w.bold, 14, arabicCenter, top - 34, Align.CENTER);
        if (!isBlank(tenant.getAppealCourtAr())) {
            textAt(w, "بدائرة استئناف " + tenant.getAppealCourtAr(), w.bold, 13, arabicCenter, top - 54, Align.CENTER);
        }

        // french block on the left
        float frenchCenter = LEFT + 95;
        if (!isBlank(tenant.getHuissierNameFr())) {
            textAt(w, tenant.getHuissierNameFr().toUpperCase(), w.bold, 13, frenchCenter, top - 13, Align.CENTER);
        }
        textAt(w, "Huissier de justice", w.bold, 11.5f, frenchCenter, top - 29, Align.CENTER);
        if (!isBlank(tenant.getAppealCourtFr())) {
            textAt(w, "Près la Cour d'appel de " + tenant.getAppealCourtFr(), w.bold, 11.5f, frenchCenter, top - 44, Align.CENTER);
        }

        // the scales of justice in the middle
        drawScales(w.out, PAGE_WIDTH / 2, top);

        // address lines
        float center = PAGE_WIDTH / 2;
        if (!isBlank(tenant.getAdress())) {
            textAt(w, tenant.getAdress(), w.bold, 11.5f, center, top - 78, Align.CENTER);
        }

        List<String> contact = new ArrayList<>();
        if (!isBlank(tenant.getAddressFr())) contact.add(tenant.getAddressFr());
        if (!isBlank(tenant.getEmail())) contact.add(tenant.getEmail());
        if (!isBlank(tenant.getPhone())) contact.add("Tel : " + tenant.getPhone());
        if (!contact.isEmpty()) {
            textAt(w, String.join("  /  ", contact), w.bold, 9.5f, center, top - 94, Align.CENTER);
        }

        // thick double line under the letterhead
        w.out.setLineWidth(2.4f);
        line(w.out, LEFT, top - 103, RIGHT, top - 103);
        w.out.setLineWidth(0.8f);
        line(w.out, LEFT, top - 107.5f, RIGHT, top - 107.5f);

        w.y = top - 118;
    }

    // small drawing of the scales (الميزان) like the logo of the real reports
    private void drawScales(PDPageContentStream out, float cx, float top) throws IOException {

        // post + knob
        out.setLineWidth(1.8f);
        line(out, cx, top - 6, cx, top - 52);
        circle(out, cx, top - 4, 2.6f);

        // beam
        out.setLineWidth(1.6f);
        line(out, cx - 32, top - 12, cx + 32, top - 12);

        // the two pans with their strings
        for (int side = -1; side <= 1; side += 2) {
            float px = cx + side * 32;

            out.setLineWidth(0.7f);
            line(out, px, top - 12, px - 11, top - 34);
            line(out, px, top - 12, px + 11, top - 34);

            out.moveTo(px - 13, top - 34);
            out.curveTo(px - 13, top - 44, px + 13, top - 44, px + 13, top - 34);
            out.closePath();
            out.fill();
        }

        // base
        out.addRect(cx - 14, top - 56, 28, 4);
        out.addRect(cx - 21, top - 60, 42, 4);
        out.fill();
    }

    // =====================================================================
    // 2. title in a double box + مرجعنا / بتاريخ
    // =====================================================================

    private void drawTitle(Writer w, String title) throws IOException {

        float size = 18;
        String visual = w.visual(title, w.bold);
        float textWidth = w.width(visual, w.bold, size);

        float boxWidth = textWidth + 70;
        float boxHeight = 30;
        float x = (PAGE_WIDTH - boxWidth) / 2;
        float y = w.y - boxHeight;

        w.out.setLineWidth(1.3f);
        roundedRect(w.out, x, y, boxWidth, boxHeight, 7);
        w.out.setLineWidth(0.7f);
        roundedRect(w.out, x + 3, y + 3, boxWidth - 6, boxHeight - 6, 5);

        w.text(visual, w.bold, size, (PAGE_WIDTH - textWidth) / 2, y + 9);

        w.y = y - 10;
    }

    private void drawRefAndDate(Writer w, String ref, Instant approvedAt) throws IOException {

        float size = 13;
        float baseline = w.y - 14;

        // مرجعنا on the right, the label is underlined like in the real reports
        String label = "مرجعنا:";
        textAt(w, label + " " + valueOr(ref, ""), w.bold, size, RIGHT, baseline, Align.RIGHT);
        float labelWidth = w.width(w.visual(label, w.bold), w.bold, size);
        w.out.setLineWidth(0.8f);
        line(w.out, RIGHT - labelWidth, baseline - 3, RIGHT, baseline - 3);

        // بتاريخ on the left = the day the huissier approved the report
        Instant date = approvedAt == null ? Instant.now() : approvedAt;
        textAt(w, "بتاريخ: " + DATE.format(date.atZone(MOROCCO)), w.bold, size, LEFT, baseline, Align.LEFT);

        w.y = baseline - 22;
    }

    // =====================================================================
    // 3. the text of the report (written by the AI and checked by the huissier)
    // =====================================================================

    private void drawBody(Writer w, String content) throws IOException {

        if (content == null) {
            return;
        }

        boolean firstLine = true;

        for (String raw : content.split("\\r?\\n")) {

            String line = removeMarkdown(raw).trim();

            if (line.isEmpty()) {
                w.y -= BODY_SIZE * 0.5f;
                continue;
            }

            // to compare without the "ـ" (لأجلـــه = لأجله)
            String plain = line.replace("ـ", "");

            // the title and مرجعنا/بتاريخ are already drawn at the top
            if (firstLine && plain.startsWith("محضر") && plain.length() < 40) {
                firstLine = false;
                continue;
            }
            firstLine = false;

            if (plain.startsWith("مرجعنا")) {
                continue;
            }

            if (plain.startsWith("نحن الموقع")) {
                w.y -= 4;
                paragraph(w, line, w.bold, 14, Align.CENTER, 0);
                w.y -= 4;

            } else if (plain.startsWith("لأجله") && plain.length() < 12) {
                drawForThis(w);

            } else if (isSectionTitle(plain)) {
                w.y -= 3;
                paragraph(w, line, w.bold, 13.5f, Align.CENTER, 0);

            } else if (plain.startsWith("وبمضمنه")) {
                w.y -= 6;
                paragraph(w, line, w.bold, 14, Align.CENTER, 0);

            } else if (plain.startsWith("توقيع المفوض")) {
                drawSignature(w, line);

            } else if (plain.startsWith("المرفقات")) {
                paragraph(w, line, w.regular, 10.5f, Align.JUSTIFY, 0);

            } else if (isBullet(line)) {
                // ➢ on the right then the text a bit inside
                w.ensureSpace(BODY_SIZE * LINE_HEIGHT);
                drawArrow(w.out, RIGHT - 2, w.y - BODY_SIZE * 0.62f);
                paragraph(w, removeBullet(line), w.bold, BODY_SIZE, Align.JUSTIFY, 16);

            } else {
                paragraph(w, line, w.bold, BODY_SIZE, Align.JUSTIFY, 0);
            }
        }
    }

    // "لأجلـــه" centered with a line, like the real reports
    private void drawForThis(Writer w) throws IOException {

        float size = 14;
        w.ensureSpace(size * LINE_HEIGHT + 6);
        w.y -= 4;

        float baseline = w.y - size;
        String visual = w.visual("لأجلـــه", w.bold);
        float textWidth = w.width(visual, w.bold, size);
        float center = PAGE_WIDTH / 2;

        w.text(visual, w.bold, size, center - textWidth / 2, baseline);
        w.out.setLineWidth(1f);
        line(w.out, center - 150, baseline - 4, center + textWidth / 2, baseline - 4);

        w.y -= size * LINE_HEIGHT + 4;
    }

    // "توقيع المفوض القضائي:" centered, then empty space for the signature and the stamp
    private void drawSignature(Writer w, String line) throws IOException {

        float size = 15;
        float stampSpace = 90;

        // the title must fit on the page; the stamp can use the bottom margin
        w.ensureSpace(size * LINE_HEIGHT + 30);
        w.y -= 8;
        paragraph(w, line, w.bold, size, Align.CENTER, 0);
        w.y -= stampSpace;
    }

    // =====================================================================
    // 4. annex: one framed photo per page, "الصورة رقم N ..." under it
    // =====================================================================

    private void drawAnnex(Writer w, List<Attachment> attachments) {

        if (attachments == null) {
            return;
        }

        // same numbers as in the text written by the AI (الصورة رقم 1، 2...)
        int number = 1;

        for (Attachment attachment : attachments) {

            String caption = "الصورة رقم " + number;
            if (!isBlank(attachment.getCaption())) {
                caption = caption + " " + attachment.getCaption();
            }

            try {
                byte[] bytes = storageService.download(attachment.getStorageKey());

                if ("application/pdf".equalsIgnoreCase(attachment.getContentType())) {
                    drawPdfAttachment(w, bytes, caption);
                } else {
                    PDImageXObject image = PDImageXObject.createFromByteArray(w.pdf, bytes, attachment.getOriginalName());
                    drawFramedImage(w, image, readOrientation(bytes), caption);
                }

            } catch (Exception e) {
                // one bad file must not block the whole report
                log.warn("Attachment {} could not be added to the PDF: {}", attachment.getId(), e.getMessage());
            }

            number++;
        }
    }

    // each page of the PDF file becomes an image, framed like a photo
    private void drawPdfAttachment(Writer w, byte[] bytes, String caption) throws IOException {

        try (PDDocument source = Loader.loadPDF(bytes)) {

            PDFRenderer renderer = new PDFRenderer(source);
            int pages = Math.min(source.getNumberOfPages(), MAX_PDF_PAGES);

            for (int i = 0; i < pages; i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 110);
                PDImageXObject image = JPEGFactory.createFromImage(w.pdf, pageImage, 0.85f);

                String pageCaption = pages > 1
                        ? caption + " (" + (i + 1) + "/" + pages + ")"
                        : caption;

                drawFramedImage(w, image, 1, pageCaption);
            }
        }
    }

    private void drawFramedImage(Writer w, PDImageXObject image, int orientation, String caption) throws IOException {

        w.newPage();

        float padding = 8;
        float captionSpace = 50;
        float maxWidth = TEXT_WIDTH - 2 * padding;
        float maxHeight = TOP - BOTTOM - captionSpace - 2 * padding;

        // photo taken with the phone turned: width and height are swapped
        boolean sideways = orientation == 6 || orientation == 8;
        float imageWidth = sideways ? image.getHeight() : image.getWidth();
        float imageHeight = sideways ? image.getWidth() : image.getHeight();

        float scale = Math.min(maxWidth / imageWidth, maxHeight / imageHeight);
        float width = imageWidth * scale;
        float height = imageHeight * scale;
        float x = (PAGE_WIDTH - width) / 2;
        float y = TOP - padding - height;

        // black frame around the photo
        w.out.setLineWidth(2.5f);
        w.out.addRect(x - padding, y - padding, width + 2 * padding, height + 2 * padding);
        w.out.stroke();

        w.out.saveGraphicsState();
        if (orientation == 6) {
            // turn 90° clockwise
            w.out.transform(new Matrix(0, -1, 1, 0, x, y + height));
            w.out.drawImage(image, 0, 0, height, width);
        } else if (orientation == 8) {
            // turn 90° anti-clockwise
            w.out.transform(new Matrix(0, 1, -1, 0, x + width, y));
            w.out.drawImage(image, 0, 0, height, width);
        } else if (orientation == 3) {
            // upside down
            w.out.transform(new Matrix(-1, 0, 0, -1, x + width, y + height));
            w.out.drawImage(image, 0, 0, width, height);
        } else {
            w.out.drawImage(image, x, y, width, height);
        }
        w.out.restoreGraphicsState();

        w.y = y - padding - 12;
        paragraph(w, caption, w.bold, 12, Align.CENTER, 0);
    }

    // EXIF orientation of a phone photo (1 = normal, 3 = upside down, 6 / 8 = turned)
    private int readOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exif != null && exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception e) {
            // no EXIF (png, screenshot...) -> normal
        }
        return 1;
    }

    // "1/3" at the bottom of each page (only when there is more than one page)
    private void drawPageNumbers(PDDocument pdf, PDType0Font font) throws IOException {

        int total = pdf.getNumberOfPages();
        if (total < 2) {
            return;
        }

        for (int i = 0; i < total; i++) {
            try (PDPageContentStream out = new PDPageContentStream(
                    pdf, pdf.getPage(i), PDPageContentStream.AppendMode.APPEND, true, true)) {

                String text = (i + 1) + "/" + total;
                float width = font.getStringWidth(text) / 1000 * 9;

                out.beginText();
                out.setFont(font, 9);
                out.newLineAtOffset((PAGE_WIDTH - width) / 2, 28);
                out.showText(text);
                out.endText();
            }
        }
    }

    // =====================================================================
    // text: cut the paragraph in lines, justify, right-to-left
    // =====================================================================

    private void paragraph(Writer w, String text, PDType0Font font, float size, Align align, float rightIndent) throws IOException {

        boolean rtl = hasArabic(text);
        String shaped = w.clean(rtl ? shape(text) : text, font).trim();
        if (shaped.isEmpty()) {
            return;
        }

        float maxWidth = TEXT_WIDTH - rightIndent;
        float spaceWidth = w.width(" ", font, size);

        List<String> line = new ArrayList<>();
        float lineWidth = 0;

        for (String word : shaped.split(" +")) {
            float wordWidth = w.width(word, font, size);

            if (!line.isEmpty() && lineWidth + spaceWidth + wordWidth > maxWidth) {
                drawLine(w, line, font, size, align, rtl, maxWidth, rightIndent, false);
                line.clear();
                lineWidth = 0;
            }

            if (!line.isEmpty()) {
                lineWidth += spaceWidth;
            }
            line.add(word);
            lineWidth += wordWidth;
        }

        drawLine(w, line, font, size, align, rtl, maxWidth, rightIndent, true);
    }

    private void drawLine(Writer w, List<String> words, PDType0Font font, float size, Align align,
                          boolean rtl, float maxWidth, float rightIndent, boolean lastLine) throws IOException {

        w.ensureSpace(size * LINE_HEIGHT);

        float baseline = w.y - size;
        float right = RIGHT - rightIndent;
        float left = right - maxWidth;

        // put the words of the line in the order we see them (right-to-left for arabic)
        String visual = reorder(String.join(" ", words), rtl);

        if (align == Align.JUSTIFY && !lastLine) {
            String[] parts = visual.split(" ");

            if (parts.length > 1) {
                float total = 0;
                for (String part : parts) {
                    total += w.width(part, font, size);
                }
                float gap = (maxWidth - total) / (parts.length - 1);

                float x = left;
                for (String part : parts) {
                    w.text(part, font, size, x, baseline);
                    x += w.width(part, font, size) + gap;
                }

                w.y -= size * LINE_HEIGHT;
                return;
            }
        }

        float lineWidth = w.width(visual, font, size);
        float x;

        if (align == Align.CENTER) {
            x = left + (maxWidth - lineWidth) / 2;
        } else if (align == Align.LEFT || (align == Align.JUSTIFY && !rtl)) {
            x = left;
        } else {
            x = right - lineWidth;
        }

        w.text(visual, font, size, x, baseline);
        w.y -= size * LINE_HEIGHT;
    }

    // one line of text at a fixed place (letterhead, مرجعنا...)
    private void textAt(Writer w, String text, PDType0Font font, float size, float x, float baseline, Align align) throws IOException {

        String visual = w.visual(text, font);
        float width = w.width(visual, font, size);

        float start = x;
        if (align == Align.CENTER) {
            start = x - width / 2;
        } else if (align == Align.RIGHT) {
            start = x - width;
        }

        w.text(visual, font, size, start, baseline);
    }

    // =====================================================================
    // arabic helpers
    // =====================================================================

    // joins the arabic letters (ا ل ع ر ب ي ة -> العربية): PDFBox can't do it alone
    private static String shape(String text) {
        try {
            ArabicShaping shaping = new ArabicShaping(
                    ArabicShaping.LETTERS_SHAPE
                            | ArabicShaping.TEXT_DIRECTION_LOGICAL
                            | ArabicShaping.LENGTH_GROW_SHRINK
            );
            return shaping.shape(text);
        } catch (ArabicShapingException e) {
            return text;
        }
    }

    // logical order (how we type) -> visual order (how we see it on the paper)
    private static String reorder(String text, boolean rtl) {
        Bidi bidi = new Bidi();
        bidi.setPara(text, rtl ? Bidi.RTL : Bidi.LTR, null);
        return bidi.writeReordered(Bidi.DO_MIRRORING);
    }

    private static boolean hasArabic(String text) {
        for (char c : text.toCharArray()) {
            if ((c >= '؀' && c <= 'ۿ') || (c >= 'ﭐ' && c <= '﻿')) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSectionTitle(String line) {
        if (line.length() > 80) {
            return false;
        }
        for (String word : SECTION_WORDS) {
            if (line.startsWith(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBullet(String line) {
        return line.startsWith("-") || line.startsWith("–") || line.startsWith("•")
                || line.startsWith("*") || line.startsWith("➢") || line.startsWith("▪");
    }

    private static String removeBullet(String line) {
        return line.substring(1).trim();
    }

    // the AI sometimes writes **bold** or # titles: we remove these signs
    private static String removeMarkdown(String line) {
        return line.replace("**", "").replace("__", "").replaceFirst("^#+\\s*", "");
    }

    private String titleOf(DocumentKind kind) {
        return switch (kind) {
            case NOTIFICATION -> "محضر تبليغ";
            case NOTIFICATION_INFORMATIF -> "محضر اخباري";
            case NOTIFICATION_REFUS -> "محضر رفض التوصل";
            case CONSTAT_LIEU, CONSTAT_WHATSAPP, CONSTAT_EMAIL, CONSTAT_REUNION -> "محضر معاينة";
        };
    }

    // =====================================================================
    // drawing helpers
    // =====================================================================

    private static PDType0Font loadFont(PDDocument pdf, String path) throws IOException {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return PDType0Font.load(pdf, input);
        }
    }

    private static void line(PDPageContentStream out, float x1, float y1, float x2, float y2) throws IOException {
        out.moveTo(x1, y1);
        out.lineTo(x2, y2);
        out.stroke();
    }

    private static void circle(PDPageContentStream out, float cx, float cy, float r) throws IOException {
        float k = 0.552f * r;
        out.moveTo(cx + r, cy);
        out.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);
        out.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);
        out.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r);
        out.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy);
        out.fill();
    }

    private static void roundedRect(PDPageContentStream out, float x, float y, float w, float h, float r) throws IOException {
        float k = 0.552f * r;
        out.moveTo(x + r, y);
        out.lineTo(x + w - r, y);
        out.curveTo(x + w - r + k, y, x + w, y + r - k, x + w, y + r);
        out.lineTo(x + w, y + h - r);
        out.curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
        out.lineTo(x + r, y + h);
        out.curveTo(x + r - k, y + h, x, y + h - r + k, x, y + h - r);
        out.lineTo(x, y + r);
        out.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        out.closePath();
        out.stroke();
    }

    // small ➢ arrow for the bullet lines
    private static void drawArrow(PDPageContentStream out, float x, float y) throws IOException {
        out.moveTo(x, y + 4);
        out.lineTo(x - 9, y);
        out.lineTo(x, y - 4);
        out.lineTo(x - 2.5f, y);
        out.closePath();
        out.fill();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String valueOr(String value, String other) {
        return isBlank(value) ? other : value;
    }

    // =====================================================================
    // the current page we are writing on
    // =====================================================================

    private static class Writer {

        final PDDocument pdf;
        final PDType0Font regular;
        final PDType0Font bold;

        PDPageContentStream out;

        // where the next line goes (from the bottom of the page)
        float y;

        // remembers which letters exist in the font
        final Map<String, Boolean> glyphs = new HashMap<>();

        Writer(PDDocument pdf, PDType0Font regular, PDType0Font bold) {
            this.pdf = pdf;
            this.regular = regular;
            this.bold = bold;
        }

        void newPage() throws IOException {
            close();
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            out = new PDPageContentStream(pdf, page);
            y = TOP;
        }

        // go to a new page if the next block does not fit
        void ensureSpace(float height) throws IOException {
            if (y - height < BOTTOM) {
                newPage();
            }
        }

        void close() throws IOException {
            if (out != null) {
                out.close();
                out = null;
            }
        }

        float width(String text, PDType0Font font, float size) throws IOException {
            return font.getStringWidth(text) / 1000 * size;
        }

        void text(String visual, PDType0Font font, float size, float x, float baseline) throws IOException {
            out.beginText();
            out.setFont(font, size);
            out.newLineAtOffset(x, baseline);
            out.showText(visual);
            out.endText();
        }

        // shaped + right-to-left + only letters the font has: ready to draw
        String visual(String text, PDType0Font font) {
            boolean rtl = hasArabic(text);
            return reorder(clean(rtl ? shape(text) : text, font), rtl);
        }

        // removes the letters the font can't draw (emoji...) instead of crashing
        String clean(String text, PDType0Font font) {
            StringBuilder builder = new StringBuilder();

            text.codePoints().forEach(code -> {
                if (code == '\t') {
                    code = ' ';
                }
                if (Character.isISOControl(code)) {
                    return;
                }

                String letter = new String(Character.toChars(code));
                String key = (font == bold ? "b" : "r") + code;

                boolean exists = glyphs.computeIfAbsent(key, k -> {
                    try {
                        font.encode(letter);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });

                if (exists) {
                    builder.append(letter);
                }
            });

            return builder.toString();
        }
    }
}
