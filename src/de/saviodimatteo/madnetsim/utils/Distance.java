package de.saviodimatteo.madnetsim.utils;

import de.saviodimatteo.madnetsim.data.Point;

public class Distance {
	
	public static double distance(double y1, double x1, double y2, double x2) {
		return cartesianDistance( y1, x1, y2, x2);
	}
	
	public static double distance(Point p1, Point p2) {
		return distance(p1.getLat(),p1.getLon(),p2.getLat(),p2.getLon());
	}

	public static double cartesianDistance(double y1, double x1, double y2, double x2) {
		double xd = x2-x1;
		double yd = y2-y1;
		return Math.sqrt(xd*xd + yd*yd);
	}
	
		/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
		/*::  This function converts decimal degrees to radians             :*/
		/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
		private static double deg2rad(double deg) {
		  return (deg * Math.PI / 180.0);
		}

		/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
		/*::  This function converts radians to decimal degrees             :*/
		/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
		private static double rad2deg(double rad) {
		  return (rad * 180.0 / Math.PI);
		}
		public static float cartesianDistance(Point p1, Point p2) {
			return (float) cartesianDistance(p1.getLat(),p1.getLon(),p2.getLat(),p2.getLon());
		}
	
}
