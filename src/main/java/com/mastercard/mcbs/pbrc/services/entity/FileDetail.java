package com.mastercard.mcbs.pbrc.services.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Builder
@Data
@Entity
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "file_detail")
public class FileDetail implements Serializable {

    private static final long serialVersionUID = 8043722506324881419L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String recordCount;

    private String name;

    private String type;

    private String location;

    private String targetTableName;

    private String jobExecutionId;

    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @CreationTimestamp
    private Date createdDate;

    @UpdateTimestamp
    private Date updatedDate;
}
