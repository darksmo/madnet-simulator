package de.saviodimatteo.madnetsim.simulation;

import de.saviodimatteo.madnetsim.actors.Node;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.utils.Pair;


public class Event {
	private int iTime;
	private EEventKind iKind;
	private Object iEventData;
	public Event(EEventKind aKind, int aTime, Object aEventData) {
		iTime = aTime;
		setKind(aKind);
		setEventData(aEventData);
	}
	public int getTime() {
		return iTime;
	}
	public void setKind(EEventKind iKind) {
		this.iKind = iKind;
	}
	public EEventKind getKind() {
		return iKind;
	}
	public void setEventData(Object iEventData) {
		this.iEventData = iEventData;
	}
	public Object getEventData() {
		return iEventData;
	}
	public Node getNode() {
		Node result = null;
		Object ed = this.getEventData();
		if ( iKind == EEventKind.NODE_MOVE ) {  // Schedules the next move || NODE_GOTDATA
			if (ed instanceof Node) {
				result = (Node) ed;
			} else {
				result = ((Pair<Node,Point>) ed).fst;
			}
		} else if (iKind == EEventKind.NODE_REQ) { // Schedules req received
			result = (Node) ed;
		} else if (iKind == EEventKind.NODE_REQ_RECEIVED) { // Schedules AP_GOTDATA || NODE_CONNECTED
			result = (Node) ((Object[]) ed)[2];
		} else if (iKind == EEventKind.NODE_CONNECTED) { // Schedules NODE_GOTDATA
			result = ((Pair<Node,Object>) this.getEventData()).fst;
		} else if (iKind == EEventKind.NODE_GOTDATA) {
			result = ((Pair<Node,Object>) this.getEventData()).fst;
		} else if (iKind == EEventKind.NODE_REQ_EXPIRED) {
			result = (Node) ed;
		}
		return result;
	}
	public void print() {
		String symbol = "Unk";
		if ( iKind == EEventKind.NODE_MOVE ) {  // Schedules the next move || NODE_GOTDATA
			Object ed = this.getEventData();
			if (ed instanceof Node) {
				Node n = (Node) ed;
				symbol = "MV (" + n.getId() + ")";
			} else {
				symbol = "dMV";
			}
		} else if (iKind == EEventKind.NODE_REQ) { // Schedules req received
			symbol = "RQ";
		} else if (iKind == EEventKind.NODE_REQ_RECEIVED) { // Schedules AP_GOTDATA || NODE_CONNECTED
			symbol = "RR";
		} else if (iKind == EEventKind.NODE_CONNECTED) { // Schedules NODE_GOTDATA
			symbol = "CO";
		} else if (iKind == EEventKind.NODE_GOTDATA) {
			Node n = ((Pair<Node,Object>) this.getEventData()).fst;
			symbol = "ND (" + n.getId() + ")";
		} else if (iKind == EEventKind.AP_GOTDATA) {
			symbol = "AD";
		} else if (iKind == EEventKind.NODE_REQ_EXPIRED) {
			symbol = "RX";
		}
		System.out.print("# " + iTime + "|" + symbol + " ");
	}
	public void println() {
		print();
		System.out.println("\n");
	}
}
