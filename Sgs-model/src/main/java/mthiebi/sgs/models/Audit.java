package mthiebi.sgs.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Audit {

    private Date createTime;

    private Date lastUpdateTime;

    @PrePersist
    protected void onCreate(){
        this.createTime = new Date();
        this.lastUpdateTime = new Date();
    }

    @PreUpdate
    protected void onUpdate(){
        this.lastUpdateTime = new Date();
    }

}