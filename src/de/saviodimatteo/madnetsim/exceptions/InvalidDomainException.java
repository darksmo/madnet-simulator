package de.saviodimatteo.madnetsim.exceptions;

public class InvalidDomainException extends AugmentedException {
	private static final long serialVersionUID = 1L;
	public InvalidDomainException(Object aValue) {
		super("The specified value is out of the domain range",aValue);
	}
}
