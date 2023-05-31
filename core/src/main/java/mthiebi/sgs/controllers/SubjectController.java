package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.SubjectDTO;
import mthiebi.sgs.dto.SubjectMapper;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/create-subject")
    public SubjectDTO createSubject(@RequestBody SubjectDTO subjectDTO){
        Subject subject = subjectService.createSubject(subjectMapper.subject(subjectDTO));
        return subjectMapper.subjectDTO(subject);
    }

    @PutMapping("/update-subject")
    public SubjectDTO updateSubject(@RequestBody SubjectDTO subjectDTO){
        Subject subject = subjectService.updateSubject(subjectMapper.subject(subjectDTO));
        return subjectMapper.subjectDTO(subject);
    }

    @GetMapping("/get-subjects")
    public List<SubjectDTO> getSubjects(@RequestParam(defaultValue = "10") int limit,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(required = false) Long id,
                                        @RequestParam(required = false) String name){
        return subjectService.getSubjects(limit, page, id, name).stream()
                .map(subject -> subjectMapper.subjectDTO(subject))
                .collect(Collectors.toList());
    }

    @GetMapping("/get-subject/{id}")
    public SubjectDTO getSubjects(@PathVariable Long id){
        return subjectMapper.subjectDTO(subjectService.findSubjectById(id));
    }

    @DeleteMapping("/delete-subject/{id}")
    public void deleteSubject(@PathVariable Long id){
        subjectService.deleteSubject(id);
    }
    
}
