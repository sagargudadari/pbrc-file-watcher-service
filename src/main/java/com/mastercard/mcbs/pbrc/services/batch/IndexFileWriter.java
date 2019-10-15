package com.mastercard.mcbs.pbrc.services.batch;

import com.mastercard.mcbs.pbrc.services.entity.FileDetail;
import com.mastercard.mcbs.pbrc.services.repository.FileDetailRepository;
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