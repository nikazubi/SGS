package mthiebi.sgs;

import java.util.function.Supplier;

public class SGSException extends Exception {

	private final SGSExceptionCode exceptionCode;

	private final String field;

	public SGSException(SGSExceptionCode exceptionCode) {
		this(exceptionCode, exceptionCode.name());
	}

	public SGSException(String message) {
		this(SGSExceptionCode.INTERNAL_SEVER_ERROR, message);
	}

	public SGSException(SGSExceptionCode exceptionCode, String message) {
		super(message);
		this.exceptionCode = exceptionCode;
		this.field = null;
	}

	public SGSException(String message, String field) {
		super(message);
		this.exceptionCode = SGSExceptionCode.BAD_REQUEST;
		this.field = field;
	}

	public static SGSException badRequest(String errorMessage) {
		return new SGSException(SGSExceptionCode.BAD_REQUEST, errorMessage);
	}

	public SGSExceptionCode getExceptionCode() {
		return exceptionCode;
	}

	public String getField() {
		return field;
	}


	public void throwSgsException(Supplier<SGSException> exceptionSupplier) throws SGSException {
		throw exceptionSupplier.get();
	}
}
