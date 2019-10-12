package com.mastercard.filewatcherservice.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.messaging.MessageChannel;

import java.io.File;

@Configuration
@EnableIntegration
public class FilePollingIntegrationFlow {

    @Value("${ftp.read.dir}")
    private String ftpReadDir;

    @Bean
    public MessageChannel fileInputChannel() {
        return new DirectChannel();
    }

    @Bean
    @InboundChannelAdapter(value = "fileInputChannel", poller = @Poller(fixedDelay = "5000"))
    public MessageSource<File> fileReadingMessageSource() {
        FileReadingMessageSource source = new FileReadingMessageSource();
        source.setDirectory(new File(ftpReadDir));
        source.setScanEachPoll(true);
        source.setUseWatchService(true);
        source.setWatchEvents(FileReadingMessageSource.WatchEventType.CREATE);
        source.setFilter(addFilters());
        return source;
    }

    private FileListFilter<File> addFilters() {
        CompositeFileListFilter<File> filters = new CompositeFileListFilter<>();
        filters.addFilter(new AcceptAllFileListFilter<>());
        filters.addFilter(new AcceptOnceFileListFilter<>());
        return filters;
    }

    @Bean
    public IntegrationFlow processFileFlow() {
        return IntegrationFlows
                .from(fileInputChannel())
                .handle("fileDetailService", "handleMessage").get();
    }

    public void setFtpReadDir(String ftpReadDir) {
        this.ftpReadDir = ftpReadDir;
    }
}