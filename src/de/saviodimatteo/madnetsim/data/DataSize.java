package de.saviodimatteo.madnetsim.data;

public class DataSize {
	
	private final int KBYTE = 1024;
	private final int MBYTE = 1048576;
	private final int GBYTE = 1073741824;
	
	private long iAmount; // Bytes
	
	public enum UNIT {
		B,
		KB,
		MB,
		GB
	}
	
	public DataSize() {
		setAmount(0);
	}
	public DataSize(int aAmount, UNIT aUnit) {
		setAmount(aAmount, aUnit);
	}
	
	public void addAmount(long aAmount) {
		setAmount(iAmount + aAmount, UNIT.B);
	}
	public void setAmount(long aAmount) {
		setAmount(aAmount,UNIT.B);
	}
	public void setAmount(long aAmount, UNIT aUnit) {
		switch (aUnit) {
		case B:
			this.iAmount = aAmount;
		case KB:
			this.iAmount = aAmount * KBYTE;
			break;
		case MB:
			this.iAmount = aAmount * MBYTE;
			break;
		case GB:
			this.iAmount = aAmount * GBYTE;
			break;
		}
	}
	
	public float getAmount(UNIT aUnit) {
		float result = -1F;
		switch (aUnit) {
		case B:
			result = (float) iAmount;
			break;
		case KB:
			result = iAmount / (float) KBYTE;
			break;
		case MB:
			result = iAmount / (float) MBYTE;
			break;
		case GB:
			result = iAmount / (float) GBYTE;
			break;
		}
		return result;
	}
	
}
