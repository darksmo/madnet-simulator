package de.saviodimatteo.madnetsim.exceptions;

public class ContentUnavailableException extends AugmentedException {
	private static final long serialVersionUID = 1L;

	public ContentUnavailableException(String description, Object value) {
		super(description, value);
	}
}
