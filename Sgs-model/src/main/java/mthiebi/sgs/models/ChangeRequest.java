package mthiebi.sgs.models;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity(name = "CHANGE_REQUESTS")
public class ChangeRequest extends Audit{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private SystemUser issuer;

    @OneToOne(fetch = FetchType.LAZY)
    private Grade prevGrade;

    private BigDecimal prevValue;

    private BigDecimal newValue;

    @Enumerated(EnumType.STRING)
    private ChangeRequestStatus status;

    @Column(columnDefinition = "nvarchar(max)")
    private String description;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public SystemUser getIssuer() {
        return issuer;
    }

    public void setIssuer(SystemUser issuer) {
        this.issuer = issuer;
    }

    public Grade getPrevGrade() {
        return prevGrade;
    }

    public void setPrevGrade(Grade prevGrade) {
        this.prevGrade = prevGrade;
    }

    public BigDecimal getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(BigDecimal prevValue) {
        this.prevValue = prevValue;
    }

    public BigDecimal getNewValue() {
        return newValue;
    }

    public void setNewValue(BigDecimal newValue) {
        this.newValue = newValue;
    }

    public ChangeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ChangeRequestStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
