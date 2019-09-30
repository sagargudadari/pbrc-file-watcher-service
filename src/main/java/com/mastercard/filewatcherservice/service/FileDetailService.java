package com.mastercard.filewatcherservice.service;

import com.mastercard.filewatcherservice.constants.ExceptionConstant;
import com.mastercard.filewatcherservice.entity.FileDetail;
import com.mastercard.filewatcherservice.entity.FileStatus;
import com.mastercard.filewatcherservice.exception.ResourceNotFoundException;
import com.mastercard.filewatcherservice.repository.FileDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FileDetailService implements MessageHandler {

    @Value("${file.regexIgnore.pattern}")
    private String fileIgnorePattern;

    @Value("${type.regex.pattern}")
    private String[] typeRegexPattern;

    private FileDetailRepository fileDetailRepository;

    @Autowired
    public FileDetailService(FileDetailRepository fileDetailRepository) {
        this.fileDetailRepository = fileDetailRepository;
    }

    @Override
    public void handleMessage(Message<?> message) {
        File file = (File) message.getPayload();
        log.info("Process Start. File Name : {} and Location : {}", file.getName(), file.getAbsolutePath());

        Pattern pattern = Pattern.compile(fileIgnorePattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(FilenameUtils.removeExtension(file.getName()));
        if (!matcher.matches()) {
            FileDetail fileDetail = transformFileDetail(file);
            FileDetail fileDetailResult = persistFileDetail(fileDetail);
            log.info("Process completed Successfully. File Detail {}", fileDetailResult);
        } else {
            log.info("Process completed. Ignoring as file name contains 'TEMP' keyword {}",
                    FilenameUtils.removeExtension(file.getName()));
        }
    }

    private FileDetail transformFileDetail(File file) {
        return FileDetail.builder()
                .location(file.getAbsolutePath())
                .name(FilenameUtils.removeExtension(file.getName()))
                .type(file.getName().split(typeRegexPattern[0])[Integer.parseInt(typeRegexPattern[1])])
                .status(FileStatus.READY)
                .build();
    }

    public synchronized FileDetail getFileToBeProcessed() throws ResourceNotFoundException {
        log.info("Get one file detail that is in 'READY' status {}", FileStatus.READY.name());
        Optional<FileDetail> readyStatusFileDetail = fileDetailRepository.findTop1ByStatus(FileStatus.READY);

        if (readyStatusFileDetail.isPresent()) {
            log.info("File detail in 'READY' status : {}", readyStatusFileDetail);
            readyStatusFileDetail.get().setStatus(FileStatus.PROCESSING);
            log.info("Updating file detail status to 'PROCESSING'");
            return fileDetailRepository.save(readyStatusFileDetail.get());
        } else {
            throw new ResourceNotFoundException(ExceptionConstant.RECORD_NOT_FOUND_MSG + "file status 'READY'");
        }
    }

    private FileDetail persistFileDetail(FileDetail fileDetail) {
        Optional<FileDetail> fileDetail1 = fileDetailRepository.findByName(fileDetail.getName());
        if (fileDetail1.isPresent()) {
            log.info("Ignore persist. Already exists in database File name : {}", fileDetail.getName());
            return fileDetail1.get();
        }
        log.info("Persist file detail into database File Name : {}", fileDetail.getName());
        return fileDetailRepository.save(fileDetail);
    }

    public FileDetail updateFileDetail(Long fileId, FileDetail fileStatus) throws ResourceNotFoundException {
        Optional<FileDetail> fileDetail = fileDetailRepository.findById(fileId);
        if (!fileDetail.isPresent()) {
            log.error("Id " + fileId + " is not existed");
            throw new ResourceNotFoundException(ExceptionConstant.RECORD_NOT_FOUND_MSG + "file Id : " + fileId);
        }
        fileDetail.ifPresent(fs -> fs.setStatus(fileStatus.getStatus()));
        return fileDetailRepository.save(fileDetail.get());
    }

    public void setFileIgnorePattern(String fileIgnorePattern) {
        this.fileIgnorePattern = fileIgnorePattern;
    }

    public void setTypeRegexPattern(String[] typeRegexPattern) {
        this.typeRegexPattern = typeRegexPattern;
    }
}
