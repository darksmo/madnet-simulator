package de.saviodimatteo.madnetsim.simulation;

public enum EEventKind {
	/** A node moves in a new position **/
	NODE_MOVE,
	/** A BASE STATION received node request **/
	NODE_REQ,
	/** A node is connecting to an access point to retrieve data **/
	NODE_REQ_RECEIVED,
	/** A node requests a content to a BASE STATION **/
	NODE_CONNECTED,
	/** A node had received some of the content from an AP/BASE STATION **/
	NODE_GOTDATA,
	/** The content request has expired for the node **/
	NODE_REQ_EXPIRED,
	/** An access point received some data from a SERVER **/
	AP_GOTDATA,
	/** An access point finished to send the content **/
	AP_DEACTIVATED,
}
