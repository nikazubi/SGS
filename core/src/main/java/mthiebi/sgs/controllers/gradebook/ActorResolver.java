package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.models.SystemUser;
import mthiebi.sgs.repository.SystemUserRepository;
import mthiebi.sgs.utils.UtilsJwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Who is making the request, for the audit columns.
 * <p>
 * The token has already been validated by the filter by the time this runs, so
 * failing to resolve the user is an attribution problem rather than an
 * authorization one - it must not cost a teacher their marks.
 */
@Component
public class ActorResolver {

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private UtilsJwt utilsJwt;

    public Long idOf(String authHeader) throws SGSException {
        try {
            SystemUser user = systemUserRepository
                    .findSystemUserByUsername(utilsJwt.getUsernameFromHeader(authHeader));
            return user == null ? null : user.getId();
        } catch (Exception e) {
            return null;
        }
    }
}
