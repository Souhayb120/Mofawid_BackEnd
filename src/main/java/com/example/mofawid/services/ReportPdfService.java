package com.example.mofawid.services;

import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.Document;

import java.util.List;

public interface ReportPdfService {

    // builds the PDF of the report like the real ones:
    // letterhead + title + مرجعنا/بتاريخ + text + signature, then one page per photo
    byte[] buildPdf(Document document, List<Attachment> attachments);

    // only photos and PDF files are printed in the report (not audio, word...)
    static boolean isPrintable(Attachment attachment) {
        String type = attachment.getContentType() == null
                ? ""
                : attachment.getContentType().toLowerCase();

        return type.startsWith("image/") || type.equals("application/pdf");
    }
}
