package de.saviodimatteo.madnetsim.actors;

import java.util.HashMap;

import de.saviodimatteo.madnetsim.data.Request;
import de.saviodimatteo.madnetsim.simulation.Simulator;

public class NodeCallbackReceiver {
	
	private int iSatisfiedRequestsCount;
	private int iRequestedIssuedCount;
	private int iDataNotAvailableInAp;
	private int iCutoutsCount;
	private int iApNoMoreInRange;
	private int iDataIgnored;
	private long iWifiDataReceived;
	private long i3GDataReceived;
	private HashMap<Integer,Integer> iDelaysHistogram;

	public NodeCallbackReceiver() {
		iSatisfiedRequestsCount = 0;
		iRequestedIssuedCount = 0;
		iCutoutsCount = 0;
		iDataNotAvailableInAp = 0;
		iApNoMoreInRange = 0;
		iDataIgnored = 0;
		iWifiDataReceived = 0;
		i3GDataReceived = 0;
		iDelaysHistogram = new HashMap<Integer,Integer>();
	}
	public long getNum3GDataReceived() {
		return i3GDataReceived;
	}

	public long getNumWifiDataReceived() {
		return iWifiDataReceived;
	}
	public int getSatisfiedRequestsCount() {
		return iSatisfiedRequestsCount;
	}

	public int getRequestedIssuedCount() {
		return iRequestedIssuedCount;
	}

	public int getDataNotAvailableInApCount() {
		return iDataNotAvailableInAp;
	}

	public int getCutoutsCount() {
		return iCutoutsCount;
	}

	public int getApNoMoreInRangeCount() {
		return iApNoMoreInRange;
	}

	public int getDataIgnoredCount() {
		return iDataIgnored;
	}

	public void cbRequestCommitted() {
		iRequestedIssuedCount++;
	}

	public void cbRequestSatisfied(Request aRequest) {
		iSatisfiedRequestsCount++;
		// Update Delays Histogram
		int startTime = aRequest.getPreparedAt();
		int endTime = aRequest.getCompletedAt();
		int delay = endTime - startTime;
		if (iDelaysHistogram.containsKey(delay))
			iDelaysHistogram.put(delay, iDelaysHistogram.get(delay) + 1);
		else
			iDelaysHistogram.put(delay, 1);
	}
	public HashMap<Integer,Integer> getDelaysHistogram() {
		return iDelaysHistogram;
	}
	public void cbNodeCutout() {
		iCutoutsCount++;
	}
	
	public void cbDataNotAvailable() {
		iDataNotAvailableInAp++;
	}
	
	public void cbApNoMoreInRange() {
		iApNoMoreInRange++;
	}
	
	public void cbDataIgnored() {
		iDataIgnored++;
	}
	public void cbWifiDataReceived(long aNumBytes) {
		iWifiDataReceived+=aNumBytes;
	}
	public void cb3GDataReceived(long aNumBytes) {
		i3GDataReceived+=aNumBytes;
	}


}
