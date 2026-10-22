package com.example.mofawid.Enums;

// result of one visit to notify someone
public enum DeliveryResult {
    DELIVERED_IN_PERSON, // توصل شخصيا
    DELIVERED_TO_OTHER,  // توصل من ينوب عنه (زوجته، والده...)
    REFUSED,             // رفض التوصل
    NOBODY_THERE,        // لم يجبنا أحد
    ADDRESS_INCOMPLETE,  // العنوان غير محدد (رقم العقار، رقم الزنقة...)
    ADDRESS_NOT_FOUND,   // عنوان غير موجود
    MOVED                // انتقل من العنوان
}
