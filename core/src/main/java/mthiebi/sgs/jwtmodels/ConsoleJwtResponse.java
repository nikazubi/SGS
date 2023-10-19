package mthiebi.sgs.jwtmodels;

import mthiebi.sgs.dto.StudentDTO;

public class ConsoleJwtResponse {

    private String jwtToken;

    private StudentDTO student;

    public ConsoleJwtResponse() {
    }

    public ConsoleJwtResponse(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public ConsoleJwtResponse(String jwtToken, StudentDTO student) {
        this.jwtToken = jwtToken;
        this.student = student;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public StudentDTO getStudent() {
        return student;
    }

    public void setStudent(StudentDTO student) {
        this.student = student;
    }
}
