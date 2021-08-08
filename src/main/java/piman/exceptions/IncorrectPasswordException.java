package piman.exceptions;

public class IncorrectPasswordException extends InvalidAccessException {

	public IncorrectPasswordException() {
		super("Invalid Password");
	}
	
}
