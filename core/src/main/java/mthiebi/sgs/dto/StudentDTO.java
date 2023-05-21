package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {

    private Long id;

    private String firstName;

    private String lastName;

    private Long age;

    private String personalNumber;

}
