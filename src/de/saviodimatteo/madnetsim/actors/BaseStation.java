package de.saviodimatteo.madnetsim.actors;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import de.saviodimatteo.madnetsim.MadnetSim;
import de.saviodimatteo.madnetsim.data.Content;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.data.Request;
import de.saviodimatteo.madnetsim.data.TimedPoint;
import de.saviodimatteo.madnetsim.data.Request.ERequestStatus;
import de.saviodimatteo.madnetsim.simulation.LockedList;
import de.saviodimatteo.madnetsim.simulation.Simulator;
import de.saviodimatteo.madnetsim.utils.Distance;

public class BaseStation extends Device {
	HashMap<Node,Request> iPendingContentRequests;
	LockedList<AccessPoint> iApList;
	float iMinApRadius = 0;
	String iId;
	float iHalfEdge;
	List<Node> iConnectedNodes;
	
	public BaseStation(String aName, float aLon, float aLat, float aRadius, LockedList<AccessPoint> aApList) {
		super(aLon, aLat);
		iConnectedNodes = new Vector<Node>();
		iPendingContentRequests = new HashMap<Node,Request>();
		iApList = aApList;
		iId = aName;
		iHalfEdge = aRadius;
		
		// Calculate the minimum link radius
		int apListSize = iApList.size();
		if (apListSize > 0) {
			iMinApRadius = iApList.get(0).getLinkRadius().fst;
			float currentMinLinkRadius;
			for (int aa=0; aa < apListSize; aa++) {
				currentMinLinkRadius = iApList.get(aa).getLinkRadius().fst;
				if ( iMinApRadius > currentMinLinkRadius)
					iMinApRadius = currentMinLinkRadius;
			}
		}
	}
	
	public float getCellRadius() {
		return iHalfEdge;
	}
	
	public void contentRequested(Node aSender, Content aContent, int aForeseenAt, int aPredictedAt, Point destPoint) {
		Request nodeRequest = new Request(aSender, aContent, aForeseenAt, aPredictedAt, destPoint);
		iPendingContentRequests.put(aSender,nodeRequest);
	}
	
	/**
	 * Instructions are issued for a node to know from what access point it should be connect
	 * to get parts of content.
	 * After such instructions are sent to the client, parts of content are delivered to 
	 * access points.
	 */
	public List<AccessPoint> getInstructions(Node aRequestor) {
		List<AccessPoint> instructions = new Vector<AccessPoint>();
		if (MadnetSim.config.getIsUploadCase()) {
			instructions = (List<AccessPoint>) Simulator.apList.clone();
		} else {
			Request lastReq = iPendingContentRequests.remove(aRequestor);
			if (lastReq == null) {
				System.out.println("Warning! the node request was not found in the base station!");
		    } else {
				Node requestor = (Node) lastReq.getRequestor();
				
				// Locate the points where the node will pass
				Vector<Point> nodePassingPoints = new Vector<Point>();
				int endPosTime = lastReq.getExpirationTime();
				for (int ii=Simulator.time; ii <= endPosTime; ii++) {
					Point p = requestor.positionAtT(ii);
					if (p != null) {
						nodePassingPoints.add(p);
					}
				}
				
				// Locate the access points who will serve the content
				for (Point p : nodePassingPoints) {
					for (AccessPoint ap : iApList.getElementsAround(p)) {
						Point apLocation = (Point) ap;
						float minApLinkRadius = ap.getLinkRadius().fst;
						float pdist = (float) Distance.distance(p, apLocation);
						if (pdist <  minApLinkRadius
								&& !instructions.contains(ap) 
							) 
						{
							instructions.add(ap);
						}
					}
				}
		    }
		}
		return instructions;
	}
	
	public boolean contains(Point aPoint) {
		boolean result = true;
		if ( aPoint.getLon() < ( this.getLon() - iHalfEdge) ||
			 aPoint.getLon() > ( this.getLon() + iHalfEdge) ||
			 aPoint.getLat() < ( this.getLat() - iHalfEdge) ||
			 aPoint.getLat() > ( this.getLat() + iHalfEdge)
		   )
			result = false;
		
		return result;
	}
	
	public boolean subscribeNode(Node aNode) {
		boolean result = false;
		if (iConnectedNodes.contains(aNode)) {
			System.out.println("Warning, the node " + aNode.getId() + " is already subscribed to " + iId);
		} else {
			result = iConnectedNodes.add(aNode);
			if (!result)
				System.out.println("Something went wrong while connecting " + aNode.getId() + " to " + iId);
		}
		return result;
	}
	public boolean unsubscribeNode(Node aNode) {
		boolean result = false;
		if (!iConnectedNodes.contains(aNode)) {
			// System.out.println("Warning, trying to unsubscribe " + aNode.getId() + " from " + iId + " *the node was not subscribed!* ");
		} else {
			result = iConnectedNodes.remove(aNode);
			if (!result)
				System.out.println("Something went wrong while disconnecting " + aNode.getId() + " from " + iId);
		}
		return result;
	}
	public int getConnectedNodes() {
		return iConnectedNodes.size();
	}
	
	public long getBandwidth(Node aNode) {
		
		/* Counts downloading users
		int numDownloadingNodes = 0;
		for (Node n : iConnectedNodes) {
			if ( n.getCurrentRequest().getRequestStatus() == ERequestStatus.REQ_TRANSFERRING)
				numDownloadingNodes++;
		} */
		long result;
		if (MadnetSim.config.getIsUploadCase())
			return 8192;
		else
			return 47360;
	}
}
