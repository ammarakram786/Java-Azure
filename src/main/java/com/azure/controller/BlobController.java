package com.azure.controller;

import com.azure.constant.ApiErrorCode;
import com.azure.constant.JsonKey;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class BlobController {

    private final BlobContainerClient blobContainerClient;

    @Autowired
    public BlobController(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    @RequestMapping(method = RequestMethod.POST,value = "/blobs")
    public ResponseEntity<?> upload(@RequestParam(value = "file", required = false) MultipartFile file) {
        JSONObject jsonObject = new JSONObject();
        String responseMsg;
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(file.getOriginalFilename());
            blobClient.upload(file.getInputStream(),file.getSize());
            String blobUrl = blobClient.getBlobUrl();
            responseMsg = "Successfully Uploaded";
            jsonObject.put(JsonKey.URL,blobUrl);
            jsonObject.put(JsonKey.SUCCESS,true);
            jsonObject.put(JsonKey.MESSAGE,responseMsg);
        }
        catch(Exception e){
            responseMsg = e.getMessage();
            jsonObject.put(JsonKey.SUCCESS,false);
            jsonObject.put(JsonKey.ERROR_CODE, ApiErrorCode.UNKNOWN);
            jsonObject.put(JsonKey.MESSAGE,responseMsg);
        }
        return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET,value = "/blobs")
    public ResponseEntity getAllBlobs(){
        JSONObject jsonObject = new JSONObject();
        String responseMsg;
        try {
            PagedIterable<BlobItem> blobItems = blobContainerClient.listBlobs();
            List a=new ArrayList();
            for (BlobItem blobItem : blobItems) {
                a.add(blobItem.getName());
            }
            responseMsg = "Successfully Got The Data";
            jsonObject.put(JsonKey.SUCCESS,true);
            jsonObject.put(JsonKey.DATA,a);
            jsonObject.put(JsonKey.MESSAGE,responseMsg);
        }catch (Exception e){
            responseMsg = e.getMessage();
            jsonObject.put(JsonKey.SUCCESS,false);
            jsonObject.put(JsonKey.ERROR_CODE, ApiErrorCode.UNKNOWN);
            jsonObject.put(JsonKey.MESSAGE,responseMsg);
        }

        return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
    }


    @RequestMapping(method = RequestMethod.GET,value = "/download")
    public ResponseEntity download(@RequestParam(name = "fileName")String fileName, HttpServletResponse response) throws IOException {
        BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=myDoc.docx");
        ByteArrayOutputStream outputStream1=new ByteArrayOutputStream();
        blobClient.download(outputStream1);
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(outputStream1.size())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new ByteArrayResource(outputStream1.toByteArray()));
    }
}

