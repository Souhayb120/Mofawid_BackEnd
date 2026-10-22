package com.example.mofawid.Enums;

// the types of reports of the app, taken from real reports of a commissaire judiciaire
public enum DocumentKind {

    // the person received the document
    NOTIFICATION("محضر تبليغ"),

    // we could not reach the person (address incomplete, nobody...)
    NOTIFICATION_INFORMATIF("محضر اخباري"),

    // the person (or someone for him) refused to receive the document
    NOTIFICATION_REFUS("محضر رفض التوصل"),

    // constat of a place (building, parking, shop...)
    CONSTAT_LIEU("محضر معاينة"),

    // constat of whatsapp messages on a phone
    CONSTAT_WHATSAPP("محضر معاينة"),

    // constat of emails
    CONSTAT_EMAIL("محضر معاينة"),

    // constat of a meeting
    CONSTAT_REUNION("محضر معاينة");

    private final String arabicLabel;

    DocumentKind(String arabicLabel) {
        this.arabicLabel = arabicLabel;
    }

    public String getArabicLabel() {
        return arabicLabel;
    }
}
