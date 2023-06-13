package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.StudentDTO;
import mthiebi.sgs.dto.StudentMapper;
import mthiebi.sgs.models.Student;
import mthiebi.sgs.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentMapper studentMapper;

    @PostMapping("/create-student")
    public StudentDTO createStudent(@RequestBody StudentDTO studentDTO){
        Student student = studentService.createStudent(studentMapper.student(studentDTO));
        return studentMapper.studentDTO(student);
    }

    @PutMapping("/update-student")
    public StudentDTO updateStudent(@RequestBody StudentDTO studentDTO){
        Student student = studentService.updateStudent(studentMapper.student(studentDTO));
        return studentMapper.studentDTO(student);
    }

    @GetMapping("/get-students")
    public List<StudentDTO> getStudents(@RequestParam int limit,
                                        @RequestParam int page,
                                        @RequestParam Long id,
                                        @RequestParam String firstName,
                                        @RequestParam String lastName,
                                        @RequestParam String personalNumber){
        return studentService.getStudents(limit, page, id, firstName, lastName, personalNumber).stream()
                .map(student -> studentMapper.studentDTO(student))
                .collect(Collectors.toList());
    }

    @GetMapping("/get-students-by-name")
    public List<StudentDTO> getStudents(@RequestParam String queryKey){
        return studentService.findByNameAndSurname(queryKey).stream()
                .map(student -> studentMapper.studentDTO(student))
                .collect(Collectors.toList());
    }

    @GetMapping("/get-student/{id}")
    public StudentDTO getStudents(@PathVariable Long id){
        return studentMapper.studentDTO(studentService.findStudentById(id));
    }

    @DeleteMapping("/delete-students/{id}")
    public void deleteStudent(@PathVariable Long id){
        studentService.deleteStudent(id);
    }


}
