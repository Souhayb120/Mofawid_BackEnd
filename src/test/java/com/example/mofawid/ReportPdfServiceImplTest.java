package com.example.mofawid;

import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.models.Attachment;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Document;
import com.example.mofawid.models.Tenant;
import com.example.mofawid.services.ServicesImpl.ReportPdfServiceImpl;
import com.example.mofawid.services.StorageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// builds a sample report (no database, no R2) and saves it in target/report-sample.pdf to look at it
class ReportPdfServiceImplTest {

    @Test
    void buildsTheReportPdf() throws Exception {

        Tenant tenant = new Tenant();
        tenant.setName("Etude Achray");
        tenant.setHuissierNameAr("عبد الغني أشرع");
        tenant.setHuissierNameFr("Achray Abdelghani");
        tenant.setAppealCourtAr("الدار البيضاء");
        tenant.setAppealCourtFr("Casablanca");
        tenant.setAdress("شارع مصطفى المعاني زنقة طرابلس الرقم 11 الطابق 1 الشقة 2 الدار البيضاء");
        tenant.setAddressFr("Bd Mustafa El Maani Rue Tarablous Imm 11 1er Etg App 2 Casablanca");
        tenant.setEmail("office@example.com");
        tenant.setPhone("06.00.00.00.00");

        Case caseEntity = new Case();
        caseEntity.setRef("2026/2528");
        caseEntity.setTenant(tenant);

        Document document = new Document();
        document.setCaseEntity(caseEntity);
        document.setKind(DocumentKind.CONSTAT_LIEU);
        document.setApprovedAt(Instant.parse("2026-08-31T10:00:00Z"));
        document.setContent("""
                محضر معاينة
                مرجعنا: 2026/2528                بتاريخ: 2026/08/31
                نحن الموقع أسفله الأستاذ عبد الغني أشرع مفوض قضائي لدى المحاكم الابتدائية بالدار البيضاء.
                بناء على الظهير الشريف رقم 1-25-49 الصادر في 09 ذي الحجة 1446 (6 يونيو 2025) بتنفيذ القانون رقم 46-21 المتعلق بتنظيم مهنة المفوضين القضائيين.
                وبناء على الطلب الذي تقدمت به السيدة فلانة الحاملة لبطاقة التعريف الوطنية عدد X000000 والذي جاء فيه " يشرفني أن ألتمس من سيادتكم التفضل بالانتقال إلى إقامة "Occitania" الكائنة بشارع النوادي (Boulevard des Clubs) ، قصد إجراء معاينة دقيقة لإثبات الحالة المتعلقة بالوقائع التالية:
                - معاينة حالة المصعد الكهربائي الموجود بالعمارة رقم 8 بالإقامة المذكورة، والتأكد من كونه خارج نطاق الخدمة والاستعمال؛
                - تضمين المحضر صورا فوتوغرافية توثق جميع العناصر المعاينة أعلاه، مع تحديد التاريخ والساعة ومكان المعاينة بدقة.
                لأجلـــه
                وبناء على طلب السيدة المذكورة، انتقلت بتاريخ 19 غشت 2026 على الساعة الرابعة وأربعة وعشرين دقيقة زوالا 16H24 الى إقامة "Occitania" وذلك بحضور، وتحت عهدة ومسؤولية السيدة فلانة، بصفتها قاطنة الإقامة المعنية بالأمر.
                وعليه، أثبت ما يلي:
                أولا: معاينة مدخل العمارة رقم 8
                توجهت إلى مدخل العمارة الحاملة للرقم "8"، الظاهر بشكل بارز أعلى المدخل الرئيسي، والمكسو بالرخام، وتحققت من كون هذه العمارة هي المعنية بطلب المعاينة.
                ثانيا: معاينة المصعد الكهربائي
                عند دخولي الى بهو العمارة المذكورة، عاينت وجود مصعدين كهربائيين يمين بهوها، وأثبت أن مؤشر الإشارة الضوئية الموجود بجانب باب المصعد الموجود اقصى اليمين كان مضاء باللون الأحمر.
                وبمضمنه حررنا هذا المحضر للرجوع اليه عند الحاجة.
                المرفقات: الصور الفوتوغرافية الملتقطة من عين المكان الموثقة لما اشرنا اليه أعلاه والمؤشر عليها بطابعنا.
                توقيع المفوض القضائي:
                """);

        // one photo: a fake storage returns a generated image instead of R2
        Attachment photo = new Attachment();
        photo.setStorageKey("photo.jpg");
        photo.setOriginalName("photo.jpg");
        photo.setContentType("image/jpeg");
        photo.setCaption("مدخل العمارة رقم 8");

        ReportPdfServiceImpl service = new ReportPdfServiceImpl(new FakeStorage(samplePhoto()));
        byte[] pdf = service.buildPdf(document, List.of(photo));

        Path file = Path.of("target", "report-sample.pdf");
        Files.write(file, pdf);

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            // page 1 = the report, page 2 = the photo
            assertEquals(2, loaded.getNumberOfPages());
        }
    }

    private static byte[] samplePhoto() throws Exception {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 800, 600);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(250, 150, 300, 300);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private static class FakeStorage implements StorageService {

        private final byte[] content;

        FakeStorage(byte[] content) {
            this.content = content;
        }

        @Override
        public String upload(MultipartFile file, String storageKey) {
            return storageKey;
        }

        @Override
        public String uploadBytes(byte[] bytes, String storageKey, String contentType) {
            return storageKey;
        }

        @Override
        public byte[] download(String storageKey) {
            return content;
        }

        @Override
        public void delete(String storageKey) {
        }
    }
}
