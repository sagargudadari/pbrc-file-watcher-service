package com.mastercard.filewatcherservice.service;

import com.mastercard.filewatcherservice.constants.ExceptionConstant;
import com.mastercard.filewatcherservice.entity.FileDetail;
import com.mastercard.filewatcherservice.entity.FileStatus;
import com.mastercard.filewatcherservice.exception.ResourceNotFoundException;
import com.mastercard.filewatcherservice.model.TableNameCheckResult;
import com.mastercard.filewatcherservice.repository.FileDetailRepository;
import com.mastercard.filewatcherservice.utils.UntarArchiveUtils;
import com.mastercard.filewatcherservice.utils.KieUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.kie.api.runtime.KieSession;
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
    private String typeRegexPattern;

    @Value("${ftp.read.dir}")
    private String ftpReadDir;

    private FileDetailRepository fileDetailRepository;

    private IndexFileService indexFileService;

    @Autowired
    public FileDetailService(FileDetailRepository fileDetailRepository, IndexFileService indexFileService) {
        this.fileDetailRepository = fileDetailRepository;
        this.indexFileService = indexFileService;
    }

    @Override
    public void handleMessage(Message<?> message) {
        File file = (File) message.getPayload();
        log.info("Process Start. File Name : {} and Location : {}", file.getName(), file.getAbsolutePath());

        Pattern pattern = Pattern.compile(fileIgnorePattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(FilenameUtils.removeExtension(file.getName()));

        if (!matcher.matches()) {
            if (file.getName().endsWith("index.csv")) {
                log.info("File came is index " + file.getName());
                indexFileService.readIndexFile(file);
            } else if (file.getName().endsWith("rules.csv")) {
                log.info("File came is rules" + file.getName());
            } else if (file.getName().endsWith("BA") || file.getName().endsWith("BE")) {
                try {
                    FileDetail fileDetailResult = transformAndPersistFileDetail(file);
                    log.info("Process completed Successfully. File Detail {}", fileDetailResult);
                } catch (ResourceNotFoundException e) {
                    log.info("Process completed. Ignoring file name {},"
                            + " as No suitable table name found", file.getName());
                }
            } else {
                log.info("Need to untar file " + file.getName());
                UntarArchiveUtils.unTarFile(file.getAbsolutePath(), new File(ftpReadDir),file.getName());
            }
        } else {
            log.info("Process completed. Ignoring as file name contains 'TEMP' keyword {}", file.getName());
        }
    }

    private FileDetail transformAndPersistFileDetail(File file) throws ResourceNotFoundException {
        Optional<FileDetail> fileDetail = fileDetailRepository.findByName(file.getName());
        if (fileDetail.isPresent()) {
            log.info("Ignore persist. Already exists in database File name : {}", file.getName());
            return fileDetail.get();
        }

        FileDetail buildFileDetail = transformFileDetail(file);
        return persistFileDetail(buildFileDetail);
    }

    private FileDetail transformFileDetail(File file) throws ResourceNotFoundException {
        String[] splitName = file.getName().split(typeRegexPattern);
        TableNameCheckResult tableNameCheckResult = new TableNameCheckResult();
        tableNameCheckResult.setType(splitName[0].concat(splitName[splitName.length - 1]));

        KieSession kieSession = KieUtils.getKieContainer().newKieSession();
        kieSession.insert(tableNameCheckResult);
        kieSession.fireAllRules();
        log.info("File type : {} and Table name : {}", tableNameCheckResult.getType(),
                tableNameCheckResult.getTargetTableName());
        kieSession.dispose();

        Optional.ofNullable(tableNameCheckResult.getTargetTableName())
                .orElseThrow(() -> new ResourceNotFoundException("No suitable table name found"));

        return FileDetail.builder()
                .location(file.getAbsolutePath())
                .name(file.getName())
                .type(tableNameCheckResult.getType())
                .targetTableName(tableNameCheckResult.getTargetTableName())
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

    public void setTypeRegexPattern(String typeRegexPattern) {
        this.typeRegexPattern = typeRegexPattern;
    }
}
