package de.saviodimatteo.madnetsim.data;

public class Poi extends Point {
	private long iSize;
	public Poi(double lon, double lat, long aSize) {
		super(lon, lat);
		iSize = aSize;
	}

	public Poi(Point aPoint, long aSize) {
		super(aPoint);
		iSize = aSize;
	}
	
	public long getSize() {
		return iSize;
	}
}
