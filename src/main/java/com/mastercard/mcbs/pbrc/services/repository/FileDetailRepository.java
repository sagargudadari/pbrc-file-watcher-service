package com.mastercard.mcbs.pbrc.services.repository;

import com.mastercard.mcbs.pbrc.services.entity.FileDetail;
import com.mastercard.mcbs.pbrc.services.entity.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileDetailRepository extends JpaRepository<FileDetail, Long> {

    Optional<FileDetail> findByName(String fileName);

    Optional<FileDetail> findTop1ByStatus(FileStatus status);
}
