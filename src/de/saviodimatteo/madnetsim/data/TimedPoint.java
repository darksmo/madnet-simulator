package de.saviodimatteo.madnetsim.data;

public class TimedPoint extends Point{
	int iTime;
	public TimedPoint(Point aPoint, int aTime) {
		this(aPoint.getLon(),aPoint.getLat(),aTime);
	}
	public TimedPoint(double aLon, double aLat, int aTime) {
		super(aLon, aLat);
		iTime = aTime;
	}
	public TimedPoint(TimedPoint aTimedPoint) {
		this((Point) aTimedPoint, aTimedPoint.getTime());
	}
	public int getTime() {
		return iTime;
	}
	public void setTime (int aTime) {
		iTime = aTime;
	}
	public void print() {
		System.out.println("(lon,lat) = (" + getLon() + "," + getLat() + ") TIME: " + iTime);
	}
}
