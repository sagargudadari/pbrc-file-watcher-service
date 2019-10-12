package com.mastercard.filewatcherservice.batch;

import com.mastercard.filewatcherservice.entity.FileDetail;
import com.mastercard.filewatcherservice.repository.FileDetailRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;

@Component
public class IndexFileWriter implements ItemWriter<FileDetail> {
 
 @Autowired
 private FileDetailRepository repo;
 
 @Override
 @Transactional
 public void write(List<? extends FileDetail> users) throws Exception {
 repo.saveAll(users);
 }
 
}