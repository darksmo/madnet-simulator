package de.saviodimatteo.madnetsim.simulation;

import java.util.List;
import java.util.Vector;

import de.saviodimatteo.madnetsim.actors.AccessPoint;
import de.saviodimatteo.madnetsim.actors.Node;
import de.saviodimatteo.madnetsim.data.Content;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.utils.Pair;

public class EventQueue{
	Vector<Event> iEventList;
	
	EventQueue() {
		iEventList = new Vector<Event>();
	}
	
	void add(Event aEvent) {
		add(aEvent,false); // Add always as late as possible: elements are always queued
	}
	void add(Event aEvent, boolean firstEvent) {
		int eventListSize = iEventList.size();
		int examinedElementPos = 0;
		boolean gotDataEvent = ( aEvent.getKind() == EEventKind.NODE_GOTDATA ? true : false );
		if (firstEvent) {
			while (examinedElementPos < eventListSize && iEventList.get(examinedElementPos).getTime() < aEvent.getTime()) {
				examinedElementPos++;
			}
		} else {
			while (examinedElementPos < eventListSize && iEventList.get(examinedElementPos).getTime() <= aEvent.getTime()) {
				if (gotDataEvent) {
					Event evt = iEventList.get(examinedElementPos);
					if (evt.getNode() == aEvent.getNode() && 
						evt.getKind() == EEventKind.NODE_REQ_EXPIRED) {
						if ( !(((Pair<Node,Object>)aEvent.getEventData()).snd instanceof List)) 
							System.out.println("T:" + Simulator.time + " Node: " + evt.getNode().getId() + " GOTDATA after DISCONNECTION! distance " + (aEvent.getTime() - evt.getTime()));
						else
							System.out.println("Was An AP LIST (Will be ignored)");
					}
				}
				examinedElementPos++;
			}
		}
		iEventList.add(examinedElementPos,aEvent);
	}

	public int getHeadTime() {
		if (iEventList.size() > 0)
			return iEventList.get(0).getTime();
		else
			return -1;
	}
	public Event pop() {
		if (iEventList.size() > 0) {
			Event evt = iEventList.remove(0);
			return evt;
		}
		return null;
	}
	
	public void print() {
		for (int k=0; k < iEventList.size(); k++) {
			Event e = iEventList.get(k);
			e.print();
			System.out.println();
		}
	}

	public int size() {
		return iEventList.size();
	}
	public Event get(int aIndex) {
		return iEventList.get(aIndex); 
	}
	
	public boolean isNodeGotDataArleadyQueued(Node aNode) {
		boolean exists = false;
	    int eventListSize = this.size();
	    for (int ee=0; ee<eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.NODE_GOTDATA) {      // consider NODE_GOTDATA events only
		    	Pair<Node,Object> nodeEventParams = ((Pair<Node, Object>) nodeEvent.getEventData());
		    	Node node = nodeEventParams.fst;
		    	Object second = nodeEventParams.snd;
		    	if ( node == aNode &&                                  // related to that node 
		    		second instanceof Pair &&                          // containing the data
		    		(((Pair<Object,Long>) second).fst instanceof AccessPoint)
		    	)
		    	{
		    		exists = true;
		    		break;
		    	}
	    	}
	    }
	    return exists;
	}
	public Event getNextNodeMoveEvent(Node aNode) {
		Event result = null;
		int eventListSize = this.size();
	    for (int ee=0; ee<eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.NODE_MOVE) {      // consider NODE_GOTDATA events only
	    		
	    		Node node = null;
		    	// Extract node from event data
		    	Object eventData = nodeEvent.getEventData();
		    	if (eventData instanceof Node) {
			    	node = (Node) eventData;
		    	} else if (eventData instanceof Pair) {
		    		Pair <Node, Point> nodePosition = (Pair<Node,Point>) eventData;
		    		node = nodePosition.fst;
		    	}
	    		if (node != null && node == aNode) {
	    			result = nodeEvent;
		    		break;
	    		}
	    	}
	    }
	    return result;
	}
	public boolean isNodeMoveArleadyQueued(Node aNode) {
		boolean exists = false;
	    int eventListSize = this.size();
	    for (int ee=0; ee<eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.NODE_MOVE) {      // consider NODE_GOTDATA events only
		    	Node node = null;
		    	
		    	// Extract node from event data
		    	Object eventData = nodeEvent.getEventData();
		    	if (eventData instanceof Node) {
			    	node = (Node) eventData;
		    	} else if (eventData instanceof Pair) {
		    		Pair <Node, Point> nodePosition = (Pair<Node,Point>) eventData;
		    		node = nodePosition.fst;
		    	}
		    	
		    	if ( node != null && node == aNode )                                  // related to that node 
		    	{
		    		exists = true;
		    		break;
		    	}
	    	}
	    }
	    return exists;
	}
	public boolean isNodeConnectedArleadyQueued(Node aNode) {
		boolean exists = false;
	    int eventListSize = this.size();
	    for (int ee=0; ee<eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.NODE_CONNECTED) {      // consider NODE_GOTDATA events only
		    	Pair<Node,AccessPoint> nodeAp = (Pair<Node,AccessPoint>) nodeEvent.getEventData();
		    	Node node = nodeAp.fst;
		    	if ( node == aNode )                                  // related to that node 
		    	{
		    		exists = true;
		    		break;
		    	}
	    	}
	    }
	    return exists;
	}

	public boolean isApGotDataScheduled(AccessPoint aAccessPoint, Content aApContent) {
		boolean exists = false;
	    int eventListSize = this.size();
	    for (int ee=0; ee<eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.AP_GOTDATA) {      // consider AP_GOTDATA events only
		    	Pair<AccessPoint,Content> apContent = (Pair<AccessPoint,Content>) nodeEvent.getEventData();
		    	if ( apContent.snd.getId().equals(aApContent.getId()) && apContent.fst == aAccessPoint) 
		    	{
		    		exists = true;
		    		break;
		    	}
	    	}
	    }
	    return exists;
	}

	public int getNextConnectTime() {
		int result = -1;
		int eventListSize = this.size();
	    for (int ee=0; ee<eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.NODE_CONNECTED) {      // consider NODE_GOTDATA events only
		    	result = nodeEvent.getTime();
	    		break;
	    	}
	    }
	    return result;
	}

	public boolean isNodeRequestExpiryAreadyQueued(Node node) {
		boolean exists = false;
	    int eventListSize = this.size();
	    for (int ee=0; ee < eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.NODE_REQ_EXPIRED) {      // consider AP_GOTDATA events only
		    	if ( nodeEvent.getNode() == node ) 
		    	{
		    		exists = true;
		    		break;
		    	}
	    	}
	    }
	    return exists;
	}
	
	public boolean isNodeRequestAreadyQueued(Node node) {
		boolean exists = false;
	    int eventListSize = this.size();
	    for (int ee=0; ee < eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.NODE_REQ) {      // consider AP_GOTDATA events only
		    	if ( nodeEvent.getNode() == node ) 
		    	{
		    		exists = true;
		    		break;
		    	}
	    	}
	    }
	    return exists;
	}
	
	public Event getNextRequestExpiry(Node node) {
		Event result = null;
	    int eventListSize = this.size();
	    for (int ee=0; ee < eventListSize; ee++) {
	    	Event nodeEvent = this.get(ee);
	    	if (nodeEvent.getKind() == EEventKind.NODE_REQ_EXPIRED) {
		    	if ( nodeEvent.getNode() == node ) 
		    	{
		    		result = nodeEvent;
		    		break;
		    	}
	    	}
	    }
	    return result;
	}
}
