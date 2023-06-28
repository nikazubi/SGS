package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.ChangeRequestDTO;
import mthiebi.sgs.dto.ChangeRequestStatusChangeDTO;
import mthiebi.sgs.dto.GradeMapper;
import mthiebi.sgs.models.ChangeRequest;
import mthiebi.sgs.models.ChangeRequestStatus;
import mthiebi.sgs.service.ChangeRequestService;
import mthiebi.sgs.service.SystemUserService;
import mthiebi.sgs.utils.AuthConstants;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/change-request")
public class ChangeRequestController {

    @Autowired
    private ChangeRequestService changeRequestService;

    @Autowired
    private UtilsJwt utilsJwt;

    @Autowired
    private GradeMapper gradeMapper;

    @GetMapping("/get-change-requests")
//    @Secured({AuthConstants.EDIT_GRADES})
    public List<ChangeRequestDTO> getChangeRequests(@RequestHeader("authorization") String authHeader,
                                                    @RequestParam(required = false) Long classId,
                                                    @RequestParam(required = false) Long studentId,
                                                    @RequestParam(required = false) String date) throws Exception {

        String username = utilsJwt.getUsernameFromHeader(authHeader);
        Date date1 = new Date();;
        if (!date.equals("NaN")) {
            date1.setTime(Long.parseLong(date));
        }
        return changeRequestService.getChangeRequests(username, classId, studentId, date1).stream()
                .map(this::adjustDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/create-change-request")
//    @Secured({AuthConstants.ADD_OR_VIEW_GRADES})
    public ChangeRequestDTO createChangeRequest(@RequestHeader("authorization") String authHeader,
                                                @RequestBody ChangeRequestDTO changeRequestDTO) throws Exception {
        String username = utilsJwt.getUsernameFromHeader(authHeader);
        return adjustDTO(changeRequestService.createChangeRequest(
                adjust(changeRequestDTO), username));
    }

    @PutMapping("/change-request-status")
//    @Secured({AuthConstants.EDIT_GRADES})
    public void changeRequestStatus(@RequestBody ChangeRequestStatusChangeDTO changeRequestStatus){
        changeRequestService.changeRequestStatus(changeRequestStatus.getChangeRequestId(), changeRequestStatus.getChangeRequestStatus());
    }

    ChangeRequestDTO adjustDTO(ChangeRequest changeRequest){
        ChangeRequestDTO changeRequestDTO = new ChangeRequestDTO();
        changeRequestDTO.setId(changeRequest.getId());
        changeRequestDTO.setStatus(changeRequest.getStatus().toString());
        changeRequestDTO.setNewValue(changeRequest.getNewValue());
        changeRequestDTO.setDescription(changeRequest.getDescription());
        changeRequestDTO.setPrevValue(changeRequest.getPrevValue());
        changeRequestDTO.setPrevGrade(gradeMapper.gradeDTO(changeRequest.getPrevGrade()));
        changeRequestDTO.setIssuerFullname(changeRequest.getIssuer().getName());

        return changeRequestDTO;
    }

    ChangeRequest adjust(ChangeRequestDTO changeRequestDTO){
        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setId(changeRequestDTO.getId());
        changeRequest.setStatus(ChangeRequestStatus.PENDING);
        changeRequest.setDescription(changeRequestDTO.getDescription());
        changeRequest.setNewValue(changeRequestDTO.getNewValue());
        changeRequest.setPrevValue(changeRequestDTO.getPrevValue());
        changeRequest.setPrevGrade(gradeMapper.grade(changeRequestDTO.getPrevGrade()));

        return changeRequest;
    }

}

