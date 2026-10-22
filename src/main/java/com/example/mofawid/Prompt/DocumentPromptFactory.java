package com.example.mofawid.Prompt;

import com.example.mofawid.Enums.DeliveryResult;
import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.Enums.ObservationKind;
import com.example.mofawid.Enums.PartyRole;
import com.example.mofawid.Enums.PartyType;
import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.DeliveryAttempt;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Observation;
import com.example.mofawid.models.Party;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.models.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the prompt sent to Gemini for each type of report.
 * Every prompt is modeled on a real report of a commissaire judiciaire (Desktop/hussier_rapports):
 * the fixed lines are already filled here by Java, and Gemini only writes the narrative part.
 */
@Component
public class DocumentPromptFactory {

    private static final ZoneId MOROCCO = ZoneId.of("Africa/Casablanca");

    private static final String MISSING = "[المعلومة غير متوفرة]";

    // current law (since 2025). Never use the old law 81-03.
    private static final String LAW =
            "بناء على الظهير الشريف رقم 1-25-49 الصادر في 09 ذي الحجة 1446 (6 يونيو 2025) "
                    + "بتنفيذ القانون رقم 46-21 المتعلق بتنظيم مهنة المفوضين القضائيين.";

    // added in the notifications
    private static final String ARTICLE_43 =
            "وبناء على الفصل الأول من المادة 43 من القانون رقم 46-21 المنظم لمهنة المفوضين القضائيين.";

    private static final String[] MONTHS = {
            "يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو",
            "يوليوز", "غشت", "شتنبر", "أكتوبر", "نونبر", "دجنبر"
    };

    // rules for all the reports
    private static final String COMMON_RULES = """
            قواعد إلزامية:
            1. أنت مساعد مفوض قضائي بالمغرب. اكتب باللغة العربية المهنية المستعملة في محاضر المفوضين القضائيين.
            2. استعمل فقط المعطيات المقدمة لك. يمنع اختلاق أي اسم أو رقم أو تاريخ أو ساعة أو عنوان أو واقعة.
            3. إذا كانت معلومة ضرورية غير متوفرة اكتب مكانها: [المعلومة غير متوفرة]
            4. الأسماء والعناوين المكتوبة بالحروف اللاتينية تنقل كما هي دون ترجمة.
            5. الأسطر الثابتة في النموذج تنقل حرفيا كما هي، وتكتب أنت فقط الجزء المشار إليه بـ [السرد].
            6. التواريخ داخل السرد تكتب بأسماء الأشهر المغربية: يناير، فبراير، مارس، أبريل، ماي، يونيو، يوليوز، غشت، شتنبر، أكتوبر، نونبر، دجنبر.
            7. استعمل صيغة الجمع للمفوض القضائي: انتقلنا، عاينا، لاحظنا، أثبتنا.
            8. الملاحظات من نوع "معاينة مباشرة" تكتب كوقائع: عاينا، لاحظنا، أثبتنا.
               الملاحظات من نوع "تصريح" تنسب لصاحبها: صرح السيد ... بأن ...
               الملاحظات من نوع "معلومة غير متحقق منها" يذكر أنه لم يتم التحقق منها.
            9. لا تقدم أي رأي أو استنتاج قانوني.
            10. استعمل السيد أو السيدة حسب الاسم، وإذا لم يكن واضحا اكتب السيد(ة).
            11. لا تستعمل Markdown ولا الرموز ** أو #.
            12. لا تضف أي شرح أو تعليق قبل المحضر أو بعده. أخرج نص المحضر فقط.
            13. المثال المقدم هو للأسلوب فقط. يمنع نقل أي اسم أو رقم أو معلومة منه.
            """;

    public String buildPrompt(
            DocumentKind kind,
            Case caseEntity,
            List<Mission> missions,
            List<Observation> observations,
            List<Party> parties,
            List<DeliveryAttempt> attempts,
            List<Attachment> attachments
    ) {

        return switch (kind) {

            case NOTIFICATION ->
                    buildNotificationPrompt(caseEntity, parties, attempts);

            case NOTIFICATION_INFORMATIF ->
                    buildInformatifPrompt(caseEntity, missions, parties, attempts);

            case NOTIFICATION_REFUS ->
                    buildRefusPrompt(caseEntity, parties, attempts);

            case CONSTAT_LIEU ->
                    buildConstatLieuPrompt(caseEntity, missions, observations, parties, attachments);

            case CONSTAT_WHATSAPP ->
                    buildConstatWhatsappPrompt(caseEntity, missions, observations, parties, attachments);

            case CONSTAT_EMAIL ->
                    buildConstatEmailPrompt(caseEntity, missions, observations, parties, attachments);

            case CONSTAT_REUNION ->
                    buildConstatReunionPrompt(caseEntity, missions, observations, parties, attachments);
        };
    }

    // =====================================================================
    // 1. محضر تبليغ  (real example: Document 422 / 423)
    // =====================================================================

    private String buildNotificationPrompt(
            Case caseEntity,
            List<Party> parties,
            List<DeliveryAttempt> attempts
    ) {

        Party target = findParty(parties, PartyRole.TARGET);

        return """
                مهمتك: صياغة محضر تبليغ (المعني بالأمر توصل بالوثيقة).

                %s

                النموذج الإلزامي (انقل الأسطر الثابتة حرفيا واكتب فقط [السرد]):

                محضر تبليغ

                مرجعنا: %s                بتاريخ: %s

                %s

                %s

                بناء على طلب %s

                والرامي الى الانتقال الى %s الكائن ب %s

                وذلك قصد: تبليغه ب %s.

                %s

                لأجلـــه

                [السرد]

                وبمضمنه حررنا هذا المحضر للرجوع اليه عند الاقتضاء.
                توقيع المفوض القضائي:

                تعليمات [السرد]:
                - فقرة واحدة قصيرة انطلاقا من الزيارة التي تم فيها التوصل.
                - إذا توصل المعني بالأمر شخصيا: بتاريخ ... توصل السيد ... شخصيا بذكره الحامل لبطاقة التعريف الوطنية عدد ... بنسخة من ... ووقع على نسخة منها.
                - إذا توصل شخص آخر نيابة عنه: اذكر اسمه وصفته ورقم بطاقته إن وجد.
                - إذا لم يوقع، اكتب ذلك بدل "ووقع على نسخة منها".
                - التاريخ في السرد يكتب بالأرقام بهذا الشكل: 2026/07/13

                رقم بطاقة التعريف الوطنية للمعني بالأمر: %s

                زيارات التبليغ:
                %s

                مثال واقعي للأسلوب (لا تنقل منه أي معلومة):
                بتاريخ 2026/07/13 توصل السيد فلان الفلاني شخصيا بذكره الحامل لبطاقة التعريف الوطنية عدد X000000 بنسخة من هذه الرسالة ووقع على نسخة منها.
                """
                .formatted(
                        COMMON_RULES,
                        safe(caseEntity.getRef()),
                        shortDate(LocalDate.now()),
                        weTheUndersigned(caseEntity.getTenant(), true),
                        LAW,
                        requesterText(caseEntity, parties),
                        target == null ? MISSING : personName(target),
                        target == null ? MISSING : safe(target.getAddress()),
                        safe(caseEntity.getSubject()),
                        ARTICLE_43,
                        target == null ? MISSING : safe(target.getCin()),
                        formatAttempts(attempts)
                );
    }

    // =====================================================================
    // 2. محضر اخباري  (real example: Document 428)
    // =====================================================================

    private String buildInformatifPrompt(
            Case caseEntity,
            List<Mission> missions,
            List<Party> parties,
            List<DeliveryAttempt> attempts
    ) {

        Party target = findParty(parties, PartyRole.TARGET);

        return """
                مهمتك: صياغة محضر اخباري (تعذر تبليغ المعني بالأمر).

                %s

                النموذج الإلزامي (انقل الأسطر الثابتة حرفيا واكتب فقط [السرد]):

                محضر اخباري

                مرجعنا: %s                بتاريخ: %s

                %s

                %s

                بناء على طلب %s

                والرامي الى الانتقال الى %s الكائن ب %s

                وذلك قصد: تبليغه ب %s.

                %s

                لأجلـــه

                [السرد]

                وبمضمنه حررنا هذا المحضر للرجوع اليه عند الاقتضاء.
                توقيع المفوض القضائي:

                تعليمات [السرد]:
                - ابدأ بـ: انتقل كاتبنا المحلف السيد %s إلى العنوان المطلوب وذلك بحسب ما ورد في ملاحظته المدونة على صورة من الوثيقة رفقته.
                - ثم: فيما يلي نص الملاحظة: ثم اذكر كل زيارة بتاريخها وسبب تعذر التبليغ كما هو في الملاحظات.
                - التاريخ يكتب بالأرقام بهذا الشكل: 18/07/2026

                زيارات التبليغ:
                %s

                مثال واقعي للأسلوب (لا تنقل منه أي معلومة):
                انتقل كاتبنا المحلف السيد فلان إلى العنوان المطلوب وذلك بحسب ما ورد في ملاحظته المدونة على صورة من الرسالة رفقته.
                فيما يلي نص الملاحظة: بتاريخ 18/07/2026 انتقلنا الى تجزئة ... فتعذر علينا الوصول الى المعني بالأمر كون العنوان لم يحدد لنا رقم العقار و رقم الزنقة.
                """
                .formatted(
                        COMMON_RULES,
                        safe(caseEntity.getRef()),
                        shortDate(LocalDate.now()),
                        weTheUndersigned(caseEntity.getTenant(), true),
                        LAW,
                        requesterText(caseEntity, parties),
                        target == null ? MISSING : personName(target),
                        target == null ? MISSING : safe(target.getAddress()),
                        safe(caseEntity.getSubject()),
                        ARTICLE_43,
                        agentName(missions),
                        formatAttempts(attempts)
                );
    }

    // =====================================================================
    // 3. محضر رفض التوصل  (real example: CamScanner, Agadir)
    // =====================================================================

    private String buildRefusPrompt(
            Case caseEntity,
            List<Party> parties,
            List<DeliveryAttempt> attempts
    ) {

        Party target = findParty(parties, PartyRole.TARGET);

        return """
                مهمتك: صياغة محضر رفض التوصل.

                %s

                النموذج الإلزامي (انقل الأسطر الثابتة حرفيا واكتب فقط [السرد]):

                محضر رفض التوصل

                مرجعنا: %s                بتاريخ: %s

                %s

                %s

                %s

                بناء على الطلب الذي تقدم به %s الرامي إلى تبليغ: %s
                إلى %s
                %s
                %s

                [السرد]

                وبمضمنه حررنا هذا المحضر للرجوع إليه عند الضرورة.
                توقيع المفوض القضائي:

                تعليمات [السرد]:
                - ابدأ بـ: ولهذه الغاية
                - اذكر كل زيارة في جملة مستقلة وبالترتيب: بتاريخ ... انتقلنا إلى عنوان المبلغ إليه المذكور أعلاه، وبعد طرقنا الباب والضغط على الجرس عدة مرات لم يجبنا أحد.
                - إذا تم ترك بطاقة الزيارة اذكر: وقمنا بترك بطاقة الزيارة تحت الباب.
                - في زيارة الرفض: هناك وجدنا السيد ... بصفته ... حسب تصريحه، وبعد أن عرفناه بصفتنا وموضوع مهمتنا، عرضنا عليه ... المراد تبليغه فرفض التوصل به.
                - اختم بـ: ودون نص ... الذي رفض التوصل به على ظهر هذا المحضر.
                - التاريخ يكتب بالأرقام بهذا الشكل: 2026/07/23

                زيارات التبليغ (بالترتيب):
                %s

                مثال واقعي للأسلوب (لا تنقل منه أي معلومة):
                ولهذه الغاية انتقلنا بتاريخ 2026/07/23 إلى عنوان المبلغ إليه المذكور أعلاه، وبعد طرقنا الباب والضغط على الجرس عدة مرات لم يجبنا أحد وقمنا بترك بطاقة الزيارة تحت الباب.
                وبتاريخ 2026/07/28 انتقلنا إلى عنوان المبلغ إليه المذكور أعلاه، هناك وجدنا السيد فلان بصفته والد المعني بالأمر حسب تصريحه، وبعد أن عرفناه بصفتنا وموضوع مهمتنا، عرضنا عليه الرسالة المراد تبليغها فرفض التوصل بها.
                ودون نص الرسالة الذي رفض التوصل به على ظهر هذا المحضر.
                """
                .formatted(
                        COMMON_RULES,
                        safe(caseEntity.getRef()),
                        shortDate(LocalDate.now()),
                        weTheUndersigned(caseEntity.getTenant(), true),
                        LAW,
                        ARTICLE_43,
                        requesterText(caseEntity, parties),
                        safe(caseEntity.getSubject()),
                        target == null ? MISSING : personName(target),
                        target == null || isBlank(target.getCin()) ? "" : "Cin " + target.getCin(),
                        target == null ? MISSING : safe(target.getAddress()),
                        formatAttempts(attempts)
                );
    }

    // =====================================================================
    // 4. محضر معاينة مكان  (real example: Occitania)
    // =====================================================================

    private String buildConstatLieuPrompt(
            Case caseEntity,
            List<Mission> missions,
            List<Observation> observations,
            List<Party> parties,
            List<Attachment> attachments
    ) {

        return """
                مهمتك: صياغة محضر معاينة لمكان.

                %s

                النموذج الإلزامي (انقل الأسطر الثابتة حرفيا واكتب فقط [السرد]):

                محضر معاينة

                مرجعنا: %s                بتاريخ: %s

                %s

                %s

                وبناء على الطلب الذي تقدم به %s والذي جاء فيه: "%s"

                لأجلـــه

                [السرد]

                وبمضمنه حررنا هذا المحضر للرجوع اليه عند الحاجة.
                %s
                توقيع المفوض القضائي:

                تعليمات [السرد]:
                - ابدأ بـ: وبناء على طلب ...، انتقلنا بتاريخ (التاريخ بالحروف) على الساعة (الساعة بالحروف ثم بالأرقام مثل 16H24) الى (العنوان)
                - إذا كان هناك شخص حاضر: وذلك بحضور، وتحت عهدة ومسؤولية السيد(ة) ... بصفته ... بتصريحه، قصد ...
                - ثم: وعليه، أثبتنا ما يلي:
                - ثم أقسام مرقمة: أولا: (عنوان القسم) ثم ثانيا: ثم ثالثا: ... حسب عناوين الملاحظات، وتحت كل قسم الوقائع بجمل واضحة.
                - إذا كانت الوقائع متعددة داخل قسم واحد، اكتب كل واقعة في سطر يبدأ بـ -

                المكان المطلوب معاينته: %s
                تاريخ وساعة الانتقال: %s
                الشخص الحاضر: %s

                الملاحظات المسجلة في عين المكان:
                %s

                مثال واقعي للأسلوب (لا تنقل منه أي معلومة):
                وبناء على طلب السيدة فلانة، انتقلنا بتاريخ 19 غشت 2026 على الساعة الرابعة وأربعة وعشرين دقيقة زوالا 16H24 الى إقامة "..." الكائنة بشارع ... وذلك بحضور، وتحت عهدة ومسؤولية السيدة فلانة، بصفتها قاطنة بالإقامة المعنية بتصريحها، قصد معاينة حالة المصعد الكهربائي.
                وعليه، أثبتنا ما يلي:
                أولا: معاينة مدخل العمارة
                توجهنا إلى مدخل العمارة الحاملة للرقم "8"، الظاهر بشكل بارز أعلى المدخل الرئيسي، وتحققنا من كون هذه العمارة هي المعنية بطلب المعاينة.
                ثانيا: معاينة المصعد الكهربائي
                عاينا وجود مصعدين كهربائيين يمين البهو، وأثبتنا أن مؤشر الإشارة الضوئية الموجود بجانب باب المصعد الموجود أقصى اليمين كان مضاء باللون الأحمر، وأن باب المصعد كان في وضعية مغلقة، ولم يستجب لمحاولة استدعائه.
                """
                .formatted(
                        COMMON_RULES,
                        safe(caseEntity.getRef()),
                        shortDate(LocalDate.now()),
                        weTheUndersigned(caseEntity.getTenant(), false),
                        LAW,
                        requesterText(caseEntity, parties),
                        safe(caseEntity.getDescription()),
                        attachmentsLine(attachments,
                                "المرفقات: الصور الفوتوغرافية الملتقطة من عين المكان الموثقة لما أشرنا اليه أعلاه والمؤشر عليها بطابعنا."),
                        safe(caseEntity.getAddress()),
                        firstObservationTime(observations),
                        presentPerson(missions),
                        formatObservations(observations)
                );
    }

    // =====================================================================
    // 5. محضر معاينة رسائل واتساب  (real example: Averda / WhatsApp)
    // =====================================================================

    private String buildConstatWhatsappPrompt(
            Case caseEntity,
            List<Mission> missions,
            List<Observation> observations,
            List<Party> parties,
            List<Attachment> attachments
    ) {

        return """
                مهمتك: صياغة محضر معاينة وتفريغ رسائل تطبيق واتساب.

                %s

                قواعد خاصة بالتفريغ:
                - نص كل رسالة يكتب حرفيا كما هو بين علامتي تنصيص، بدون ترجمة وبدون تصحيح، حتى إذا كان بالدارجة أو بالفرنسية أو مختلطا.
                - اذكر لكل رسالة: المرسل، اليوم، الساعة (مثل 17H36)، ونوعها (نصية أو صوتية أو وثيقة).

                النموذج الإلزامي (انقل الأسطر الثابتة حرفيا واكتب فقط [السرد]):

                محضر معاينة

                مرجعنا: %s                بتاريخ: %s

                %s

                %s

                وبناء على الطلب الذي تقدم به %s والذي جاء فيه: "%s"

                لأجلـــه

                [السرد]

                وبمضمنه حررنا هذا المحضر للرجوع اليه عند الحاجة.
                %s
                توقيع المفوض القضائي:

                تعليمات [السرد]:
                - ابدأ بـ: فإنه بتاريخ ... على الساعة ... انتقلنا الى ... ومن خلال الهاتف المحمول الممسوك بين يدي السيد(ة) ... والمعرف بالمراجع التالية: (نوع الهاتف، رقم الطراز، الرقم التسلسلي)
                - ثم: وبعد أخذنا المعلومات المومأ اليها أعلاه، عاينا السيد(ة) ... تتصفح الهاتف المحمول المطلوب فيه المعاينة، وبإرشاد منها، وتحت عهدتها ومسؤوليتها، ... فقد سجلنا أنه جاء فيها ما يلي:
                - ثم الرسائل مجمعة حسب اليوم بعنوان مثل: الرسائل النصية يوم 30 يونيو 2026 أو الرسائل الصوتية المتبادلة يوم 02 يوليوز 2026
                - الرسائل الصوتية ترقم: الرسالة الأولى، الرسالة الثانية ...

                مكان الانتقال: %s
                تاريخ وساعة الانتقال: %s
                الشخص الحاضر الماسك للهاتف: %s

                المعطيات المسجلة (معلومات الهاتف والرسائل):
                %s

                مثال واقعي للأسلوب (لا تنقل منه أي معلومة):
                فإنه بتاريخ 2026/09/07 على الساعة الثانية عشر وعشر دقائق زوالا، انتقلنا الى مقر الشركة بعنوانها أعلاه، ومن خلال الهاتف المحمول الممسوك بين يدي السيدة فلانة، والمعرف بالمراجع التالية:
                Nom de téléphone: Iphone ...
                Model Number: ...
                NUMERO DE SERIE: ...
                الرسائل النصية يوم 30 يونيو 2026
                عاينا وجود ثلاثة رسائل نصية متتالية مرسلة من صاحب رقم الهاتف المذكور أعلاه جاء فيها ما يلي: "..."
                الرسالة الأولى: مرسلة من طرف السيدة فلانة على الساعة 12H04 زوالا جاء فيها: "..."
                """
                .formatted(
                        COMMON_RULES,
                        safe(caseEntity.getRef()),
                        shortDate(LocalDate.now()),
                        weTheUndersigned(caseEntity.getTenant(), false),
                        LAW,
                        requesterText(caseEntity, parties),
                        safe(caseEntity.getDescription()),
                        attachmentsLine(attachments,
                                "المرفقات: الوثائق المذكورة أعلاه مؤشر عليها بطابعنا."),
                        safe(caseEntity.getAddress()),
                        firstObservationTime(observations),
                        presentPerson(missions),
                        formatObservations(observations)
                );
    }

    // =====================================================================
    // 6. محضر معاينة بريد إلكتروني  (real example: Averda / emails)
    // =====================================================================

    private String buildConstatEmailPrompt(
            Case caseEntity,
            List<Mission> missions,
            List<Observation> observations,
            List<Party> parties,
            List<Attachment> attachments
    ) {

        return """
                مهمتك: صياغة محضر معاينة وتفريغ رسائل بريد إلكتروني.

                %s

                قواعد خاصة:
                - لكل رسالة اذكر: اليوم والتاريخ، الساعة، الحساب المرسل (البريد الإلكتروني كما هو)، الموضوع، ثم ملخص أمين لمضمونها دون إضافة.
                - بعد كل رسالة اذكر الصورة الموافقة لها: والصورة رقم N أدناه موثقة لما أشرنا إليه أعلاه. ثم سطر: الصورة رقم N: (التعليق)
                - استعمل الصور بالترتيب وبنفس التعليقات المقدمة.

                النموذج الإلزامي (انقل الأسطر الثابتة حرفيا واكتب فقط [السرد]):

                محضر معاينة

                مرجعنا: %s                بتاريخ: %s

                %s

                %s

                بناء على الطلب الذي تقدم به %s والذي جاء فيه: "%s"

                لأجلـــه

                [السرد]

                وبمضمنه حررنا هذا المحضر للرجوع اليه عند الحاجة.
                توقيع المفوض القضائي:

                تعليمات [السرد]:
                - ابدأ بـ: وبناء على الطلب المذكور أعلاه، انتقلنا بتاريخ ... على الساعة ... الى ...، وهناك خاطبنا السيد(ة) ... بصفته ... بتصريحه، وبعد اطلاعنا على البريد الإلكتروني المهني الخاص به، وتحت عهدته ومسؤوليته الشخصية، فقد دلنا على الرسائل الإلكترونية المراد معاينتها وتفريغها في هذا المحضر.
                - ثم أقسام مرقمة حسب التاريخ: أولا: المراسلة المؤرخة في ... ثم ثانيا: ...

                مكان الانتقال: %s
                تاريخ وساعة الانتقال: %s
                الشخص الحاضر: %s

                الرسائل المسجلة:
                %s

                الصور المرفقة (بالترتيب):
                %s

                مثال واقعي للأسلوب (لا تنقل منه أي معلومة):
                أولا: المراسلة المؤرخة في 08 يوليوز 2026
                عاينا رسالة صادرة يوم الأربعاء 08 يوليوز 2026 على الساعة 17:14 من الحساب المهني ...@... ضمن سلسلة مراسلات تتعلق بـ ...، ويتضمن نصها طلب ...
                والصورة رقم 1 أدناه موثقة لما أشرنا إليه أعلاه.
                الصورة رقم 1: مراسلة 2026/07/08 بشأن ...
                """
                .formatted(
                        COMMON_RULES,
                        safe(caseEntity.getRef()),
                        shortDate(LocalDate.now()),
                        weTheUndersigned(caseEntity.getTenant(), false),
                        LAW,
                        requesterText(caseEntity, parties),
                        safe(caseEntity.getDescription()),
                        safe(caseEntity.getAddress()),
                        firstObservationTime(observations),
                        presentPerson(missions),
                        formatObservations(observations),
                        formatAttachments(attachments)
                );
    }

    // =====================================================================
    // 7. محضر معاينة اجتماع  (real example: Averda / CDT meeting)
    // =====================================================================

    private String buildConstatReunionPrompt(
            Case caseEntity,
            List<Mission> missions,
            List<Observation> observations,
            List<Party> parties,
            List<Attachment> attachments
    ) {

        return """
                مهمتك: صياغة محضر معاينة انعقاد اجتماع.

                %s

                قواعد خاصة:
                - كل ما قاله الحاضرون هو تصريح وينسب لصاحبه: أخذ السيد ... الكلمة ...، صرح ...، أكد ...، طالب ...
                - لا تحكم على صحة ما قيل.

                النموذج الإلزامي (انقل الأسطر الثابتة حرفيا واكتب فقط [السرد]):

                محضر معاينة

                مرجعنا: %s                بتاريخ: %s

                %s

                %s

                بناء على الطلب الذي تقدم به %s والذي يطلب منا بمقتضاه: "%s"

                لأجلـــه

                [السرد]

                وبمضمنه حررنا هذا المحضر للرجوع اليه عند الحاجة.
                %s
                توقيع المفوض القضائي:

                تعليمات [السرد]:
                - ابدأ بـ: ونزولا عند القيام بالمطلوب، فقد انتقلنا بتاريخ ... على الساعة ... (بالحروف ثم بالأرقام مثل 11H00) الى ...، وعند وقوفنا بعين المكان، وبعد أخذ مكاننا بقاعة الاجتماعات، وتعريفنا الحاضرين بصفتنا وموضوع مهمتنا، فإننا قمنا بعد ذلك بما يلي:
                - أولا: عاينا قيام الحاضرين بالتوقيع على لائحة الحضور على النحو الآتي: ثم الحاضرون من كل جانب.
                - ثم مجريات الاجتماع بالترتيب، مع نسبة كل كلام لصاحبه.
                - اختم بـ: لينتهي الاجتماع عند هذا الحد على الساعة ... وغادر الجميع عين المكان.

                مكان الاجتماع: %s
                تاريخ وساعة الانتقال: %s
                ساعة آخر ملاحظة (نهاية الاجتماع): %s

                الملاحظات المسجلة خلال الاجتماع (بالترتيب):
                %s

                مثال واقعي للأسلوب (لا تنقل منه أي معلومة):
                أولا: عاينا قيام الحاضرين من جانب الشركة ومن جانب المكتب النقابي بالتوقيع على لائحة الحضور على النحو الآتي:
                بخصوص ممثلي الشركة حضر السادة: فلان، فلان.
                بعد ذلك، أخذ السيد فلان الكلمة رحب من خلالها بالجميع ونوه الى أن هذا الاجتماع يأتي في سياق الحوار ...
                أما من جانب المكتب النقابي، فقد صرحوا بأنهم ...
                لينتهي الاجتماع عند هذا الحد على الساعة الواحدة زوالا 13H00 وغادر الجميع عين المكان.
                """
                .formatted(
                        COMMON_RULES,
                        safe(caseEntity.getRef()),
                        shortDate(LocalDate.now()),
                        weTheUndersigned(caseEntity.getTenant(), false),
                        LAW,
                        requesterText(caseEntity, parties),
                        safe(caseEntity.getDescription()),
                        attachmentsLine(attachments,
                                "المرفقات: الوثائق المذكورة أعلاه مؤشر عليها بطابعنا."),
                        safe(caseEntity.getAddress()),
                        firstObservationTime(observations),
                        lastObservationTime(observations),
                        formatObservations(observations)
                );
    }

    // =====================================================================
    // helpers
    // =====================================================================

    // "نحن الموقع أسفله الأستاذ ... مفوض قضائي ..."
    // notifications use the court of appeal, constats use the first instance courts (like the real reports)
    private String weTheUndersigned(Tenant tenant, boolean notification) {

        String name = tenant == null ? MISSING : safe(tenant.getHuissierNameAr());

        String court;

        if (notification) {
            court = "بدائرة استئناف "
                    + (tenant == null ? MISSING : safe(tenant.getAppealCourtAr()));
        } else {
            court = "لدى "
                    + (tenant == null ? MISSING : safe(tenant.getFirstInstanceCourtAr()));
        }

        return "نحن الموقع أسفله الأستاذ " + name + " مفوض قضائي " + court + ".";
    }

    // the requester: the REQUESTER party, or the client of the case if there is no party
    private String requesterText(Case caseEntity, List<Party> parties) {

        Party requester = findParty(parties, PartyRole.REQUESTER);

        if (requester == null) {
            return safe(caseEntity.getClientName())
                    + " الكائن ب " + safe(caseEntity.getAddress());
        }

        if (requester.getType() == PartyType.COMPANY) {

            String text = "شركة " + requester.getFullName()
                    + " الكائن مقرها الاجتماعي في " + safe(requester.getAddress());

            if (!isBlank(requester.getLegalRepresentative())) {
                text += " في شخص " + requester.getLegalRepresentative();
            }

            return text;
        }

        String text = personName(requester);

        if (!isBlank(requester.getCin())) {
            text += " الحامل(ة) لبطاقة التعريف الوطنية عدد " + requester.getCin();
        }

        text += " الكائن(ة) ب " + safe(requester.getAddress());

        return text;
    }

    private String personName(Party party) {

        if (party.getType() == PartyType.COMPANY) {
            return "شركة " + party.getFullName();
        }

        return "السيد(ة) " + party.getFullName();
    }

    private Party findParty(List<Party> parties, PartyRole role) {

        if (parties == null) {
            return null;
        }

        for (Party party : parties) {
            if (party.getRole() == role) {
                return party;
            }
        }

        return null;
    }

    private String formatAttempts(List<DeliveryAttempt> attempts) {

        if (attempts == null || attempts.isEmpty()) {
            return "[لا توجد زيارات]";
        }

        StringBuilder builder = new StringBuilder();

        int number = 1;

        for (DeliveryAttempt attempt : attempts) {

            builder.append("- الزيارة ").append(number).append(":\n");

            builder.append("  التاريخ: ")
                    .append(dateTimeText(attempt.getAttemptedAt()))
                    .append("\n");

            builder.append("  النتيجة: ")
                    .append(resultText(attempt.getResult()))
                    .append("\n");

            if (!isBlank(attempt.getReceiverName())) {
                builder.append("  الشخص الذي وجدناه: ")
                        .append(attempt.getReceiverName())
                        .append("\n");
            }

            if (!isBlank(attempt.getReceiverCapacity())) {
                builder.append("  صفته: ")
                        .append(attempt.getReceiverCapacity())
                        .append("\n");
            }

            if (!isBlank(attempt.getReceiverCin())) {
                builder.append("  رقم بطاقة التعريف الوطنية: ")
                        .append(attempt.getReceiverCin())
                        .append("\n");
            }

            if (attempt.getSigned() != null) {
                builder.append("  وقع على النسخة: ")
                        .append(attempt.getSigned() ? "نعم" : "لا")
                        .append("\n");
            }

            if (Boolean.TRUE.equals(attempt.getVisitCardLeft())) {
                builder.append("  تم ترك بطاقة الزيارة تحت الباب\n");
            }

            if (!isBlank(attempt.getNotes())) {
                builder.append("  ملاحظات: ")
                        .append(attempt.getNotes())
                        .append("\n");
            }

            builder.append("\n");
            number++;
        }

        return builder.toString();
    }

    private String resultText(DeliveryResult result) {

        if (result == null) {
            return MISSING;
        }

        return switch (result) {
            case DELIVERED_IN_PERSON -> "توصل المعني بالأمر شخصيا";
            case DELIVERED_TO_OTHER -> "توصل شخص آخر نيابة عن المعني بالأمر";
            case REFUSED -> "رفض التوصل";
            case NOBODY_THERE -> "لم يجبنا أحد";
            case ADDRESS_INCOMPLETE -> "العنوان غير محدد بدقة";
            case ADDRESS_NOT_FOUND -> "العنوان غير موجود";
            case MOVED -> "المعني بالأمر انتقل من العنوان";
        };
    }

    // observations sorted by time, with their section title
    private String formatObservations(List<Observation> observations) {

        if (observations == null || observations.isEmpty()) {
            return "[لا توجد ملاحظات]";
        }

        List<Observation> sorted = new ArrayList<>(observations);
        sorted.sort(Comparator.comparing(
                Observation::getObservedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        StringBuilder builder = new StringBuilder();

        for (Observation observation : sorted) {

            if (!isBlank(observation.getSectionTitle())) {
                builder.append("- القسم: ")
                        .append(observation.getSectionTitle())
                        .append("\n");
            } else {
                builder.append("- القسم: [بدون عنوان]\n");
            }

            builder.append("  نوع الملاحظة: ")
                    .append(kindText(observation.getKind()))
                    .append("\n");

            if (observation.getObservedAt() != null) {
                builder.append("  الوقت: ")
                        .append(dateTimeText(toLocal(observation)))
                        .append("\n");
            }

            builder.append("  النص: ")
                    .append(safe(observation.getDescription()))
                    .append("\n\n");
        }

        return builder.toString();
    }

    private String kindText(ObservationKind kind) {

        if (kind == null) {
            return MISSING;
        }

        return switch (kind) {
            case DIRECT -> "معاينة مباشرة";
            case DECLARATION -> "تصريح";
            case UNVERIFIED -> "معلومة غير متحقق منها";
        };
    }

    private String formatAttachments(List<Attachment> attachments) {

        if (attachments == null || attachments.isEmpty()) {
            return "[لا توجد صور]";
        }

        StringBuilder builder = new StringBuilder();

        int number = 1;

        for (Attachment attachment : attachments) {

            String caption = isBlank(attachment.getCaption())
                    ? attachment.getOriginalName()
                    : attachment.getCaption();

            builder.append("الصورة رقم ")
                    .append(number)
                    .append(": ")
                    .append(caption)
                    .append("\n");

            number++;
        }

        return builder.toString();
    }

    // the "المرفقات" line only when there are attachments
    private String attachmentsLine(List<Attachment> attachments, String line) {

        if (attachments == null || attachments.isEmpty()) {
            return "";
        }

        return line;
    }

    // the collaborateur who did the mission (كاتبنا المحلف)
    private String agentName(List<Mission> missions) {

        Mission mission = lastMission(missions);

        if (mission == null || mission.getAssignedTo() == null) {
            return MISSING;
        }

        User agent = mission.getAssignedTo();

        if (!isBlank(agent.getPrenom()) || !isBlank(agent.getNom())) {
            return (safeEmpty(agent.getPrenom()) + " " + safeEmpty(agent.getNom())).trim();
        }

        return agent.getUsername();
    }

    // "السيدة ... بصفتها ..."
    private String presentPerson(List<Mission> missions) {

        Mission mission = lastMission(missions);

        if (mission == null || isBlank(mission.getPresentPersonName())) {
            return "[لا يوجد]";
        }

        return mission.getPresentPersonName()
                + " بصفته(ا): " + safe(mission.getPresentPersonCapacity());
    }

    private Mission lastMission(List<Mission> missions) {

        if (missions == null || missions.isEmpty()) {
            return null;
        }

        Mission last = missions.get(0);

        for (Mission mission : missions) {
            if (mission.getCreatedAt().isAfter(last.getCreatedAt())) {
                last = mission;
            }
        }

        return last;
    }

    private String firstObservationTime(List<Observation> observations) {

        Observation first = null;

        for (Observation observation : observations) {
            if (observation.getObservedAt() == null) {
                continue;
            }
            if (first == null || observation.getObservedAt().isBefore(first.getObservedAt())) {
                first = observation;
            }
        }

        return first == null ? MISSING : dateTimeText(toLocal(first));
    }

    private String lastObservationTime(List<Observation> observations) {

        Observation last = null;

        for (Observation observation : observations) {
            if (observation.getObservedAt() == null) {
                continue;
            }
            if (last == null || observation.getObservedAt().isAfter(last.getObservedAt())) {
                last = observation;
            }
        }

        return last == null ? MISSING : dateTimeText(toLocal(last));
    }

    private LocalDateTime toLocal(Observation observation) {
        return LocalDateTime.ofInstant(observation.getObservedAt(), MOROCCO);
    }

    // ex: 2026/07/17 (used in the header "بتاريخ")
    private String shortDate(LocalDate date) {
        return "%d/%02d/%02d".formatted(
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth()
        );
    }

    // ex: 19 غشت 2026 على الساعة 16H24
    private String dateTimeText(LocalDateTime dateTime) {

        if (dateTime == null) {
            return MISSING;
        }

        return dateTime.getDayOfMonth() + " "
                + MONTHS[dateTime.getMonthValue() - 1] + " "
                + dateTime.getYear()
                + " على الساعة "
                + "%02dH%02d".formatted(dateTime.getHour(), dateTime.getMinute());
    }

    private String safe(String value) {

        if (value == null || value.isBlank()) {
            return MISSING;
        }

        return value;
    }

    private String safeEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
