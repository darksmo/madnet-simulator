package de.saviodimatteo.madnetsim.data;

import java.util.Random;

public class Content {
	private String iId;
	private long iSize; //in bytes

	public Content(long aSize) {
		this(aSize, String.valueOf(new Random(aSize).nextInt()));
	}
	public Content(long aSize, String aId) {
		iSize = aSize;
		iId = aId;
	}
	
	public String getId() {
		return iId;
	}
	public long getSize() {
		return iSize;
	}
	

}
