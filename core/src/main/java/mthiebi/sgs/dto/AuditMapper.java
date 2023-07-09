package mthiebi.sgs.dto;

import mthiebi.sgs.models.Audit;
import org.mapstruct.Mapper;

@Mapper(config = ACMapperConfig.class)
public interface AuditMapper {

    AuditDTO auditDTO(Audit audit);

    Audit audit(AuditDTO auditDTO);

}
