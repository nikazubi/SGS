package mthiebi.sgs.models;

import javax.persistence.*;

@Entity(name = "CHANGE_REQUESTS")
public class ChangeRequest extends Audit{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private SystemUser issuer;

    @OneToOne(fetch = FetchType.LAZY)
    private Student student;

    @OneToOne(fetch = FetchType.LAZY)
    private Grade prevGrade;

    @OneToOne(fetch = FetchType.LAZY)
    private Subject subject;

    private Long prevValue;

    private Long newValue;

    @Enumerated(EnumType.STRING)
    private ChangeRequestStatus status;

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

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Grade getPrevGrade() {
        return prevGrade;
    }

    public void setPrevGrade(Grade prevGrade) {
        this.prevGrade = prevGrade;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Long getPrevValue() {
        return prevValue;
    }

    public void setPrevValue(Long prevValue) {
        this.prevValue = prevValue;
    }

    public Long getNewValue() {
        return newValue;
    }

    public void setNewValue(Long newValue) {
        this.newValue = newValue;
    }

    public ChangeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ChangeRequestStatus status) {
        this.status = status;
    }
}
