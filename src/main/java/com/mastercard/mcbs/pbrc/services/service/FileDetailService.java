package com.mastercard.mcbs.pbrc.services.service;

import com.mastercard.mcbs.pbrc.services.constants.ExceptionConstant;
import com.mastercard.mcbs.pbrc.services.entity.FileDetail;
import com.mastercard.mcbs.pbrc.services.entity.FileStatus;
import com.mastercard.mcbs.pbrc.services.exception.ResourceNotFoundException;
import com.mastercard.mcbs.pbrc.services.model.TableNameCheckResult;
import com.mastercard.mcbs.pbrc.services.repository.FileDetailRepository;
import com.mastercard.mcbs.pbrc.services.utils.UntarArchiveUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.kie.api.runtime.KieContainer;
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

    private final KieContainer kieContainer;

    @Autowired
    public FileDetailService(FileDetailRepository fileDetailRepository, IndexFileService indexFileService, KieContainer kieContainer) {
        this.fileDetailRepository = fileDetailRepository;
        this.indexFileService = indexFileService;
        this.kieContainer = kieContainer;
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
                UntarArchiveUtils.unTarFile(file.getAbsolutePath(), new File(ftpReadDir));
            }
        } else {
            log.info("Process completed. Ignoring as file name contains 'TEMP' keyword {}", file.getName());
        }
    }

    private FileDetail transformAndPersistFileDetail(File file) throws ResourceNotFoundException {
        Optional<FileDetail> fileDetail = fileDetailRepository.findByName(file.getName());
        if (fileDetail.isPresent() && FileStatus.COMPLETED == fileDetail.get().getStatus()) {
            log.info("Ignore persist. Already exists in database File name : {}", file.getName());
            return fileDetail.get();
        }

        fileDetail = Optional.ofNullable(transformFileDetail(file, fileDetail.get()));
        return persistFileDetail(fileDetail);
    }

    private FileDetail transformFileDetail(File file, FileDetail fileDetail) throws ResourceNotFoundException {
        String[] splitName = file.getName().split(typeRegexPattern);
        TableNameCheckResult tableNameCheckResult = new TableNameCheckResult();
        tableNameCheckResult.setType(splitName[0].concat(splitName[splitName.length - 1]));

        KieSession kieSession = kieContainer.newKieSession("rulesSession");
        kieSession.insert(tableNameCheckResult);
        kieSession.fireAllRules();
        kieSession.dispose();
        log.info("File type : {} and Table name : {}", tableNameCheckResult.getType(),
                tableNameCheckResult.getTargetTableName());

        Optional.ofNullable(tableNameCheckResult.getTargetTableName())
                .orElseThrow(() -> new ResourceNotFoundException("No suitable table name found"));

        return FileDetail.builder()
                .id(fileDetail.getId())
                .location(file.getAbsolutePath())
                .name(file.getName())
                .recordCount(fileDetail.getRecordCount())
                .type(tableNameCheckResult.getType())
                .targetTableName(tableNameCheckResult.getTargetTableName())
                .status(FileStatus.READY)
                .createdDate(fileDetail.getCreatedDate())
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

    private FileDetail persistFileDetail(Optional<FileDetail> fileDetail) {
        log.info("Persist file detail into database File Name : {}", fileDetail.get().getName());
        return fileDetailRepository.save(fileDetail.get());
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
