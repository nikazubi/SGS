package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.AcademyClassMapper;
import mthiebi.sgs.dto.AcademyClassDTO;
import mthiebi.sgs.dto.StudentListDTO;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.service.AcademyClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/academy-class")
public class AcademyClassController {

    @Autowired
    private AcademyClassService academyClassService;

    @Autowired
    private AcademyClassMapper academyClassMapper;

    @PostMapping("/create-academy-class")
    public AcademyClassDTO createAcademyClass(@RequestBody AcademyClassDTO academyClassDTO){
        AcademyClass academyClass = academyClassService.createAcademyClass(academyClassMapper.academyClass(academyClassDTO));
        return academyClassMapper.academyClassDTO(academyClass);
    }

    @PutMapping("/update-academy-class")
    public AcademyClassDTO updateAcademyClass(@RequestBody AcademyClassDTO academyClassDTO){
        AcademyClass academyClass = academyClassService.updateAcademyClass(academyClassMapper.academyClass(academyClassDTO));
        return academyClassMapper.academyClassDTO(academyClass);
    }

    @GetMapping("/get-academy-class")
    public List<AcademyClassDTO> getAcademyClasses(){
        return academyClassService.getAcademyClasses().stream()
                .map(academyClass -> academyClassMapper.academyClassDTO(academyClass))
                .collect(Collectors.toList());
    }

    @DeleteMapping("/delete-academy-class")
    public void deleteAcademyClass(@RequestParam Long id){
        academyClassService.deleteAcademyClass(id);
    }

    @PutMapping("/attach-students-to-academy-class")
    public void attachStudentsToAcademyClass(@RequestParam Long academyClassCode, @RequestBody StudentListDTO studentListDTO){
        academyClassService.attachStudentsToAcademyClass(academyClassCode, studentListDTO.getStudentIdList());
    }

}
