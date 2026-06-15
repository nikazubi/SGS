package mthiebi.sgs.models;

import lombok.Builder;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity(name = "ACADEMY_CLASS")
public class AcademyClass extends mthiebi.sgs.models.Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long classLevel;

    private String className;

    @Builder.Default
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "academyClass", cascade = CascadeType.ALL)
    private List<TotalAbsence> totalAbsences = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "academy_class_id")
    private List<mthiebi.sgs.models.Student> studentList;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "class_subject",
            joinColumns = @JoinColumn(name = "academy_class_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private List<mthiebi.sgs.models.Subject> subjectList;

    @Builder.Default
    private Boolean isTransit = Boolean.FALSE;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getClassLevel() {
        return classLevel;
    }

    public void setClassLevel(Long classLevel) {
        this.classLevel = classLevel;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<mthiebi.sgs.models.Student> getStudentList() {
        return studentList;
    }

    public void setStudentList(List<mthiebi.sgs.models.Student> studentList) {
        this.studentList = studentList;
    }

    public List<mthiebi.sgs.models.Subject> getSubjectList() {
        return subjectList;
    }

    public void setSubjectList(List<mthiebi.sgs.models.Subject> subjectList) {
        this.subjectList = subjectList;
    }

    public List<TotalAbsence> getTotalAbsences() {
        return totalAbsences;
    }

    public void setTotalAbsences(List<TotalAbsence> totalAbsences) {
        this.totalAbsences = totalAbsences;
    }

    public void addTotalAbsence(TotalAbsence totalAbsence) {
        totalAbsence.setAcademyClass(this);
        totalAbsences.add(totalAbsence);
    }

    public Boolean getIsTransit() {
        return isTransit;
    }

    public void setIsTransit(Boolean isTransit) {
        this.isTransit = isTransit;
    }

    @Transient
    public TotalAbsence getActiveTotalAbsence() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return getTotalAbsenceForYearAndMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) ); //erased + 1
    }

    @Transient
    public TotalAbsence getTotalAbsenceForYearAndMonth(Integer year, Integer month) {
        return this.totalAbsences.stream().filter(totalAbsence -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(totalAbsence.getActivePeriod());
                    if (month.equals(1) || month.equals(2)) {
                        return calendar.get(Calendar.YEAR) == year &&
                                (calendar.get(Calendar.MONTH)  == 1 || calendar.get(Calendar.MONTH) == 2); //erased + 1
                    }
                    if (month.equals(9) || month.equals(10)) {
                        return calendar.get(Calendar.YEAR) == year &&
                                (calendar.get(Calendar.MONTH)  == 9 || calendar.get(Calendar.MONTH) == 10); //erased + 1
                    }
                    return calendar.get(Calendar.YEAR) == year &&
                            (calendar.get(Calendar.MONTH)  == month); //erased + 1
                })
                .max(Comparator.comparing(Audit::getCreateTime))
                .orElse(TotalAbsence.builder()
                        .academyClass(this)
                        .activePeriod(null)
                        .totalAcademyHour(0L)
                        .build());
    }

    @Override
    public String toString() {
        return "AcademyClass{" +
                "id=" + id +
                ", classLevel=" + classLevel +
                ", className='" + className + '\'' +
                ", studentList=" + studentList +
                ", subjectList=" + subjectList +
                '}';
    }
}
