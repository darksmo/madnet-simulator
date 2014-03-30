package de.saviodimatteo.madnetsim.data;

import java.util.HashMap;
import java.util.Vector;

public class PositionCache {
	private HashMap<Integer, Point> iCache;
	public PositionCache() {
		iCache = new HashMap<Integer, Point>();
	}
	public void put(int aTime, Point aPoint) {
		iCache.put(aTime, aPoint);
	}
	public Point lookup(int aTime) {
		Point result = iCache.get(aTime);
		return result;
	}
	public void cleanup(int aTillTime) {
		Vector<Integer> keysToRemove = new Vector<Integer>();
		for (Integer key : iCache.keySet()) {
			if (key < aTillTime)
				keysToRemove.add(key);
		}
		for (int key : keysToRemove) {
			iCache.remove(key);
		}
	}
}
