package mthiebi.sgs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StudentDTO {

    private long indexInList;

    private long id;

    private String firstName;

    private String lastName;

    private Long age;

    private String personalNumber;
    private String username;
    private String password;
    private String ownerMail;

}
