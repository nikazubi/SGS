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
    public List<StudentDTO> getStudents(){
        return studentService.getStudents().stream()
                .map(student -> studentMapper.studentDTO(student))
                .collect(Collectors.toList());
    }

    @DeleteMapping("/delete-students")
    public void deleteStudent(@RequestParam Long id){
        studentService.deleteStudent(id);
    }


}
