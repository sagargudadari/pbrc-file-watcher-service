package com.mastercard.mcbs.pbrc.services.controller;


import com.mastercard.mcbs.pbrc.services.entity.FileDetail;
import com.mastercard.mcbs.pbrc.services.exception.ResourceNotFoundException;
import com.mastercard.mcbs.pbrc.services.service.FileDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/file-watcher-service")
public class FileDetailController {

    private FileDetailService fileDetailService;

    @Autowired
    public FileDetailController(FileDetailService fileDetailService) {
        this.fileDetailService = fileDetailService;
    }

    @GetMapping
    public ResponseEntity<FileDetail> getNewFileDetails() throws ResourceNotFoundException {

        return new ResponseEntity(fileDetailService.getFileToBeProcessed(), OK);
    }

    @PatchMapping("{id}")
    public ResponseEntity<FileDetail> updateFileDetails(@PathVariable(name = "id") Long fileId,
                                                        @Valid @RequestBody FileDetail fileDetail)
            throws ResourceNotFoundException {

        return new ResponseEntity(fileDetailService.updateFileDetail(fileId, fileDetail), OK);
    }
}
