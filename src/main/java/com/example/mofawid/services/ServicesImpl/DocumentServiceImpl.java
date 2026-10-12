package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.Enums.CaseStatus;
import com.example.mofawid.Enums.DocumentKind;
import com.example.mofawid.Enums.DocumentStatus;
import com.example.mofawid.models.Mission;
import com.example.mofawid.models.Observation;
import com.example.mofawid.repositories.MissionRepo;
import com.example.mofawid.repositories.ObservationRepo;
import com.example.mofawid.services.AiService;
import java.util.List;
import com.example.mofawid.DTO.DocumentDTO.DocumentRequestDTO;
import com.example.mofawid.DTO.DocumentDTO.DocumentResponseDTO;
import com.example.mofawid.mapper.DocumentMapper;
import com.example.mofawid.models.Case;
import com.example.mofawid.models.Document;
import com.example.mofawid.models.User;
import com.example.mofawid.repositories.*;
import com.example.mofawid.services.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final MissionRepo missionRepository;
    private final ObservationRepo observationRepository;
    private final AiService aiService;
    private final DocumentRepo documentRepository;
    private final CaseRepo caseRepository;
    private final UserRepo userRepository;
    private final DocumentMapper documentMapper;

    public DocumentServiceImpl(
            MissionRepo missionRepository, ObservationRepo observationRepository, AiService aiService, DocumentRepo documentRepository,
            CaseRepo caseRepository,
            UserRepo userRepository,
            DocumentMapper documentMapper
    ) {
        this.missionRepository = missionRepository;
        this.observationRepository = observationRepository;
        this.aiService = aiService;
        this.documentRepository = documentRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.documentMapper = documentMapper;
    }

    @Override
    public DocumentResponseDTO createDocument(
            UUID caseId,
            DocumentRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // Case must belong to current tenant
        Case caseEntity = caseRepository
                .findByIdAndTenantId(
                        caseId,
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Case not found")
                );

        Document document =
                documentMapper.toEntity(dto);

        document.setCaseEntity(caseEntity);

        document.setStatus(
                DocumentStatus.DRAFT
        );

        Document savedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                savedDocument
        );
    }

    @Override
    public DocumentResponseDTO getDocumentById(
            UUID id
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Document not found")
                );

        return documentMapper.toResponseDto(document);
    }

    @Override
    public Page<DocumentResponseDTO> getCaseDocuments(
            UUID caseId,
            int page,
            int size
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        // Important:
        // verify the Case itself belongs to current tenant
        caseRepository
                .findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() ->
                        new RuntimeException("Case not found")
                );

        Pageable pageable =
                PageRequest.of(page, size);

        Page<Document> documents =
                documentRepository
                        .findAllByCaseEntityId(
                                caseId,
                                pageable
                        );

        return documents.map(
                documentMapper::toResponseDto
        );
    }

    @Override
    public DocumentResponseDTO updateDocument(
            UUID id,
            DocumentRequestDTO dto
    ) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Document not found")
                );

        if (document.getStatus() == DocumentStatus.APPROVED) {
            throw new RuntimeException(
                    "Approved document cannot be modified"
            );
        }

        document.setKind(dto.getKind());
        document.setContent(dto.getContent());

        Document updatedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                updatedDocument
        );
    }

    @Override
    public void deleteDocument(UUID id) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        id,
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Document not found")
                );

        if (document.getStatus() == DocumentStatus.APPROVED) {
            throw new RuntimeException(
                    "Approved document cannot be deleted"
            );
        }

        documentRepository.delete(document);
    }

    private User getCurrentUser() {

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Current user not found"
                        )
                );
    }



    @Override
    public DocumentResponseDTO generateAiDraft(UUID caseId) {

        User currentUser = getCurrentUser();

        UUID tenantId = currentUser.getTenant().getId();

        // 1. Get case from same tenant
        Case caseEntity = caseRepository
                .findByIdAndTenantId(caseId, tenantId)
                .orElseThrow(() ->
                        new RuntimeException("Case not found")
                );

        // 2. Get missions
        List<Mission> missions =
                missionRepository.findAllByCaseEntityId(caseId);

        // 3. Build strict Arabic prompt
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
        أنت مساعد متخصص في صياغة مسودات محاضر المفوض القضائي بالمغرب.

        المطلوب منك إعداد "مسودة محضر معاينة" باللغة العربية
        وبأسلوب مهني ورسمي قريب من الصياغة المتداولة في مكاتب
        المفوضين القضائيين بالمغرب.

        هذه المسودة ليست وثيقة نهائية ولا تعتبر محضرا رسميا
        إلا بعد مراجعتها واعتمادها وتوقيعها من طرف المفوض القضائي.

        =========================
        قواعد إلزامية
        =========================

        1. اعتمد حصرا على المعلومات المقدمة لك أسفله.

        2. يمنع منعا باتا اختراع:
           - أسماء أشخاص
           - أرقام بطائق
           - أرقام ملفات قضائية
           - تواريخ
           - ساعات
           - عناوين
           - تصريحات
           - وقائع
           - أحكام
           - أوامر قضائية
           - أرقام قوانين غير مقدمة أو غير مؤكدة.

        3. إذا كانت معلومة ضرورية غير موجودة،
           اكتب مكانها بوضوح:
           [المعلومة غير متوفرة]
           ولا تحاول استنتاجها.

        4. يجب أن تكون المعاينات مادية ومحايدة ومجردة من أي رأي.

        لا تكتب مثلا:
        "ثبت أن الطرف تسبب في الضرر."

        بل اكتب مثلا:
        "عاينا وجود كسر ظاهر على مستوى..."

        5. لا تحدد المسؤولية القانونية لأي شخص.

        6. لا تصف فعلا بأنه:
           - غير قانوني
           - احتيالي
           - متعمد
           - يشكل اعتداء
           - يشكل إخلالا

           إلا إذا كانت هذه العبارة واردة صراحة في معطيات الملف
           ويجب حينها نسبتها لصاحب التصريح وليس تقديمها كحقيقة.

        7. فرّق بوضوح بين أنواع المعلومات:

           DIRECT:
           واقعة عاينها المكلف بالمهمة مباشرة.
           تصاغ بصيغة مثل:
           "عاينا..."
           "لاحظنا..."
           "تبين لنا ماديا..."

           DECLARATION:
           تصريح صادر عن شخص آخر.
           لا تقدمه كحقيقة.
           استعمل:
           "صرح لنا..."
           "وحسب تصريحه..."

           UNVERIFIED:
           معلومة غير متحقق منها.
           يجب التنبيه بوضوح إلى عدم التحقق منها
           وعدم تقديمها كواقعة ثابتة.

        8. لا تضف اجتهادا قانونيا ولا تحليلا ولا استنتاجات.

        9. حافظ على أسلوب عربي قانوني واضح ورسمي
           دون مبالغة أو حشو.

        10. لا تستعمل Markdown.
            لا تستعمل النجوم ** أو العناوين بصيغة #.
            أعد النص كوثيقة عربية نظيفة قابلة لاحقا للطباعة PDF.

        =========================
        البنية المطلوبة للمحضر
        =========================

        اكتب الوثيقة بهذا الترتيب:

        المملكة المغربية

        محضر معاينة

        مرجع الملف:
        ...

        لفائدة:
        ...

        الموضوع:
        ...

        أولا: السند والغرض من الإجراء

        اذكر أن المعاينة تدخل في إطار إعداد مسودة محضر
        اعتمادا على المعطيات المسجلة في الملف.

        لا تقل إن المعاينة تمت بناء على أمر قضائي
        إلا إذا كانت هذه المعلومة مقدمة صراحة.

        ثانيا: بيانات الانتقال والمهمة

        اذكر:
        - عنوان المكان
        - موضوع المهمة
        - تاريخ أو توقيت المهمة إذا كان متوفرا

        إذا لم تتوفر المعلومة اكتب:
        [المعلومة غير متوفرة]

        ثالثا: المعاينات

        رتب المعاينات ترتيبا زمنيا ومنطقيا.

        حول كل Observation إلى فقرة قانونية واضحة.

        بالنسبة لـ DIRECT:
        استخدم لغة المعاينة المباشرة.

        بالنسبة لـ DECLARATION:
        نسب الكلام إلى صاحبه فقط إذا كانت هويته موجودة.
        وإذا لم تكن موجودة، لا تخترع اسما.

        بالنسبة لـ UNVERIFIED:
        وضح أن المعلومة غير متحقق منها.

        رابعا: الخلاصة

        لا تقدم استنتاجا قانونيا.

        اكتف بصياغة مهنية مثل:
        "وبناء على ما تمت معاينته أعلاه، حررت هذه المسودة
        لإثبات الوقائع المادية المضمنة بها، قصد عرضها على
        المفوض القضائي المختص للمراجعة والاعتماد."

        خامسا: المرفقات

        لا تذكر صورا أو وثائق أو تسجيلات
        إلا إذا كانت موجودة فعلا في المعطيات.

        في النهاية أضف:

        "مسودة مولدة بمساعدة الذكاء الاصطناعي
        خاضعة للمراجعة والاعتماد من طرف المفوض القضائي."

        =========================
        معطيات الملف
        =========================
        """);

        prompt.append("\nمرجع الملف: ")
                .append(safe(caseEntity.getRef()));

        prompt.append("\nاسم طالب الإجراء / الزبون: ")
                .append(safe(caseEntity.getClientName()));

        prompt.append("\nICE: ")
                .append(safe(caseEntity.getClientIce()));

        prompt.append("\nالعنوان: ")
                .append(safe(caseEntity.getAddress()));

        prompt.append("\nالموضوع: ")
                .append(safe(caseEntity.getSubject()));

        prompt.append("\nوصف الملف: ")
                .append(safe(caseEntity.getDescription()));

        prompt.append("\n\n=========================");
        prompt.append("\nالمهام والمعاينات");
        prompt.append("\n=========================\n");

        // 4. Missions + observations
        for (Mission mission : missions) {

            prompt.append("\nالمهمة: ")
                    .append(safe(mission.getTitle()));

            prompt.append("\nوصف المهمة: ")
                    .append(safe(mission.getDescription()));

            prompt.append("\nتاريخ المهمة: ")
                    .append(
                            mission.getScheduledAt() != null
                                    ? mission.getScheduledAt().toString()
                                    : "[المعلومة غير متوفرة]"
                    );

            List<Observation> observations =
                    observationRepository.findAllByMissionId(
                            mission.getId()
                    );

            if (observations.isEmpty()) {
                prompt.append("\nلا توجد معاينات مسجلة لهذه المهمة.\n");
                continue;
            }

            prompt.append("\nالمعاينات المسجلة:\n");

            for (Observation observation : observations) {

                prompt.append("\nنوع المعاينة: ")
                        .append(observation.getKind());

                prompt.append("\nالمضمون: ")
                        .append(safe(observation.getDescription()));

                prompt.append("\nتاريخ التسجيل: ")
                        .append(
                                observation.getObservedAt() != null
                                        ? observation.getObservedAt().toString()
                                        : "[المعلومة غير متوفرة]"
                        );

                prompt.append("\n---");
            }

            prompt.append("\n");
        }

        // 5. Gemini
        String generatedText =
                aiService.generate(prompt.toString());

        // 6. Save document
        Document document = new Document();

        document.setCaseEntity(caseEntity);
        document.setKind(DocumentKind.PROCES_VERBAL);
        document.setStatus(DocumentStatus.DRAFT_AI);
        document.setContent(generatedText);

        Document savedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(savedDocument);
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "[المعلومة غير متوفرة]"
                : value;
    }

    @Override
    public DocumentResponseDTO submitForApproval(UUID documentId) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        documentId,
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Document not found")
                );

        if (document.getStatus() != DocumentStatus.DRAFT
                && document.getStatus() != DocumentStatus.DRAFT_AI) {

            throw new RuntimeException(
                    "Only a draft document can be submitted for approval"
            );
        }

        document.setStatus(
                DocumentStatus.AWAITING_APPROVAL
        );

        Case caseEntity = document.getCaseEntity();

        caseEntity.setCaseStatus(
                CaseStatus.AWAITING_APPROVAL
        );

        caseRepository.save(caseEntity);

        Document updatedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                updatedDocument
        );
    }

    @Override
    public DocumentResponseDTO approveDocument(UUID documentId) {

        User currentUser = getCurrentUser();

        UUID tenantId =
                currentUser.getTenant().getId();

        Document document = documentRepository
                .findByIdAndCaseEntityTenantId(
                        documentId,
                        tenantId
                )
                .orElseThrow(() ->
                        new RuntimeException("Document not found")
                );

        if (document.getStatus()
                != DocumentStatus.AWAITING_APPROVAL) {

            throw new RuntimeException(
                    "Document must be awaiting approval"
            );
        }

        document.setStatus(
                DocumentStatus.APPROVED
        );

        Case caseEntity = document.getCaseEntity();

        caseEntity.setCaseStatus(
                CaseStatus.CLOSED
        );

        caseRepository.save(caseEntity);

        Document updatedDocument =
                documentRepository.save(document);

        return documentMapper.toResponseDto(
                updatedDocument
        );
    }
}