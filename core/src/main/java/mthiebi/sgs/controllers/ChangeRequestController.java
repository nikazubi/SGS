package mthiebi.sgs.controllers;

import mthiebi.sgs.dto.ChangeRequestDTO;
import mthiebi.sgs.dto.ChangeRequestMapper;
import mthiebi.sgs.dto.ChangeRequestStatusChangeDTO;
import mthiebi.sgs.models.ChangeRequest;
import mthiebi.sgs.models.ChangeRequestStatus;
import mthiebi.sgs.service.ChangeRequestService;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    private ChangeRequestMapper changeRequestMapper;

    @GetMapping("/get-change-requests")
    public List<ChangeRequestDTO> getChangeRequests(@RequestHeader("Authorization") String authHeader) throws Exception {

        String username = utilsJwt.getUsernameFromHeader(authHeader);
        return changeRequestService.getChangeRequests(username).stream()
                .map(o -> changeRequestMapper.changeRequestDto(o))
                .collect(Collectors.toList());
    }

    @PostMapping("/create-change-request")
    public ChangeRequestDTO createChangeRequest(@RequestHeader("Authorization") String authHeader,
                                                @RequestBody ChangeRequestDTO changeRequestDTO) throws Exception {
        String username = utilsJwt.getUsernameFromHeader(authHeader);
        return changeRequestMapper.changeRequestDto(changeRequestService.createChangeRequest(
                changeRequestMapper.changeRequest(changeRequestDTO), username));
    }

    @PutMapping("/change-request-status")
    public void changeRequestStatus(@RequestBody ChangeRequestStatusChangeDTO changeRequestStatus){
        changeRequestService.changeRequestStatus(changeRequestStatus.getChangeRequestId(), changeRequestStatus.getChangeRequestStatus());
    }
}

