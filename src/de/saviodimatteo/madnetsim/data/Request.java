package de.saviodimatteo.madnetsim.data;

import de.saviodimatteo.madnetsim.actors.Node;
import de.saviodimatteo.madnetsim.exceptions.InvalidDomainException;
import de.saviodimatteo.madnetsim.simulation.Simulator;

public class Request {
	public enum ERequestStatus { 
		REQ_INACTIVE,		// The transfer never started or completed (check percentage)
		REQ_TRANSFERRING,	// The transfer is in progress
		REQ_INCOMPLETE		// The transfer was not successfully completed (ap out of range)
	}
	/** 
	 * Represents a content request. It stores the content the node would like to download,
	 * the final destination point of its path and the estimated time to arrive there
	 */
	private IRequestor iRequestor;
	private Content iContent;
	private int iExpirationTime;
	private int iPreparedTime;
	private double iDestLon;
	private double iDestLat;
	private double iPercentComplete;
	private int iBytesReceived;
	private ERequestStatus iStatus;
	private int iCompletedTime = -1;
	
	public Request(IRequestor aRequestor, Content aContent, int aExpirationTime, int aPreparedTime, double aDestLongitude, double aDestLatitude) {
		setRequestor(aRequestor);
		setContent(aContent);
		setExpirationTime(aExpirationTime);
		setPreparationTime(aPreparedTime);
		setDestination(aDestLongitude, aDestLatitude);
		iPercentComplete = 0;
		iStatus = ERequestStatus.REQ_INACTIVE;
	}
	
	public Request(Node aSender, Content aContent, int aExpirationTime, int aPreparedTime, Point aDestPoint) {
		this(aSender,aContent,aExpirationTime, aPreparedTime,aDestPoint.getLon(), aDestPoint.getLat());
	}
	public Request(Node aSender, Content aContent) {
		this(aSender,aContent,0, Simulator.time, -1, -1);
	}

	public int getPreparedAt() {
		return iPreparedTime;
	}
	public void setDestination(double aDestLongitude, double aDestLatitude) {
		this.iDestLat = aDestLatitude;
		this.iDestLon = aDestLongitude;
	}
	public void setRequestor(IRequestor iRequestor) {
		this.iRequestor = iRequestor;
	}

	public IRequestor getRequestor() {
		return iRequestor;
	}

	public void setContent(Content iContent) {
		this.iContent = iContent;
	}

	public Content getContent() {
		return iContent;
	}

	public void setExpirationTime(int iTime) {
		this.iExpirationTime = iTime;
	}

	public int getExpirationTime() {
		return iExpirationTime;
	}
	public int getBytesReceived() {
		return iBytesReceived;
	}
	public void addData(long aBytes, int aTime ) {
		iBytesReceived+=aBytes;
		iStatus = ERequestStatus.REQ_TRANSFERRING;
		long wishedBytes = iContent.getSize();
		if (iBytesReceived > wishedBytes)
			iPercentComplete = 100;
		else {
			iPercentComplete = 100 / (float)wishedBytes * (float)iBytesReceived;
		}
		
		// Set completion
		if (iPercentComplete >= 100) {
			iStatus = ERequestStatus.REQ_INACTIVE;
			if (iCompletedTime == -1)
				iCompletedTime = aTime;
		}
	}
	public int getCompletedAt() {
		return iCompletedTime;
	}
	public double getDestLat() {
		return this.iDestLat;
	}
	public double getDestLon() {
		return this.iDestLon;
	}
	public double getPercentComplete() {
		return iPercentComplete;
	}
	public ERequestStatus getRequestStatus() {
		return iStatus;
	}

	public void setStatus(ERequestStatus aRequestStatus) {
		iStatus	= aRequestStatus;	
	}
	public void setPreparationTime(int aPreparedAtT) {
		iPreparedTime = aPreparedAtT;
	}
	
}
