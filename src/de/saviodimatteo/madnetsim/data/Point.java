package de.saviodimatteo.madnetsim.data;

public class Point {
  private double lat;
  private double lon;
  public boolean visited = false;
  
  public Point (double aLon, double aLat) {
    super();
    this.lat = aLat;
    this.lon = aLon;
  }
  
  public Point(Point aPoint) {
	 this(aPoint.getLon(), aPoint.getLat());
  }

public double getLat() {
	  return this.lat;
  }
  public double getLon() {
	  return this.lon;
  }
  
  public void setPosition(double lon, double lat) {
	  this.lat = lat;
	  this.lon = lon;
  }
  public boolean equals(Point p1, Point p2) {
	  if (p1.getLat() == p2.getLat() && p1.getLon() == p2.getLon())
		  return true;
	  else
		  return false;
  }
  public void setPosition(Point aPosition) {
	  setPosition(aPosition.getLon(), aPosition.getLat());
  }
  public void print() {
	  System.out.println("(" +lon+ "," +lat+ ")");
  }
  
}
