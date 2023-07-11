package mthiebi.sgs.handler;

import mthiebi.sgs.SGSException;
import org.springframework.validation.FieldError;

import java.util.function.Supplier;

public class ExceptionUtils {

	private static final String EXCEPTION_KEY_PREFIX = "exceptionKey-";
	
	private static final String VALIDATION_EXCEPTION_KEY_PREFIX = EXCEPTION_KEY_PREFIX + "dV-";
	
	public static final String VALIDATION_EXCEPTION_DEFAULT_MESSAGE_ARGUMENTS_DELIMITER = "::";
	
	public static final String FIELD_PREFIX = "field-";
	
	public static final String UNEXPECTED_EXCEPTION_MESSAGE = "unexpectedError";
	
	public static final String CUSTOM_VALIDATION_FIELD = "customValidation";
	
	public static void throwLcmsException(Supplier<SGSException> exceptionSupplier) throws SGSException {
		throw exceptionSupplier.get();
	}
	
	public static String getMessageKeyForExceptionKey(String exceptionKey) {
		return EXCEPTION_KEY_PREFIX + exceptionKey;
	}
	
	public static String getExceptionKeyForValidationExceptionMessage(String exceptionKey) {
		return VALIDATION_EXCEPTION_KEY_PREFIX + exceptionKey;
	}

	public static String getExceptionKeyFromValidationExceptionDefaultMessage(String defaultMessage) {
		return defaultMessage.split(VALIDATION_EXCEPTION_DEFAULT_MESSAGE_ARGUMENTS_DELIMITER)[0];
	}

	public static String getUnexpectedExceptionMessage() {
		return UNEXPECTED_EXCEPTION_MESSAGE;
	}

	public static String getCustomValidationField() {
		return CUSTOM_VALIDATION_FIELD;
	}

	public static String getArgumentsFromValidationExceptionDefaultMessage(String defaultMessage) {
		if (defaultMessageHasArguments(defaultMessage)) {
			return defaultMessage.split(VALIDATION_EXCEPTION_DEFAULT_MESSAGE_ARGUMENTS_DELIMITER)[1];
		}
		return null;
	}

	// FOR CUSTOM VALIDATION WITH @AssertTrue or @AssertFalse ANNOTATIONS VALIDATION METHOD IS TREATED AS GETTER OF BOOLEAN FIELD,
	// SO IF REJECTED VALUE IS FALSE OR TRUE, THEREFORE IT IS A CUSTOM VALIDATION
	// THIS CHECK WILL RETURN WRONG VALUE IF @AssertTrue or @AssertFalse WILL BE
	// USED FOR VALIDATING ACTUAL BOOLEAN FIELDS (VALUE SHOULD ALWAYS BE TRUE/FALSE) WHICH SEEMS UNREASONABLE AT THIS MOMENT
	public static boolean isCustomValidationError(FieldError fieldError) {
		return Boolean.TRUE.equals(fieldError.getRejectedValue()) || Boolean.FALSE.equals(fieldError.getRejectedValue());
	}

	public static String getMessageKeyForField(String field) {
		return FIELD_PREFIX + field;
	}

	private static boolean defaultMessageHasArguments(String defaultMessage) {
		return defaultMessage.split(VALIDATION_EXCEPTION_DEFAULT_MESSAGE_ARGUMENTS_DELIMITER).length > 1;
	}
}
