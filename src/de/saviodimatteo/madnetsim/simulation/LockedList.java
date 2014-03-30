package de.saviodimatteo.madnetsim.simulation;

import java.util.List;
import java.util.Vector;

import de.saviodimatteo.madnetsim.actors.AccessPoint;
import de.saviodimatteo.madnetsim.data.Point;

public class LockedList<T> extends Vector<T> {
	private static final long serialVersionUID = 1L;

	boolean locked;
	double minX,maxX,minY,maxY;
	Vector<T>[][] rapidPick;
	int rapidPickXSize;
	int rapidPickYSize;
	
	LockedList() {
		super();
		minX = Double.MAX_VALUE;
		minY = Double.MAX_VALUE;
		maxX = Double.MIN_VALUE;
		minX = Double.MIN_VALUE;
		locked = false;
	}
	public boolean add(T aElement) {
		if (!locked) {
			boolean result = super.add(aElement);
			double apX = ((Point)aElement).getLon();
			double apY = ((Point)aElement).getLat();
			if (apX < minX) minX = apX;
			if (apX > maxX) maxX = apX;
			if (apY < minY) minY = apY;
			if (apY > maxY) maxY = apY;
			return result;
		}
		else {
			System.out.println("ERROR! Adding access point on a locked list!");
			return false;
		}
	}
	
	public void lock() {
		if (locked) {
			System.out.println("ERROR! The list was already locked");
		} else {
			rapidPickXSize = ((int) (maxX - minX)) + 1;
			rapidPickYSize = ((int) (maxY - minY)) + 1;
			if (rapidPickYSize <= 0) 
				rapidPickYSize = 1;
			if (rapidPickXSize <= 0 )
				rapidPickYSize = 1;
			rapidPick = new Vector[rapidPickXSize][rapidPickYSize];
			for (int xx=0; xx<rapidPickXSize; xx++) 
				for(int yy=0; yy<rapidPickYSize; yy++)
					rapidPick[xx][yy] = new Vector<T>();
			for (T el : this) {
				double apX = ((Point)el).getLon();
				double apY = ((Point)el).getLat();
				rapidPick[(int)(apX - minX)][(int)(apY - minY)].add(el);
			}
			locked = true;
		}
	}
	
	public T get(int index) {
		return super.get(index);
	}
	
	public List<T> getElementsAround(Point aPoint) {
		double px = aPoint.getLon();
		double py = aPoint.getLat();
		int arrayPx = (int) (px - minX);
		int arrayPy = (int) (py - minY);
		try {
			if (arrayPx < 0 || arrayPy < 0 || arrayPx >= rapidPickXSize || arrayPy >= rapidPickYSize)
				return new Vector<T>();
			else
				return rapidPick[arrayPx][arrayPy];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public List<T> getIncreasedElementsAround(Point aPoint, float radius) {
		double px = aPoint.getLon();
		double py = aPoint.getLat();
		int arrayPx = (int) (px - minX);
		int arrayPy = (int) (py - minY);
		try {
			if (arrayPx < 0 || arrayPy < 0 || arrayPx >= rapidPickXSize || arrayPy >= rapidPickYSize)
				return new Vector<T>();
			else {
				List<T> reslist = getElementsAround(aPoint);
				for (T el : (getElementsAround(new Point(aPoint.getLon() - radius, aPoint.getLat() - radius))))
					if (!reslist.contains(el)) reslist.add(el);
				for (T el : (getElementsAround(new Point(aPoint.getLon() - radius, aPoint.getLat() + radius))))
					if (!reslist.contains(el)) reslist.add(el);
				for (T el : (getElementsAround(new Point(aPoint.getLon() + radius, aPoint.getLat() - radius))))
					if (!reslist.contains(el)) reslist.add(el);
				for (T el : (getElementsAround(new Point(aPoint.getLon() + radius, aPoint.getLat() + radius))))
					if (!reslist.contains(el)) reslist.add(el);
				for (T el : (getElementsAround(new Point(aPoint.getLon(), aPoint.getLat() - radius))))
					if (!reslist.contains(el)) reslist.add(el);
				for (T el : (getElementsAround(new Point(aPoint.getLon(), aPoint.getLat() + radius))))
					if (!reslist.contains(el)) reslist.add(el);
				for (T el : (getElementsAround(new Point(aPoint.getLon() + radius, aPoint.getLat()))))
					if (!reslist.contains(el)) reslist.add(el);
				for (T el : (getElementsAround(new Point(aPoint.getLon() - radius, aPoint.getLat()))))
					if (!reslist.contains(el)) reslist.add(el);
				return reslist;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
