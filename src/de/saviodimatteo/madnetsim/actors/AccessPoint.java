package de.saviodimatteo.madnetsim.actors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.saviodimatteo.madnetsim.MadnetSim;
import de.saviodimatteo.madnetsim.config.ConfigReader;
import de.saviodimatteo.madnetsim.data.Content;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.simulation.Simulator;
import de.saviodimatteo.madnetsim.utils.Pair;


public class AccessPoint extends Device {
	private static final float KToMeters = 2000;
	long iMaximumBandwidth; // bytes per second
	public Map<Node, Long> iConnectedNodes;
	public List<Content> iCachedContents;
	private String iId;
	public AccessPoint(float lon, float lat) {
		super(lon, lat);
		iMaximumBandwidth = MadnetSim.config.getApMaximumBandwidth();
		iCachedContents = new Vector<Content>();
		iId = String.valueOf(Simulator.apList.size());
		iConnectedNodes = new HashMap<Node,Long>();
	}
	public void connectNode(Node aNode) {
		if (aNode != null) {
			if (iConnectedNodes.containsKey(aNode) ) {
					if (!MadnetSim.config.getIsUploadCase()) {
						System.out.println("Warning!!, at T=" + Simulator.time + " node " + aNode.iId + " is contained in AP #" + iId);
					}
			}
			else {
				iConnectedNodes.put(aNode,0L);
			}
		}
	}
	
	/* 
	 * Disconnection happens for a node when:
	 * - the access point is out of range
	 * - the node just completed its request
	 * - after the LAST data transfer
	 * - each time a disconnect (EXPIRATION) event occurs
	 */
	public void disconnectNode(Node aNode) {
		if (aNode != null) {
			if (null == iConnectedNodes.remove(aNode)) {
					System.out.println("!! Warning, at T=" + Simulator.time + " Node " + aNode.iId + " was not disconnected from AP #" + iId);
			}
		}
	}
	public boolean isConnected(Node aNode) {
		return iConnectedNodes.containsKey(aNode);
	}
	public long getBandwidth() {
		return iMaximumBandwidth;
	}
	
	public long requestBandwidth(float aNodeDistance, Node aConnectedNode) {
		long resultingBw = -1;
		
		if (iConnectedNodes.containsKey(aConnectedNode)) {
			ConfigReader config = MadnetSim.config;
			
			// Compute the bandwidth according to the node distance
			Vector<Point>[] f = config.f;
			float meters = aNodeDistance * KToMeters;
			int lookVector = (int) meters;
			float xToFit = (float) aNodeDistance;
			float x;
			float y = -1;
			if (lookVector >= f.length) {
				lookVector = f.length - 1;
			}
			for (int ii = 0; ii < f[lookVector].size(); ii++) {
				Point fPoint = f[lookVector].get(ii);
				x = (float) fPoint.getLon();
				y = (float) fPoint.getLat();
				if ( x >= xToFit) {
					break;
				}
			}
			if (y < 0) // should NEVER happen
				System.out.println("WARNING! A node requested a some bandwidth outside the functions capabilities");
			else {
				// Let's check if that bandwidth is available...
				long availableBw = getAvailableBandwidth(aConnectedNode);
				if (y <= availableBw) {
					resultingBw = (long) y;
					// ... and register that bandwidth for the node
					iConnectedNodes.put(aConnectedNode, resultingBw);
				} else if (isConnected(aConnectedNode) 
						&& iConnectedNodes.get(aConnectedNode) > 0L
						&& availableBw > 0) {
						// If the Node is connected and transferring, assign the available bandwidth
						iConnectedNodes.put(aConnectedNode, availableBw);
						resultingBw = availableBw;
				} else {
					// Node cutout (will return -1)
					//System.out.println("--CUTOUT-- Requested:" + y + " Available:" + availableBw);
				}
			}
		} else { // Requesting node is not connected! should never happen...
			System.out.println("WARNING!! The node " + aConnectedNode + " is requesting data but it's not connected to the AP #" + iId);
		}
		
		System.out.println("Result Bw:" + resultingBw);
		
		return resultingBw;
	}
	
	public long getAvailableBandwidth() {
		return getAvailableBandwidth(null);
	}
	/**
	 * Calculates the available bandwidth in the access point for a given node.
	 * During the search, the specified node will be ignored if it is among the 
	 * connected ones. This happens because a request of an existing node should
	 * use the same allocated bandwidth.
	 * @param aReceivingNode the node that is requesting for bandwidth.
	 * @return the available bandwidth for the requesting node.
	 */
	public long getAvailableBandwidth(Node aReceivingNode) {
		long availableBandwidth = iMaximumBandwidth;
		for (Node n : iConnectedNodes.keySet()) {
			if (n != aReceivingNode) {
				long val = iConnectedNodes.get(n);
				availableBandwidth-=val;
			}
		}
		return availableBandwidth;
	}
	public void contentTransferred(Content aContent) {
		if (aContent != null) {
			iCachedContents.add(aContent);
		}
	}
	public boolean hasContent(Content aContent) {
		boolean result = false;
		if (MadnetSim.config.getIsUploadCase()) {
			result = true;
		} else {
			for (Content c : iCachedContents) {
				String contentId = c.getId();
				String requestedContentId = aContent.getId();
				if (contentId.equals(requestedContentId)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}
	public String getId() {
		return iId;
	}
}
