package mthiebi.sgs.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorInfo {

	private String message;

	private String field;

	private Map<String, String> arguments;
}
