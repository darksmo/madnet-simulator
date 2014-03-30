package de.saviodimatteo.madnetsim.exceptions;
 
public class AugmentedException extends Exception {
	private static final long serialVersionUID = 1L;
	private String description;
	private Object value;
	AugmentedException(String aDescription, Object aValue) {
		super();
		this.setDescription(aDescription);
		this.setValue(aValue);
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public Object getValue() {
		return value;
	}
}
