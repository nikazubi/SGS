package mthiebi.sgs.dto;

import org.mapstruct.Builder;
import org.mapstruct.MapperConfig;

import static org.mapstruct.CollectionMappingStrategy.ADDER_PREFERRED;

@MapperConfig(builder = @Builder(disableBuilder = true), collectionMappingStrategy = ADDER_PREFERRED)
public interface ACMapperConfig {
}
