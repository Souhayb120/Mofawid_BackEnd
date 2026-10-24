package com.example.mofawid.services;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String upload(MultipartFile file, String storageKey);

    // for files we create ourselves (ex: the PDF of a report)
    String uploadBytes(byte[] content, String storageKey, String contentType);

    byte[] download(String storageKey);

    void delete(String storageKey);
}