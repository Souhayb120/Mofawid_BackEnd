package com.example.mofawid.services;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String upload(MultipartFile file, String storageKey);

    byte[] download(String storageKey);

    void delete(String storageKey);
}