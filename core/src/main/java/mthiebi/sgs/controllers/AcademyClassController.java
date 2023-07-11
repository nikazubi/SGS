package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.*;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.service.AcademyClassService;
import mthiebi.sgs.utils.AuthConstants;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
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

    @Autowired
    private UtilsJwt utilsJwt;

    @PostMapping("/create-academy-class")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public AcademyClassDTO createAcademyClass(@RequestBody AcademyClassDTO academyClassDTO){
        AcademyClass academyClass = academyClassService.createAcademyClass(academyClassMapper.academyClass(academyClassDTO));
        return academyClassMapper.academyClassDTO(academyClass);
    }

    @PutMapping("/update-academy-class")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public AcademyClassDTO updateAcademyClass(@RequestBody AcademyClassDTO academyClassDTO) throws SGSException {
        AcademyClass academyClass = academyClassService.updateAcademyClass(academyClassMapper.academyClass(academyClassDTO));
        return academyClassMapper.academyClassDTO(academyClass);
    }

    @GetMapping("/get-academy-class")
    @Secured({AuthConstants.VIEW_ACADEMY_CLASS})
    public List<AcademyClassDTO> getAcademyClasses(){
        return academyClassService.getAcademyClasses().stream()
                .map(academyClass -> academyClassMapper.academyClassDTO(academyClass))
                .collect(Collectors.toList());
    }

    @GetMapping("/get-academy-classes")
    @Secured({AuthConstants.VIEW_ACADEMY_CLASS})
    public List<AcademyClassDTO> getAcademyClasses(@RequestHeader("authorization") String authHeader,
                                                   @RequestParam(required = false) String queryKey) throws Exception {

        String username = utilsJwt.getUsernameFromHeader(authHeader);
        return academyClassService.getAcademyClasses(username, queryKey).stream()
                .map(academyClass -> academyClassMapper.academyClassDTO(academyClass))
                .collect(Collectors.toList());
    }

    @GetMapping("/get-academy-class/{id}")
    @Secured({AuthConstants.VIEW_ACADEMY_CLASS})
    public AcademyClassDTO findAcademyClassById(@PathVariable Long id){
        return academyClassMapper.academyClassDTO(academyClassService.findAcademyClassById(id));
    }

    @DeleteMapping("/delete-academy-class/{id}")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public void deleteAcademyClass(@PathVariable Long id){
        academyClassService.deleteAcademyClass(id);
    }

    @PutMapping("/attach-students-to-academy-class")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public void attachStudentsToAcademyClass(@RequestParam Long academyClassId, @RequestBody StudentListDTO studentListDTO) throws SGSException {
        academyClassService.attachStudentsToAcademyClass(academyClassId, studentListDTO.getStudentIdList());
    }

    @PutMapping("/attach-subjects-to-academy-class")
    @Secured({AuthConstants.MANAGE_ACADEMY_CLASS})
    public void attachSubjectsToAcademyClass(@RequestParam Long academyClassId, @RequestBody SubjectListDTO subjectListDTO) throws SGSException {
        academyClassService.attachSubjectsToAcademyClass(academyClassId, subjectListDTO.getSubjectIdList());
    }

}
