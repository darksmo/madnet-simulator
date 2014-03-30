package de.saviodimatteo.madnetsim.actors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import de.saviodimatteo.madnetsim.MadnetSim;
import de.saviodimatteo.madnetsim.config.ConfigReader;
import de.saviodimatteo.madnetsim.data.Content;
import de.saviodimatteo.madnetsim.data.DataSize;
import de.saviodimatteo.madnetsim.data.IRequestor;
import de.saviodimatteo.madnetsim.data.Poi;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.data.Request;
import de.saviodimatteo.madnetsim.data.TimedPoint;
import de.saviodimatteo.madnetsim.data.Request.ERequestStatus;
import de.saviodimatteo.madnetsim.simulation.Simulator;
import de.saviodimatteo.madnetsim.utils.Distance;
import de.saviodimatteo.madnetsim.utils.FileHelper;
import de.saviodimatteo.madnetsim.utils.Pair;


public class Node extends Device implements IRequestor {
	private static int iNumCreatedNodes = 0;
	
	// Fields of the *trace file*
	private final int KTimeField = 0;
	private final int KLatField = 1;
	private final int KLonField = 2;
	private final int KRequestField = 3;
	private final int KLookaheadField = 4;
	
	private int iLookaheadLength;
	private final int KUnhappinessTime = 10;
	BufferedReader iTracesReader;
	
	/* The first dimension indicates the number of the current prediction.
	 * The second dimension is 3 Objects large, these are:
	 *  [0] = TimedPoint	the predicted position and time
	 *  [2] = Long		the size of an issued request (optional)
	 *  [3] = Integer		the lookahead (optional)
	 *  0,1,2 are read from the configuration file of the node.
	 */
	Object[][] iPredictionList;
	int iPtrPredictionListEnd; // Always points to the last prediction available
	
	// Number of fields
	private final int KPLNumInfo = 3;
	
	// Fields of the *prediction list*
	private final int KPLTimedPosition = 0;
	private final int KPLReqContent = 1;
	private final int KPlReqLookahead = 2;
	
	boolean iEndTracesReached;
	private Integer iNextRequestLookAhead = null;
	private Content iNextRequestedContent = null;
	
	// Knowledge of the past
	private TimedPoint iPreviousPosition;
	private TimedPoint iPreviousTracePosition;
	private int iLastPosTime;
	
	BaseStation iBaseStation;
	String iId;
	private int iNumber;
	public List<AccessPoint> iApList;
	Request iRequest;
	AccessPoint iLastDownloadAp; // used to disconnect when completion is reached during 3G transfers.
	private Request iLastSatisfiedRequest = null;
	
	private NodeCallbackReceiver iCallbackReceiver;
	private Poi iLastVisitedPoi; // used to avoid the node to generate the same content if moved again within the POI's range.
	private boolean iWaitingFirstTransfer;
	
	private Node(BaseStation aBaseStation) {
		super(0, 0);
		iLookaheadLength = MadnetSim.config.getNodeMaximumLookahead();
		iPtrPredictionListEnd = iLookaheadLength;
		iPredictionList = new Object[iLookaheadLength+1][KPLNumInfo];
		setMemoryLimit(new DataSize(80,DataSize.UNIT.GB));
		iMinLinkRadius = 50;
		iMaxLinkRadius = 60;
		iBaseStation = aBaseStation;
		// ** REMOVE THIS AND THE COMMENT IN SETPOSITION TO USE DYNAMIC BASE STATIONS **/
		if (iBaseStation == null) {
			iBaseStation = Simulator.bsList.get(0); // Set the first one as base station
			iBaseStation.subscribeNode(this);
		}
		// ** END **
		
		iId = String.valueOf(new Random((long) (iMinLinkRadius + iMaxLinkRadius)).nextInt());
		iEndTracesReached = false;
		iRequest = null;
		iNumber = iNumCreatedNodes;
		Node.iNumCreatedNodes++;
		iCallbackReceiver = new NodeCallbackReceiver();
		iLastVisitedPoi = null; // And will remain like this on download case
		iWaitingFirstTransfer = false;
	}
	public Node(String aId) {
		this((BaseStation)null);
		iId = aId;
		Object[] p = readNextPosition();
		setPosition((Point) p[0]);
		for (int kk=0; kk < iLookaheadLength; kk++) {
			p = readNextPosition();
			for (int ii=0 ; ii < KPLNumInfo; ii++) {
				iPredictionList[kk][ii] = p[ii];
			}
		}
		iPreviousTracePosition = new TimedPoint((Point)this, Simulator.time);
		updateSimulationEndTime();
	}
	public NodeCallbackReceiver getCallbackReceiver() {
		return iCallbackReceiver;
	}
	public int getNumber() {
		return iNumber;
	}
	public boolean isWaitingFirstTransfer() {
		return iWaitingFirstTransfer;
	}
	public void commitRequest(int aTime, Point aDestPoint) {
		if (iBaseStation == null)
			System.out.println("time: " + Simulator.time + ") Basestation was null " + this.iId + " " + this.getLon() + "," + this.getLat());
		else
			iBaseStation.contentRequested(this, iRequest.getContent(), aTime, iRequest.getPreparedAt(), aDestPoint);
		
	}
	
	private void updateSimulationEndTime() {
		ConfigReader c = MadnetSim.config;
		Vector<String> tailResult = (Vector<String>) FileHelper.tail((c.getNodeMovesBaseDir() + 
				"/" +
				c.getNodeMovesPrefix() +
				iId + 
				c.getNodeMovesSuffix()), 1);
		String lastLine = tailResult.get(0);
		int maxTime = Integer.valueOf(lastLine.split(" ")[0]);
		if ( Simulator.endTime < maxTime )
			Simulator.endTime = maxTime;
	}
	
	private Object[] readNextPosition() {
		ConfigReader c = MadnetSim.config;
		return readNextPosition(c.getNodeMovesBaseDir() + 
				"/" +
				c.getNodeMovesPrefix() +
				iId + 
				c.getNodeMovesSuffix());
	}
	
	// Returns an Object[4] containing <Point><Integer (time)><reqNumBytes (null)><reqLookAhead (null)>
	private Object[] readNextPosition(String aTracePath) {		
		Object[] result = new Object[KPLNumInfo];
		for (int ii=0 ; ii < KPLNumInfo; ii++) {
			result[ii] = null;
		}
		// Create a new traces reader if necessary
		if (iTracesReader == null) { 
			try {
				iTracesReader = new BufferedReader(new FileReader(aTracePath));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		// Try to read a line
		try {
			if (iTracesReader.ready()) {
				try {
					String traceLine = iTracesReader.readLine();
					if (traceLine != null) {
						String[] traceFields = traceLine.split(" ");
						float lat = Float.valueOf(traceFields[KLatField]);
						float lon = Float.valueOf(traceFields[KLonField]);
						if (traceFields.length > 3) {
							// Format <SIZE(long)>-<ID(string)>-<KIND(char)>
							String[] rawRequest = traceFields[KRequestField].split("-");
							long requestSize = MadnetSim.config.getFixedRequestSize();
							String requestKindStr = "B";
							String requestId = null;
							
							if (rawRequest.length >= 1 && requestSize == 0) // In the configuration the value was set to 0...
								requestSize = Long.valueOf(rawRequest[0]);
							if (rawRequest.length >= 2)
								requestId = String.valueOf(rawRequest[1]);
							if (rawRequest.length >= 3)
								requestKindStr = String.valueOf(rawRequest[2]);
						
							Content c;
							if (requestId != null)
								c = new Content(requestSize,requestId);
							else
								c = new Content(requestSize);
							result[KPLReqContent] = c;
						}
						if (traceFields.length > 4)
							result[KPlReqLookahead] = Integer.valueOf(traceFields[KLookaheadField]);
						int traceTime = Integer.valueOf(traceFields[KTimeField]);
						Point tracePosition = new Point(lon,lat);
						result[KPLTimedPosition] = new TimedPoint(tracePosition,traceTime);
					} else { // End of Stream Reached
						iTracesReader.close();
						result = null;
					}
				} catch (IOException e) {
					// no more traces to read
					result = null;
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// do nothing
		}
		return result;
	}
	
	public boolean goToNextPosition() {
		boolean isNodeMoved  = true;
		if (iPredictionList[0][KPLTimedPosition] != null) {
			// Take the next position from the prediction list
			TimedPoint p = (TimedPoint) iPredictionList[0][KPLTimedPosition];
			iNextRequestedContent = (Content) iPredictionList[0][KPLReqContent];
			iNextRequestLookAhead = (Integer) iPredictionList[0][KPlReqLookahead];
			
			// Also this is the previous traces position as the node can move...
			iPreviousTracePosition = p;
			
			// Shift predictions
			for (int kk=0; kk<iLookaheadLength-1; kk++) {
				for (int ii=0 ; ii < KPLNumInfo; ii++) {
					iPredictionList[kk][ii] = iPredictionList[kk+1][ii];
				}
			}
			
			// Read the last prediction
			iPredictionList[iLookaheadLength-1] = readNextPosition();
			
			// move the node
			setPosition(p);			
		} else {
			iEndTracesReached = true;
			isNodeMoved = false;
			System.out.println("Warning, no more traces for Node " + iId);
		}
		return isNodeMoved;
	}
	public TimedPoint getPreviousPosition() {
		return iPreviousPosition;
	}
	
	public void setPosition(Point aPosition) { // do not add code here!
		  setPosition(aPosition.getLon(), aPosition.getLat(), Simulator.time);
	}
	public void setPosition(TimedPoint aPosition) { // do not add code here!
		this.setPosition((Point) aPosition, aPosition.getTime());
	}
	public void setPosition(Point aPosition, int aCurrentTime) { // do not add code here!
		this.setPosition(aPosition.getLon(),aPosition.getLat(), aCurrentTime);
	}
	
	public void setPosition(double lon, double lat, int aCurrentTime) { // add code here!
		// Update previous
		iPreviousPosition = new TimedPoint(getLon(),getLat(),iLastPosTime);
		
		// Update current
		iLastPosTime = aCurrentTime;
		super.setPosition(lon,lat);
	
		// Every time a node moves check if it can generate some content
		if (iLastVisitedPoi == null && MadnetSim.config.getIsUploadCase()) {
			List<Poi> poiList = Simulator.poiList.getElementsAround(this);
			for (Poi poi : poiList) {
				if (isPointInRange((Point) poi, this, MadnetSim.config.getPoiRadius())) {
					iLastVisitedPoi = poi;
					break;
				}
			}
		}
		
		// Every time a node moves set the correct base station
		/* if (iBaseStation == null || !iBaseStation.contains(this)) {
			if (iBaseStation != null) { // i.e., and !contains this
				iBaseStation.unsubscribeNode(this);
				iBaseStation = null;
			}
			float bsRay = Simulator.bsList.get(0).getCellRadius();
			List<BaseStation> baseAround = Simulator.bsList
					.getIncreasedElementsAround(this, bsRay);
			for (BaseStation bs : baseAround) {
				if (bs.contains(this)) {
					iBaseStation = bs;
					bs.subscribeNode(this);
					break;
				}
			}
			if (iBaseStation == null) {  // TOREMOVE
				//System.out.println("Time " + Simulator.time + ") ERROR: No Available Base Station for Node " + this.iId + " in position " + this.getLon() + "," + this.getLat());
				iBaseStation = Simulator.bsList.get(0);
			}
		} */
		
		// Set request status
		if (iRequest != null) {
			if (iRequest.getPercentComplete() > 0) {  // The node connected (and transferred data) at least one time
				// And remove if the request completed or never started
				if (iRequest.getRequestStatus() == ERequestStatus.REQ_INACTIVE) {
					removeOutOfRangeAps();
				}
			}
		}
		
	}
	
	public void resetRequest() {
		clearApList();
		iRequest = null;
		iLastVisitedPoi = null; // Make sure a new request upload will start 
								// after this time.
	}
	/**
	 * Returns true if the node will follow another predicted move
	 */
	public boolean willNextMove() {
		return ( iPredictionList[0][KPLTimedPosition] != null ? true : false);
	}
	
	public BaseStation getBaseStation() {
		return iBaseStation;
	}
	
	public String getId() {
		return iId;
	}
	
	// Get the content
	public void setApList(List<AccessPoint> aApList) {
		iApList = aApList;
		iWaitingFirstTransfer=false;
	}
	public AccessPoint getClosestApInRange() {
		List<AccessPoint> apList = null;
		if (MadnetSim.config.getIsUploadCase()) {
			apList = Simulator.apList.getElementsAround(this);
		} else {
			apList = iApList;
		}
		
		AccessPoint result = null;
		if (apList != null) {
			int apListSize = apList.size();
			for (int aa = 0; aa < apListSize; aa++) {
				if (aa >= 0) {
					AccessPoint ap = apList.get(aa);
					
					boolean isApInRange = isApInRange(ap); 
					if ( isApInRange && 
					    (result == null || 
					     Distance.distance(this, ap) < Distance.distance(this,result)) 
					) {
						result = ap;
					}
				}
			}
		}
		return result;
	}
	
	public List<AccessPoint> getOutOfRangeAps() {
		List<AccessPoint> apList = new Vector<AccessPoint>();
		if (iApList != null) {
			int apListSize = iApList.size();
			for (int aa = 0; aa < apListSize; aa++) {
				AccessPoint ap = iApList.get(aa);
				if (!isApInRange(ap))
					apList.add(ap);
			}
		}
		return apList;
	}
	public boolean isPointInRange(Point aPoint, Point aCurrentPosition, float aRangeRadius) {
		if (aPoint != null  &&
				aCurrentPosition != null // CHECK
				) {
			double pointApDistance = Distance.distance(aCurrentPosition, aPoint);
			if (pointApDistance < aRangeRadius) {
				return true;
			}
		}
		return false;
	}
	public boolean isPointInRange(AccessPoint aAp, Point aCurrentPosition) {
		return isPointInRange(aAp,aCurrentPosition, aAp.getLinkRadius().fst);
	}
	public boolean isApInRange(AccessPoint aAp) {
		return isPointInRange(aAp,(Point)this);
	}

	/**
	 * Returns the number of seconds an access point is in range, limited to the simulator delta.
	 * @param aAccessPoint is the access point we want to test the range
	 * @param aPreviousTransferTime is the amount of seconds passed since the previous transfer
	 * @return Pair<Integer, Point> that is the number of seconds and the node position.
	 */
//	public Pair<Integer,Point> getSecondsInRange(AccessPoint aAccessPoint, int aPreviousTransferTime) {
//		return getSecondsInRange(aAccessPoint, aPreviousTransferTime, Simulator.time + Simulator.KDeltaT);
//	}
	public Pair<Integer, Point> getSecondsInRange(AccessPoint aAccessPoint, int aFromTime, int aMaximumTime) {
		// input parameters
		Point startPosition = positionAtT(aFromTime);
		int   startPositionTime = aFromTime;
		
		// init
		int       currentTime = startPositionTime;
		Point currentPosition = startPosition;
		int	  endPositionTime = startPositionTime;
		Point     endPosition = startPosition;
		
		// algorithm
		boolean inRange = isPointInRange(aAccessPoint,currentPosition);
		while (inRange && currentTime < aMaximumTime) {
			currentTime++;
			currentPosition = positionAtT(currentTime);
			if (inRange = isPointInRange(aAccessPoint,currentPosition)) {
				endPosition = currentPosition;
				endPositionTime = currentTime;
			}
		}
		
		// result
		int nodeStopSeconds = endPositionTime - startPositionTime;
		return new Pair<Integer, Point>(nodeStopSeconds, endPosition);
	}
	public Pair<Integer, Point> getSecondsInRange(BaseStation aBaseStation, int aFromTime, int aMaximumTime) {
		// input parameters
		Point startPosition = positionAtT(aFromTime);
		int   startPositionTime = aFromTime;
		
		// init
		int       currentTime = startPositionTime;
		Point currentPosition = startPosition;
		int	  endPositionTime = startPositionTime;
		Point     endPosition = startPosition;
		
		// algorithm
		boolean inRange = aBaseStation.contains(currentPosition);
		while (inRange && currentTime < aMaximumTime) {
			currentTime++;
			currentPosition = positionAtT(currentTime);
			if (inRange = aBaseStation.contains(currentPosition)) {
				endPosition = currentPosition;
				endPositionTime = currentTime;
			}
		}
		
		// result
		int nodeStopSeconds = endPositionTime - startPositionTime;
		return new Pair<Integer, Point>(nodeStopSeconds, endPosition);
	}
	
	public int getLastPosTime() {
		return iLastPosTime;
	}
	
	public Point positionAtT(int aTime) {
		// INIT (input parameters)
		TimedPoint previousPosition = iPreviousTracePosition;
		int previousPositionTime = previousPosition.getTime();
		
		Point result = null;
		TimedPoint nextPosition = (TimedPoint) iPredictionList[0][KPLTimedPosition];
		
		// Reached the end of the trace
		if (nextPosition == null)
			return new Point(this);
		
		int nextPositionTime = nextPosition.getTime();
		
		int shiftCount = 0;
		while (aTime > nextPositionTime && iPredictionList[shiftCount+1] != null && iPredictionList[shiftCount+1][KPLTimedPosition] != null) {
			// Shift positions one left
   			previousPosition = (TimedPoint) nextPosition;
   			previousPositionTime = previousPosition.getTime(); // Added
			nextPosition = (TimedPoint) iPredictionList[shiftCount+1][KPLTimedPosition];
   			nextPositionTime = nextPosition.getTime();
			shiftCount++;
		}
			
		if (nextPosition != null) {
			double prevX = previousPosition.getLon();
			double prevY = previousPosition.getLat();
			double nextY = nextPosition.getLat();
			double nextX = nextPosition.getLon();
			double derX, derY;
			
			double intervalDurationSecs = nextPositionTime - previousPositionTime;
			double secondsDone = aTime - previousPositionTime;
			double increment = 1/intervalDurationSecs;
			double incrementXY;
			if (Math.abs(prevX - nextX) > Math.abs(prevY - nextY)) {
				incrementXY = (nextX - prevX) / intervalDurationSecs;
				derX = prevX + incrementXY * secondsDone;
				derY = linearInt(prevY,nextY,increment * secondsDone);
				
			} else {
				incrementXY = (nextY - prevY) / intervalDurationSecs;
				derY = prevY + incrementXY * secondsDone;
				derX = linearInt(prevX,nextX,increment * secondsDone);
				
			}
			
			result = new Point(derX,derY);
		}
		return result;
	}

	public double linearInt(double y1, double y2, double mu) {
		return y1*(1-mu)+y2*mu;
	}
	
	boolean isNan (double aNumber)
	{
	    if(aNumber == aNumber)
	        return false;
	    else
	        return true;
	}
	public void clearApList() {
		if (iApList != null) {
			for (AccessPoint ap : iApList)
				if (ap.isConnected(this))
					ap.disconnectNode(this);
			
				iApList.clear();
		}
	}
	public List<AccessPoint> removeOutOfRangeAps() {
		List<AccessPoint> result = new Vector<AccessPoint>();
		int apListPoint = 0;
		if (iApList != null && iApList.size() > 0) {
			int apListSize = iApList.size();
			while (apListPoint < apListSize) {
				AccessPoint ap = iApList.get(apListPoint);
				float nodeApDistance = (float) Distance.distance((Point) this, (Point) ap);
				if (nodeApDistance >= ap.getLinkRadius().fst) {
					AccessPoint removedAp = iApList.remove(apListPoint);
					result.add(removedAp);
					apListSize--;
				} else {
					apListPoint++;
				}
			}
		}		
		return result;
	}
	public AccessPoint getHeadAp() {
		AccessPoint result = null;
		if (iApList.size() > 0) {
			return iApList.get(0);
		}
		return result;
	}
	public AccessPoint removeHeadAp() {
		AccessPoint result = null;
		if (iApList != null && iApList.size() > 0 && iApList.get(0) != null) {
			result = iApList.remove(0);
			updateRequestStatus(false);
		}
		return result;
	}
	public void prepareRequest(Content aContent, int aForeseenAt, int aPreparedAt, Point aForeseenPosition) {
		Content c = aContent;
		Request r = new Request(this,c, aForeseenAt, aPreparedAt, aForeseenPosition);
		iRequest = r;
		iWaitingFirstTransfer = true;
	}
	private void dataReceived(long aBytes, int aTime) {
		iRequest.addData(aBytes, aTime);
		iWaitingFirstTransfer = false;
		if (iRequest.getCompletedAt() == aTime && iRequest != iLastSatisfiedRequest) {
			this.getCallbackReceiver().cbRequestSatisfied(iRequest);
			
			// Reset the request if upload case (do not wait the lookahead)
			if (MadnetSim.config.getIsUploadCase()) {
				resetRequest();
			}
			
			iLastSatisfiedRequest = iRequest;
		}
	}
	public void dataReceivedFromAp(long aBytes, AccessPoint aAp, int aTime) {
		dataReceived(aBytes,aTime);
		iLastDownloadAp = aAp;
	}
	public void dataReceivedFromBs(long aBytes, BaseStation aBs, int aTime) {
		dataReceived(aBytes,aTime);
	}
	
	public Content getRequestedContent() {
		if (iRequest != null)
			return iRequest.getContent();
		else
			return null;
	}
	public long getLastVisitedPoiSize() {
		long result = iLastVisitedPoi.getSize();
		return result;
	}
	
	/**
	 * Returns the position for the node to receive the information (request expiry position).
	 */
	public TimedPoint getForeseenPosition() {
		TimedPoint result = null;
		if (iNextRequestLookAhead != null && !MadnetSim.config.getIsUploadCase()) {
			result = (TimedPoint) iPredictionList[iNextRequestLookAhead-1][KPLTimedPosition];
		} 
		if (result == null) {  // ALWAYS do this for the upload case
			TimedPoint farestPosition = getFarestPrediction();
			if (farestPosition == null)
				return null;
			else 
				return new TimedPoint(farestPosition);
		} else {
			return new TimedPoint(result);
		}
	}
	private TimedPoint getFarestPrediction() {
		boolean predictionEnded = false;
		TimedPoint result = null;
		for (int kk=iPtrPredictionListEnd; kk >= 0; kk--) {
			if (predictionEnded)
				break;	
			TimedPoint futurePoint = (TimedPoint) iPredictionList[kk][KPLTimedPosition];
			if (futurePoint != null) {
				TimedPoint futurePosition = (TimedPoint) iPredictionList[kk][KPLTimedPosition];
				result = (TimedPoint) futurePosition;
				predictionEnded = true;
				iPtrPredictionListEnd = kk;
			}
		}
		return result;
	}
	public boolean willNextIssueRequest() {
		boolean result = false;
		if (!MadnetSim.config.getIsUploadCase()) {
			if (iPredictionList[0][KPLTimedPosition] != null && ( (Content) iPredictionList[0][KPLReqContent] != null) )
				result = true;
		} else {
			if (iLastVisitedPoi != null && iRequest == null) {
				/* Conditions are:
				 * - The previous request was satisfied (hence reset, and iLastVisitedPoi as well)
				 * - New Poi was visited (i.e., != null)
				 */
				result = true;
			}
		}
		return result;
	}
	public AccessPoint getLastDownloadAp() {
		return iLastDownloadAp;
	}
	public Content getReadRequestContent() {
		return iNextRequestedContent;
	}
	public int getRequestLookahead() {
		return ( iNextRequestLookAhead != null && iNextRequestLookAhead > 0 ? iNextRequestLookAhead : iLookaheadLength );
	}
	public TimedPoint getNextPosition() {
		return getNextPosition(0);
	}
	public TimedPoint getNextPosition(int aNextPosition) {
		return (TimedPoint) iPredictionList[aNextPosition][KPLTimedPosition];
	}
	public Request getCurrentRequest() {
		return iRequest;
	}
	public void setRequestStatus(ERequestStatus aReqStatus) {
		iRequest.setStatus(aReqStatus);
	}
	public void updateRequestStatus(boolean contentApInRange) {
		if (iRequest.getPercentComplete() < 100) {
			if (contentApInRange)
				iRequest.setStatus(ERequestStatus.REQ_TRANSFERRING);
			else
				iRequest.setStatus(ERequestStatus.REQ_INCOMPLETE);
		} else if (iRequest.getPercentComplete() >= 100) {
			iRequest.setStatus(ERequestStatus.REQ_INACTIVE);
		}
	}
	public boolean isHappy() {
		if (iRequest != null && iRequest.getRequestStatus() == ERequestStatus.REQ_INACTIVE) {
			if (Simulator.time - iRequest.getPreparedAt() > KUnhappinessTime && (!((Point)this).equals((Point)iPreviousPosition)) ) {
				return false;
			}
		}
		return true;
	}
	public void setHappy() {
		iRequest.setPreparationTime(Simulator.time);
	}
}
