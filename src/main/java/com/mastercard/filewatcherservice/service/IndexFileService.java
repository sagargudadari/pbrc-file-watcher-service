package com.mastercard.filewatcherservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

@Slf4j
@Service
public class IndexFileService {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    @Qualifier("indexJob")
    Job indexFileJob;

    public void readIndexFile(File file) {

        JobParameters jobParameters = new JobParametersBuilder()
                .addDate("date", new Date())
                .addString("pathToFile", file.getAbsolutePath())
                .toJobParameters();
        try {
            jobLauncher.run(indexFileJob, jobParameters);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            log.error("Batch job of index failed with message : {}", e.getLocalizedMessage());
        }
    }
}
