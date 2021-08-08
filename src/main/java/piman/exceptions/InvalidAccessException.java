package piman.exceptions;

public class InvalidAccessException extends Exception {
	
	public InvalidAccessException(String message) {
		super("Access Denied: " + message);
	}
	
}
