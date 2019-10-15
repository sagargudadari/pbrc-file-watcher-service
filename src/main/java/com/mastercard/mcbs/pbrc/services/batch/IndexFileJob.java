package com.mastercard.mcbs.pbrc.services.batch;

import com.mastercard.mcbs.pbrc.services.entity.FileDetail;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class IndexFileJob extends JobExecutionListenerSupport {

    @Autowired
    JobBuilderFactory jobBuilderFactory;

    @Autowired
    StepBuilderFactory stepBuilderFactory;

    @Autowired
    IndexFileWriter indexFileWriter;

    @Bean(name = "indexJob")
    public Job indexFileJob(Step step) {

        Job job = jobBuilderFactory.get("indexFile-Job")
                .incrementer(new RunIdIncrementer())
                .listener(this)
                .start(step)
                .build();

        return job;
    }

    @Bean
    public Step step(ItemReader<FileDetail> reader) {
        return stepBuilderFactory.get("step")
                .<FileDetail, FileDetail>chunk(1)
                .reader(reader)
                .writer(indexFileWriter)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<FileDetail> reader(@Value("#{jobParameters[pathToFile]}") String pathToFile) {
        FlatFileItemReader<FileDetail> itemReader = new FlatFileItemReader<FileDetail>();
        itemReader.setLineMapper(lineMapper());
        itemReader.setResource(new FileSystemResource(pathToFile));
        return itemReader;
    }

    @Bean
    public LineMapper<FileDetail> lineMapper() {
        DefaultLineMapper<FileDetail> lineMapper = new DefaultLineMapper<FileDetail>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("recordCount", "name");
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        BeanWrapperFieldSetMapper<FileDetail> fieldSetMapper = new BeanWrapperFieldSetMapper<FileDetail>();
        fieldSetMapper.setTargetType(FileDetail.class);
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
    }

}