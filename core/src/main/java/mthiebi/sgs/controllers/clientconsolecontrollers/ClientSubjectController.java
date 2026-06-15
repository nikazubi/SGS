package mthiebi.sgs.controllers.clientconsolecontrollers;
import lombok.RequiredArgsConstructor;

import mthiebi.sgs.dto.SubjectDTO;
import mthiebi.sgs.dto.SubjectMapper;
import mthiebi.sgs.service.SubjectService;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/client/subjects")
@RequiredArgsConstructor
public class ClientSubjectController {

    private final SubjectService subjectService;

    private final SubjectMapper subjectMapper;

    private final UtilsJwt utilsJwt;

    @GetMapping("/get-subjects-for-student")
    public List<SubjectDTO> getSubjects(@RequestHeader("authorization") String authHeader) throws Exception {

        String userName = utilsJwt.getUsernameFromHeader(authHeader);
        return subjectService.getSubjectsForStudent(userName).stream()
                .map(subject -> subjectMapper.subjectDTO(subject))
                .collect(Collectors.toList());
    }
}
