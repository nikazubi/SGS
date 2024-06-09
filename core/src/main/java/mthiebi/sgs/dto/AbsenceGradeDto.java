package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AbsenceGradeDto extends AuditDTO {

    private Long id;

    private BigDecimal value;

    private String gradeType;

    private StudentDTO student;

    private SubjectDTO subject;

    private AcademyClassDTO academyClass;

    private Date exactMonth;
}
