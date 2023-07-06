package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.SubjectDTO;
import mthiebi.sgs.dto.SubjectMapper;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.service.SubjectService;
import mthiebi.sgs.service.SystemUserService;
import mthiebi.sgs.utils.AuthConstants;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subjects")
public class SubjectController {
    
    @Autowired
    private SubjectService subjectService;
    
    @Autowired
    private SubjectMapper subjectMapper;

    @Autowired
    private UtilsJwt utilsJwt;

    @PostMapping("/create-subject")
    @Secured({AuthConstants.MANAGE_SUBJECT})
    public SubjectDTO createSubject(@RequestBody SubjectDTO subjectDTO){
        Subject subject = subjectService.createSubject(subjectMapper.subject(subjectDTO));
        return subjectMapper.subjectDTO(subject);
    }

    @PutMapping("/update-subject")
    @Secured({AuthConstants.MANAGE_SUBJECT})
    public SubjectDTO updateSubject(@RequestBody SubjectDTO subjectDTO){
        Subject subject = subjectService.updateSubject(subjectMapper.subject(subjectDTO));
        return subjectMapper.subjectDTO(subject);
    }

    @GetMapping("/get-subjects")
    @Secured({AuthConstants.VIEW_SUBJECT})
    public List<SubjectDTO> getSubjects(@RequestHeader("authorization") String authHeader,
                                        @RequestParam(defaultValue = "10") int limit,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(required = false, defaultValue = "0") Long id,
                                        @RequestParam(required = false) String name) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        return subjectService.getSubjects(limit, page, id, name, userName).stream()
                .map(subject -> subjectMapper.subjectDTO(subject))
                .collect(Collectors.toList());
    }

    @GetMapping("/get-subject/{id}")
    @Secured({AuthConstants.VIEW_SUBJECT})
    public SubjectDTO getSubjects(@PathVariable Long id){
        return subjectMapper.subjectDTO(subjectService.findSubjectById(id));
    }

    @DeleteMapping("/delete-subject/{id}")
    @Secured({AuthConstants.MANAGE_SUBJECT})
    public void deleteSubject(@PathVariable Long id){
        subjectService.deleteSubject(id);
    }
}