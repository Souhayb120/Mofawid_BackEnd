package com.example.mofawid.Enums;

public enum DocumentKind {

    PROCES_VERBAL_CONSTAT("محضر معاينة"),

    PROCES_VERBAL_NOTIFICATION("محضر تبليغ"),
    NOTIFICATION_JUGEMENT("تبليغ حكم أو قرار قضائي"),
    NOTIFICATION_CONVOCATION("تبليغ استدعاء"),

    MISE_EN_DEMEURE("إنذار"),
    SOMMATION_DE_PAYER("إنذار بالأداء"),
    SOMMATION_INTERPELLATIVE("إنذار استجوابي"),

    PROCES_VERBAL_EXECUTION("محضر تنفيذ"),
    PROCES_VERBAL_TENTATIVE_EXECUTION("محضر محاولة تنفيذ"),
    PROCES_VERBAL_EXPULSION("محضر تنفيذ الإفراغ"),

    PROCES_VERBAL_SAISIE_MOBILIERE("محضر حجز منقول"),
    PROCES_VERBAL_SAISIE_ARRET("محضر حجز لدى الغير"),
    PROCES_VERBAL_INVENTAIRE("محضر جرد"),

    PROCES_VERBAL_VENTE_AUX_ENCHERES("محضر بيع بالمزاد العلني"),

    OFFRE_REELLE_DE_PAIEMENT("عرض حقيقي للأداء"),
    PROCES_VERBAL_DEPOT("محضر إيداع"),

    MISE_EN_DEMEURE_RECOUVREMENT("إنذار من أجل التحصيل"),
    PROCES_VERBAL_RECOUVREMENT_AMIABLE("محضر تحصيل ودي"),

    PROCES_VERBAL_ASSEMBLEE_GENERALE("محضر جمعية عامة"),

    AUTRE("وثيقة أخرى");

    private final String arabicLabel;

    DocumentKind(String arabicLabel) {
        this.arabicLabel = arabicLabel;
    }

    public String getArabicLabel() {
        return arabicLabel;
    }
}