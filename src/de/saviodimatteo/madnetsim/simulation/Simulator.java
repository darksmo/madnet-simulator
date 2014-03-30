package de.saviodimatteo.madnetsim.simulation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.saviodimatteo.madnetsim.MadnetSim;
import de.saviodimatteo.madnetsim.actors.AccessPoint;
import de.saviodimatteo.madnetsim.actors.BaseStation;
import de.saviodimatteo.madnetsim.actors.Node;
import de.saviodimatteo.madnetsim.data.Content;
import de.saviodimatteo.madnetsim.data.Poi;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.data.Request;
import de.saviodimatteo.madnetsim.data.TimedPoint;
import de.saviodimatteo.madnetsim.data.Request.ERequestStatus;
import de.saviodimatteo.madnetsim.exceptions.ContentUnavailableException;
import de.saviodimatteo.madnetsim.exceptions.InvalidDomainException;
import de.saviodimatteo.madnetsim.graph.Edge;
import de.saviodimatteo.madnetsim.graph.WeightedEdge;
import de.saviodimatteo.madnetsim.simulation.statistics.Statistics;
import de.saviodimatteo.madnetsim.utils.Distance;
import de.saviodimatteo.madnetsim.utils.Pair;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.graph.SparseMultigraph;

public class Simulator {
	public static int KDeltaT; // Accessed by node
	public static int time;
	public static int endTime;
	public static List<Node> nodeList;
	public static LockedList<BaseStation> bsList;
	public static LockedList<AccessPoint> apList;
	public static LockedList<Poi> poiList;
	public EventQueue eventList;
    private Statistics stats;
	private int giveProgressEverySteps = -1;
	public static long iApMaximumUsage = 0; // updated when loading ap list only
	public static long iApMinimumUsage = Long.MAX_VALUE;
	public SparseMultigraph<Point, WeightedEdge> iCityGraph;
	public BetweennessCentrality<Point, Edge> iCb;
	
	public Simulator(int aDeltaT) {
		endTime = 0;
		
		int progressReportSteps = MadnetSim.config.getStdoutReportSteps();
		if (progressReportSteps > 0)
			setPrintProgress(progressReportSteps);
		
		if (aDeltaT > 0)
			KDeltaT = aDeltaT;
		initializeSystem();
		initializeEventList();
	}
	private boolean stopCondition() {
		int eventsCount = eventList.size();
		if (eventsCount <= 0 && time > endTime)
			return true;
		else
			return false;
	} 

	public boolean doSimulationStep() {
		if (!stopCondition()) {
			// Pop and Handle the Events
			while (eventList.size() > 0 && eventList.getHeadTime() <= time) {
				Event event = eventList.pop();
				EEventKind eventKind = event.getKind();
				if ( eventKind == EEventKind.NODE_MOVE ) {  // Schedules the next move || NODE_GOTDATA
						handleNodeMove(event);
				} else if (eventKind == EEventKind.NODE_REQ) { // Schedules req received
					handleNodeRequestIssued(event);
				} else if (eventKind == EEventKind.NODE_REQ_RECEIVED) { // Schedules AP_GOTDATA || NODE_CONNECTED
 					handleNodeRequestReceived(event);
				} else if (eventKind == EEventKind.NODE_CONNECTED) { // Schedules NODE_GOTDATA
					try {
 						handleNodeConnected(event);
					} catch (ContentUnavailableException e) {
						e.printStackTrace();
					}
				} else if (eventKind == EEventKind.NODE_GOTDATA) {
					handleNodeGotData(event);
				} else if (eventKind == EEventKind.NODE_REQ_EXPIRED) {
					handleNodeRequestExpired(event);
				} else if (eventKind == EEventKind.AP_GOTDATA) {
					handleApGotData(event);
				}
			}
			collectStatistics();		
			time+=KDeltaT;
			if (  MadnetSim.config.getIsStatisticsEnabled() && 
				 (time % stats.KNodeStatsArrayTimeSize) == 0) 
			{
				stats.dumpPartialStats();
			}
			return true;
		} else
			return false;
	}
	
	public void setPrintProgress(int steps) {
		giveProgressEverySteps = steps;
	}
	public Statistics getStatistics() {
		return stats;
	}
		
	private void collectStatistics() {
		if (MadnetSim.config.getIsDebugEnabled())
			printStatus();
		if (MadnetSim.config.getIsStatisticsEnabled()) {
			if (MadnetSim.config.getIsIndividualNodesStatsEnabled())
				stats.updateIndividualNodeStats(nodeList);
			if (MadnetSim.config.getIsUnhappinessStatsEnabled())
				stats.recordUnhappyNodesPosition(nodeList);
			
			stats.updateGlobalStats(apList);
		}
	}
	public void simulate() {
		int stepCount = 0;
		while(this.doSimulationStep()) {
			stepCount++;
			if (giveProgressEverySteps > 0 && stepCount > giveProgressEverySteps) {
				System.out.println("%:" + ((float)time * 100.0f / (float) endTime));
				stepCount = 0;
			}
		}
		
		if (MadnetSim.config.getIsStatisticsEnabled()) {
			stats.dumpDelayHistogram(nodeList);
		}
	}
	
	private void markNeighboursNodes(Point seed) {
		List<Point> points = new Vector<Point>();
		points.add(seed);
		while (!points.isEmpty())
		{
			for (Point n : iCityGraph.getNeighbors(points.remove(0))) {
				if (n.visited == false) {
					n.visited = true;
					points.add(n);
				}
			}	
		}
	}
	
	private void initializeSystem() {
		iCityGraph = new SparseMultigraph<Point,WeightedEdge>();
		
		Vector<String> nodeNames = loadStrings(MadnetSim.config.getNodeNamesPath());
		Vector<String> baseStationData = loadStrings(MadnetSim.config.getBasestationNamesPath());
		Vector<String> accessPointData = loadStrings(MadnetSim.config.getApsPath());
		Vector<String> poiData = loadStrings(MadnetSim.config.getPoiPath());
		time = 0;
		
		// Initialize Point of Interests List
		poiList = new LockedList<Poi>();
		if (MadnetSim.config.getIsUploadCase()) {
			System.out.println("Initializing POI LockedList...");
			for (String line : poiData) {
				String[] poiFields = line.split(" ");
				double longitude = Double.valueOf(poiFields[0]);
				double latitude = Double.valueOf(poiFields[1]);
				long contentSize = Long.valueOf(poiFields[2]);
				Poi poi = new Poi(longitude,latitude,contentSize);
				poiList.add(poi);
			}
			poiList.lock();
			System.out.println("POI LockedList initialized (" + poiList.size() + ")");
		}
		
		// Initialize APs their locations, link radius
		apList = new LockedList<AccessPoint>();
		int apCount = accessPointData.size();
		for (int ii=0; ii < apCount; ii++) {
			String strApData = accessPointData.get(ii);
			String[] apData = strApData.split(" ");
			long usage = Long.valueOf(apData[0]);
			float latitude = Float.valueOf(apData[1]);
			float longitude = Float.valueOf(apData[2]);
			float minRadius = Float.valueOf(apData[3]);
			float maxRadius = Float.valueOf(apData[4]);
			
			if (usage >= MadnetSim.config.getApUsageThreshold() ) {
				AccessPoint ap = new AccessPoint(longitude, latitude);
				try {
					ap.setLinkRadius(minRadius, maxRadius);
					ap.setUsage(usage);
					
					// MIN-MAX for the plotting
					if (usage > iApMaximumUsage)
						iApMaximumUsage = usage;
					if (usage < iApMinimumUsage)
						iApMinimumUsage = usage;
					
				} catch (InvalidDomainException e) {
					System.out.println("Invalid link radius for this access point");
				}
				apList.add(ap);
			}
		}
		apList.lock();
		
		// Initialize BASE STATIONS
		bsList = new LockedList<BaseStation>();
		for (String line : baseStationData) {
			String[] baseData = line.split(" "); // # name, x, y, halfedge(radius)
			String bsName = baseData[0];
			float bsX = Float.parseFloat(baseData[1]);
			float bsY = Float.parseFloat(baseData[2]);
			float radius = Float.parseFloat(baseData[3]);
			BaseStation basestation = new BaseStation(bsName,bsX,bsY,radius,apList);
			bsList.add(basestation);
		}
		bsList.lock();
		
		// Initialize NODES and their position
		nodeList = new Vector<Node>();
		int nodeNamesCount = nodeNames.size();
		for (int nn = 0; nn < nodeNamesCount ; nn++) {
			String nodeName = nodeNames.get(nn);
			Node n = new Node(nodeName);
			nodeList.add(n);
		}
		
		// Initialize Statistics
		stats = new Statistics(nodeNames.size());
		
		time = KDeltaT;
	}
	private void loadCityGraph(String aFilename) {
		BufferedReader iGraphReader = null;
		Map<String,Point> nodesMap = new HashMap<String,Point>();
		try {
			iGraphReader = new BufferedReader(new FileReader(aFilename));
			while (iGraphReader.ready()) {
				String line = iGraphReader.readLine();
				String[] edges = line.split(" ");
				if (edges.length == 5) {
					Point a = null;
					Point b = null;
					float weight = Float.parseFloat(edges[4]);
					a = nodesMap.get(edges[0]+edges[1]);
					b = nodesMap.get(edges[2]+edges[3]);
					if (a == null) {
						a = new Point(Double.parseDouble(edges[0]),Double.parseDouble(edges[1]));
						nodesMap.put(edges[0] + edges[1], a);
					}
					if (b == null) {
						b = new Point(Double.parseDouble(edges[2]),Double.parseDouble(edges[3]));
						nodesMap.put(edges[2] + edges[3], b);
					}
					iCityGraph.addEdge(new WeightedEdge(weight), a, b);
				}
			}
			iGraphReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void initializeEventList() {
		eventList = new EventQueue();	
		// Add initial events
		int nodesCount = nodeList.size();
		for (int nn=0; nn < nodesCount; nn++) {
			Node node = nodeList.get(nn);
			if (node.willNextMove()) {
				// Schedule the next TRACE movement first
				TimedPoint nextPosition = node.getNextPosition();
				Event evt = new Event(EEventKind.NODE_MOVE,nextPosition.getTime(),node);
				eventList.add(evt);
				
				// Schedule a new movement to the next Delta if no moves are scheduled for the node in that time
				int nextMoveTime = time + KDeltaT;
				Event nextMoveEvent = eventList.getNextNodeMoveEvent(node);
				if (nextMoveEvent != null && nextMoveEvent.getTime() > nextMoveTime) {
					Point nodePos = node.positionAtT(nextMoveTime);
					Pair<Node, Point> deltaMoveArgs = new Pair<Node, Point>(node,nodePos);
					Event evtDeltaMove = new Event(EEventKind.NODE_MOVE,nextMoveTime,deltaMoveArgs);
					eventList.add(evtDeltaMove);
				}
			}
		}
		
	}

	private Vector<String> loadStrings(String aFilename) {
		Vector<String> result = new Vector<String>();
		try {
			FileReader fileReader = new FileReader(aFilename);
			BufferedReader bufReader = new BufferedReader(fileReader);
			String line;
			while ( (line = bufReader.readLine()) != null ) {
				result.add(line);
			}
			fileReader.close();
			bufReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	// --- HANDLE EVENTS ---
	private void handleNodeMove(Event event) {
		// Argument of this event can be Node or Pair<Node,Point> where point can be a suggested position
		// computed during the simulation. When this appears the node should simply move in the suggested 
		// point and next move according to its trace;
		Object evtArgs = event.getEventData();
		
		Node node = null;
  		int nextEventTime = 0;
		if ( evtArgs instanceof Pair) {         // Handle Dictated movement
			Pair<Node,Point> eventNodePosition = (Pair<Node,Point>) evtArgs;
			// The movement is not written in the traces, it is a computed intermediate point before the next delta
			node = eventNodePosition.fst;
			Point destination = eventNodePosition.snd;
			node.setPosition(destination);
			nextEventTime = event.getTime();

			// Node Request for the upload case
			if (MadnetSim.config.getIsUploadCase()) {
				if (node.willNextIssueRequest() && !eventList.isNodeRequestAreadyQueued(node)) {
					Event reqEvt = new Event(EEventKind.NODE_REQ,event.getTime()+1,node);
					eventList.add(reqEvt);
				}
			}
			

			
		} else if (evtArgs instanceof Node)  {  // Handle Movement according to node trace
			// Move the node to the next position and schedule a new movement
			node = (Node) event.getEventData();
			node.goToNextPosition();

			// Schedule the next movement if available (information for this are read from the node config file) 
			// WARNING! it is important to schedule the movement before the request issue
			if (node.willNextMove()) {
				// Schedule the next movement according to trace time
				TimedPoint nextPos = node.getNextPosition();
				
				// The download should start according to the traces,
				nextEventTime = nextPos.getTime();
				
				Event moveEvt = new Event(EEventKind.NODE_MOVE,nextEventTime,node);
				eventList.add(moveEvt);
			
				// Schedule a request issue according the traces or do not trigger any (when uploading).
				if (node.willNextIssueRequest()) {
					if (!eventList.isNodeRequestAreadyQueued(node)) {
						
						// Set the request time according to the current case
						int reqTime = 0;
						if (MadnetSim.config.getIsUploadCase())
							reqTime = event.getTime();
						else
							reqTime = nextEventTime;
						
						Event reqEvt = new Event(EEventKind.NODE_REQ,reqTime,node);
						eventList.add(reqEvt);
					}
				}
			}
			
		} else { 
			System.out.println("WARNING, unknown arguments");
		}
		// NODE MOVED -- now scheduling future events
				
		// NEW MOVEMENT -- to the next Delta if no moves are scheduled for the node in that time
		int nextMoveTime = time + KDeltaT;
		Event nextMoveEvent = eventList.getNextNodeMoveEvent(node);
		if (nextMoveEvent != null && nextMoveEvent.getTime() > nextMoveTime) {
			Point nodePos = node.positionAtT(nextMoveTime);
			Pair<Node, Point> deltaMoveArgs = new Pair<Node, Point>(node,nodePos);
			Event evtDeltaMove = new Event(EEventKind.NODE_MOVE,nextMoveTime,deltaMoveArgs);
			eventList.add(evtDeltaMove);
		}
		
    	// REQUEST EXPIRATION -- if the node expected response time is expired (i.e., the node reached the req lookahead)
		Request nodeReq = node.getCurrentRequest();
    	if (nodeReq != null && nodeReq.getExpirationTime() <= event.getTime()) {
    		Event reqExpiredEvt = new Event(EEventKind.NODE_REQ_EXPIRED,event.getTime(),node);
    		eventList.add(reqExpiredEvt);
    	}
		
		// AP CONNECTION -- make the node connect to the related AP in the future
		int connectionDelay = MadnetSim.config.getNodeApConnectionDelay();
		AccessPoint ap = node.getClosestApInRange();
		if (ap != null) {
			Request nodeRequest = node.getCurrentRequest(); // returns the previously prepared request
			if (nodeRequest != null && 
					nodeRequest.getPercentComplete() < 100 &&
					!node.isWaitingFirstTransfer() &&
					ap.hasContent(node.getRequestedContent()) && 
					!eventList.isNodeConnectedArleadyQueued(node) && 
					!eventList.isNodeGotDataArleadyQueued(node) && 
					!eventList.isNodeRequestExpiryAreadyQueued(node)){
				Pair<Node,AccessPoint> eventArgs = new Pair<Node,AccessPoint>(node, ap);
				Event connEvt = new Event(EEventKind.NODE_CONNECTED,event.getTime() + connectionDelay, eventArgs);
				eventList.add(connEvt);
			}
		}
	}

	
	private void handleNodeRequestIssued(Event event) {
		// Data contains the node who issued the request
		Node node = (Node) event.getEventData();
		node.getCallbackReceiver().cbRequestCommitted();
		int eventTime = event.getTime();
		
		int requestDelay = MadnetSim.config.getContentRequestDelay(); // Time needed for the request to reach the Base Station
																 // should be set according to the base station signal strength?
		Content requestContent = null;
		if (!MadnetSim.config.getIsUploadCase()) {
			requestContent = node.getReadRequestContent();
			if (requestContent == null)
				requestContent = new Content(MadnetSim.config.getFixedRequestSize());
		} else {
				requestContent = new Content(node.getLastVisitedPoiSize());
		}
		TimedPoint predictedPos = node.getForeseenPosition();
		if (predictedPos != null) { // TOFIX The node knows when it wants to receive the data... 
			int predictedTime = predictedPos.getTime();
			node.prepareRequest(requestContent, predictedTime, eventTime, (Point) predictedPos);   // the node stores the incomplete request 
																							       // of data it wishes to receive
			// Schedule new event
			Object[] eventData = new Object[4];
			eventData[0] = (Point) predictedPos;
			eventData[1] = node.getRequestedContent();
			eventData[2] = node;
			eventData[3] = predictedTime; 
			Event evt = new Event(EEventKind.NODE_REQ_RECEIVED, eventTime+requestDelay, eventData);
			eventList.add(evt);
		}
	}
	
	private void handleNodeRequestReceived(Event event) {
		// Event Data contain an Object array <(dest)Point>,<Content>,<Node>,<(arrivalTime)Integer>
		Object[] eventData = (Object[]) event.getEventData();
		Point destPoint =  (Point) eventData[0];
		Content requestContent = (Content) eventData[1];
		Node node = (Node) eventData[2];
		Integer foreseenTime = (Integer) eventData[3];
		

		/* ----------- BRIEF EXPLANATION OF NODE REQUEST ISSUE --------
		 * To explain how the node request is modelled by the simulator we should look before
		 * how the content request between the access point and the node truly happens:
		 * 
		 * REALITY:
		 *   The NODE issues a request, the request is received by the BASE STATION, data is delivered
		 *   simultaneously to the NODE (ap list) and the APs (content).
		 * 
		 *   IF the NODE reaches its destination (lookahead time), the request is said to be "EXPIRED".
		 *   When this happens there is no problem because the request the node issued to the BASE STATION 
		 *   includes the final position and lookahead time. In other words, as such information as well as
		 *   the global time is known by the BASE STATION, no data is sent over 3G/wired/wireless network.
		 * 
		 * SIMULATION:
		 *   The simulated counterpart of event request is the following:
		 *   T1 - Request is prepared. Request content is created and allocated. 
		 *        (the information is in the node) 
		 *   T1 - REQUEST_RECEIVED event is scheduled in the future. 
		 *        (this models the time needed for the request to be received)
		 *   T2 - On REQUEST_RECEIVED (this point) the node commits the request, and BASE STATION object 
		 *        receives it.
		 *   
		 *   Between T1 and T2, it could happen that the node moves and request EXPIRES. In this case we
		 *   simply do not commit the request (as the NODE and its BASE STATION have previously dialed).
		 */
		Request currentRequest = node.getCurrentRequest();
		if (currentRequest != null) {                    // This happens if the request is expired
			node.commitRequest(foreseenTime, destPoint);	// The content is requested to the BASE STATION
	
			// Schedule the event containing the instructions
			int baseStationCommunicationDelay = MadnetSim.config.getContentResponseDelay();  // ResponseDelay: time required to get the instruction from the BASE STATION
			BaseStation baseStation = node.getBaseStation();
			List<AccessPoint> suggestedAps = baseStation.getInstructions(node);
			int apListSize = suggestedAps.size();
			if (apListSize > 0) {
				Pair<Node, List<AccessPoint>> evtParams = new Pair<Node, List<AccessPoint>>(node, suggestedAps); 
				Event nodeEvt = new Event(EEventKind.NODE_GOTDATA,event.getTime()+baseStationCommunicationDelay,evtParams);
				eventList.add(nodeEvt);
			}
		
			// Schedule received data through 3G
			if (MadnetSim.config.getIs3GEnabled()) {
				Pair<Integer,Point> nodeStopPair = node.getSecondsInRange(baseStation, event.getTime(),event.getTime()+Simulator.KDeltaT);
				int nodeStopSeconds = nodeStopPair.fst;
				long downloadBandwidth = baseStation.getBandwidth(node);
				long dataAmount = nodeStopSeconds * downloadBandwidth;
				Pair<BaseStation, Long> evtSubData = new Pair<BaseStation,Long>(baseStation, dataAmount); 
				Pair<Node,Pair<BaseStation, Long>> evtData = new Pair<Node,Pair<BaseStation,Long>>(node,evtSubData);
				Event bsEvt = new Event(EEventKind.NODE_GOTDATA, event.getTime()+nodeStopSeconds+baseStationCommunicationDelay, evtData);
				eventList.add(bsEvt);
			}
			
			// Schedule the event to deliver content to access points
			int apCommunicationDelay;
			for (int aa = 0; aa < apListSize; aa++) {
				apCommunicationDelay = MadnetSim.config.getContentDeliveryDelay();  // Time to transfer the data through backbone
				AccessPoint ap = suggestedAps.get(aa);
				if (!ap.hasContent(requestContent)) {
					Pair<AccessPoint, Content> apEventData = new Pair<AccessPoint,Content>(ap,requestContent);
					if (!eventList.isApGotDataScheduled(ap,requestContent) && 
						   !ap.hasContent(requestContent)) { // Check that the content is not already in the AP
							
						Event apEvt = new Event(EEventKind.AP_GOTDATA,event.getTime()+apCommunicationDelay,apEventData);
						eventList.add(apEvt);
					}
				}
			}
		}
	}
	
	private void handleNodeConnected(Event event) throws ContentUnavailableException {
		Pair<Node,AccessPoint> evtArgs = (Pair<Node,AccessPoint>) event.getEventData();
		Node node = evtArgs.fst;
		// Schedule the first transfer by connecting to the selected AP (was in range before the connection)
		AccessPoint ap = evtArgs.snd;
		if (node.isApInRange(ap)) {
			// Connect the node
			ap.connectNode(node);
			
			// Take the next expiry event into account
			boolean eventSchedulePriority = false;
			Pair<Integer,Point> nodeStop;
    		Event nextExpiryEvent = eventList.getNextRequestExpiry(node);
    		if (nextExpiryEvent != null) {
    			nodeStop = calculateNodeStopPair(node, ap, event.getTime(), nextExpiryEvent.getTime());
    			eventSchedulePriority = true;
    		} else {
    			nodeStop = calculateNodeStopPair(node, ap, event.getTime());
    		}
			
    		// Try to get some bandwidth
			int nodeStopSeconds = nodeStop.fst;
			float nodeApDistance = (float)Distance.distance(node, ap);
    		long apBw = ap.requestBandwidth(nodeApDistance,node);
    		if (apBw < 0) {
    			/* 
    			 * {CALLBACK} The node has been cut out. 
    			 */
    			node.getCallbackReceiver().cbNodeCutout();
    			if (ap.isConnected(node))
	    			ap.disconnectNode(node);
    			
    		} else {
    			// The node is allowed to download
    			long dataAmount = (nodeStopSeconds * apBw);
    		    		
				// Before scheduling the new event check if the requested 
				// content is available into the access point
				// The transfer should be scheduled only if it's the first one (and not already scheduled)
				if (!eventList.isNodeGotDataArleadyQueued(node)) {
					Content nodeRequestContent = node.getRequestedContent();
					if (ap.hasContent(nodeRequestContent)) {  
						Request nodeRequest = node.getCurrentRequest();
						if (nodeRequest != null) { // Check the upload procedure has not canceled the request.
							ERequestStatus requestStatus = nodeRequest.getRequestStatus();
							if ( (requestStatus == ERequestStatus.REQ_INACTIVE  && nodeRequest.getPercentComplete() == 0)   ||
	//							 (requestStatus == ERequestStatus.REQ_INCOMPLETE  && nodeRequest.getPercentComplete() < 100) ) {
								 (nodeRequest.getPercentComplete() < 100) ) {
									int completionTime = event.getTime() + nodeStopSeconds;
									Pair<AccessPoint, Long> evtSubData = new Pair<AccessPoint, Long>(ap, dataAmount);
									Pair<Node,Pair<AccessPoint, Long>> evtData = new Pair<Node,Pair<AccessPoint, Long>>(node,evtSubData);
									Event evt = new Event(EEventKind.NODE_GOTDATA, completionTime, evtData);
									eventList.add(evt,eventSchedulePriority);
								}
							else { // No event was scheduled
								if (ap.isConnected(node))
									ap.disconnectNode(node);
							}
						} else {
							// (UPLOAD CANCELED THE REQUEST, because completed through 3G)
			    			node.removeOutOfRangeAps();
						}
					} else {
						/*
						 * {CALLBACK} Requested data is no longer available by the AccessPoint.
						 */
						node.getCallbackReceiver().cbDataNotAvailable();
						if (ap.isConnected(node))
							ap.disconnectNode(node);
					}
				}
    		}
		} else { 
			/*
			 *  {CALLBACK} The AP the node wants to connect to is no more 
			 *  in range! for example because connected too late (when it 
			 *  moved and the AP is out of range).
			 */
			node.getCallbackReceiver().cbApNoMoreInRange();
		}
	}

	private void handleNodeGotData(Event event) {
		Object eventData = event.getEventData();
		if (eventData instanceof Pair) {
			Pair<Node,Object> nodeData = (Pair<Node,Object>) eventData;
			Node node = nodeData.fst;
			Object second = nodeData.snd;
			
			if (second instanceof List) {         // The node receives the list of access points
				if (node.getCurrentRequest() == null) {
					/* 
					 * {CALLBACK} The node received access point list is ignored because 
					 * they were sent too late (after the request expiration).
					 */
					node.getCallbackReceiver().cbDataIgnored();
				} else {
					List<AccessPoint> apList = (List<AccessPoint>) second;
					node.setApList(apList);
				}
			} else if (second instanceof Pair) {  // The node receives some data
			
				Long amountDataReceived = ((Pair<Object,Long>) second).snd;
			    Object sender = ((Pair<Object,Long>)second).fst;
				/* WARNING! The node could have its request set to EXPIRED (closed) after a movement).
				          In this case the received data should be ignored (TODO, maybe we need an ACK
				          mechanism internal to the simulator in order to validate the data sent by a
				          base station). 
				 */
				if (node.getCurrentRequest() == null) {
					/* 
					 * {CALLBACK} The received amount of data is ignored because 
					 * they were sent too late (after the request expiration).
					 */
					 node.getCallbackReceiver().cbDataIgnored();
				} else {
				    /// *** Now process according to the sender
					if (sender instanceof AccessPoint) {
						AccessPoint ap = ((AccessPoint) sender);
						node.dataReceivedFromAp(amountDataReceived,ap,event.getTime());
				    	node.getCallbackReceiver().cbWifiDataReceived(amountDataReceived);
					    // Schedule another transfer if needed
					    // WARNING! the current time is now "time" and the request could have a lower time
					    // WARNING! another GOT_DATA should not be scheduled if there is already one in the event list,
					    //   because we have to wait ALL the scheduled transfer to complete!
					    // WARNING! avoid to send other data after the disconnection
					    if (!eventList.isNodeGotDataArleadyQueued(node))  {   
						    Request nodeReq = node.getCurrentRequest();
						    
						    // The *upload* request was satisfied with this 
						    // last transfer. (The request was reset)
						    if (nodeReq == null) {
						    	// Request completed (100%) after this last download. 
				    			if (ap != null && ap.isConnected(node)) {
				    				// The AP the data were received from is in range
				    				ap.disconnectNode(node);
				    			}
						    } else {
						    
							    Content nodeReqContent = nodeReq.getContent();
							    
							    if (nodeReq.getPercentComplete() < 100) {
							    	if( ap == null ) { 
							    		// The AP who sent the data is not in range now, therefore set the request
							    		// as incomplete and hope another access point with the same content will 
							    		// be in range.
							    		node.setRequestStatus(ERequestStatus.REQ_INCOMPLETE);
							    		
							    	} else if (ap.hasContent(nodeReqContent)) {
							    		Pair<Integer,Point> nodeOutOfRangeInfo;
							    		
							    		// Take the next expiry event into account
							    		Event nextExpiryEvent = eventList.getNextRequestExpiry(node);
							    		boolean eventSchedulePriority = false; // Process these BEFORE connection Expired (that erases the access point list);
							    		if (nextExpiryEvent != null) {
							    			nodeOutOfRangeInfo = calculateNodeStopPair(node, ap, event.getTime(), nextExpiryEvent.getTime());
							    			eventSchedulePriority = true;
							    		} else {
							    			nodeOutOfRangeInfo = calculateNodeStopPair(node, ap, event.getTime());
							    		}
							    	    int nodeStopSeconds = nodeOutOfRangeInfo.fst;
								    	
								    	if (nodeStopSeconds > 0) { 
								    		float nodeApDistance = (float)Distance.distance(node, ap);
								    		long apBw = ap.requestBandwidth(nodeApDistance,node);
								    		if (apBw < 0) {
								    			/*
								    			 *  {CALLBACK} The node has been cut out. 
								    			 */
								    			node.getCallbackReceiver().cbNodeCutout();
								    			if (ap.isConnected(node))
									    			ap.disconnectNode(node);
								    		} else {
								    			// The node is allowed to download
								    			long dataAmount = (nodeStopSeconds * apBw);
								    		
									    		// WARINING! The node can receive more data than required, so let's set the correct amount!
										    	long remainingDataAmount = nodeReqContent.getSize() - nodeReq.getBytesReceived();
										    	
										    	// WARNING! The node request could have been fully satisfied with the last transfer!
										    	if (remainingDataAmount <= 0) {  
										    			// request satisfied (100%)
										    			if (ap.isConnected(node))
											    			ap.disconnectNode(node);
										    			node.updateRequestStatus(false);
									    			
									    		} else {  
									    			// request not satisfied
									    			if (dataAmount > remainingDataAmount) { // apply the correction
											    		dataAmount = remainingDataAmount;
											    		nodeStopSeconds = (int) (nodeStopSeconds / dataAmount * remainingDataAmount);
											    	}
											    	int evtScheduledTime = event.getTime() + nodeStopSeconds;
											    	
										    		// Make node receive the data
											    	Pair<AccessPoint, Long> evtSubData = new Pair<AccessPoint, Long>(ap, dataAmount);
											    	Pair<Node, Pair<AccessPoint, Long>> evtData = new Pair<Node, Pair<AccessPoint, Long>>(node,evtSubData);
											    	Event nodeGotDataEvt = new Event(EEventKind.NODE_GOTDATA,evtScheduledTime,evtData);
											    	eventList.add(nodeGotDataEvt, eventSchedulePriority);
										    	}
								    		}
								    	} else {  // Will not spend seconds in range of this ap
						    				if (ap.isConnected(node))
						    					ap.disconnectNode(node);
						    				node.updateRequestStatus(false);
							    		}
								    }
							    } else {  // Request completed (100%) after this last download. 
					    			if (ap != null && ap.isConnected(node)) {
					    				// The AP the data were received from is in range
					    				ap.disconnectNode(node);
					    			}
					    			node.updateRequestStatus(false);
							    }
						    }
						}
					} else if (sender instanceof BaseStation) { // The data are sent by the base station
						node.dataReceivedFromBs(amountDataReceived, null, event.getTime());
						node.getCallbackReceiver().cb3GDataReceived(amountDataReceived);
						
						// Schedule another transfer if the request is not complete
						Request nodeReq = node.getCurrentRequest();
						if (nodeReq == null) {
							// The request is expired (no need to send more data)
			    			/// TODO disconnect the node from the downloading queue of the BS 
						} else {
							Content nodeReqContent = nodeReq.getContent();
							long remainingDataAmount = nodeReqContent.getSize() - nodeReq.getBytesReceived();
					    	
					    	// WARNING! The node request could have been fully satisfied with the last transfer!
					    	if (remainingDataAmount <= 0) {  // request satisfied (100%)
				    			/// Disconnect the node from an eventual access point 
					    		AccessPoint ap = node.getLastDownloadAp();
					    		if (ap != null && ap.isConnected(node)) {
				    				ap.disconnectNode(node);
				    			}
				    			 node.updateRequestStatus(false);
					    	} else {
					    		if (!eventList.isNodeRequestExpiryAreadyQueued(node)) {
						    		// Calculate and schedule another gotdata
									BaseStation baseStation = node.getBaseStation();
									Pair<Integer,Point> nodeStopPair = node.getSecondsInRange(baseStation, event.getTime(),event.getTime()+Simulator.KDeltaT);
									int nodeStopSeconds = nodeStopPair.fst;
									if (nodeStopSeconds == 0) { // The node will not spend future seconds in this cell but
																// let the transfer continue in the next cell
										nodeStopSeconds = 1;
									}
									long downloadBandwidth = baseStation.getBandwidth(node);
									long dataAmount = nodeStopSeconds * downloadBandwidth;
									Pair<BaseStation, Long> evtSubData = new Pair<BaseStation,Long>(baseStation, dataAmount); 
									Pair<Node,Pair<BaseStation, Long>> evtData = new Pair<Node,Pair<BaseStation,Long>>(node,evtSubData);
									Event bsEvt = new Event(EEventKind.NODE_GOTDATA, event.getTime()+nodeStopSeconds, evtData);
									eventList.add(bsEvt);
					    		}
					    	}
						}
					}
				}
			}
	    }
	}
	private void handleNodeRequestExpired (Event event) {
		// Event data contains the node which request is now expired
		Node node = (Node) event.getEventData();
		
 		if (MadnetSim.config.getPrintExpiredTimesize()) {
 			Request r = node.getCurrentRequest();
 			if (r != null)
 				System.out.println("EXPBYTES: " + r.getBytesReceived() + " Bytes in " +  (Simulator.time - r.getPreparedAt()) + " Seconds");
 			else
 				System.out.println("NOREQ at " + time);
 		}
 		
 		// Clear AP List and the request
 		node.resetRequest();
 	}
	private void handleApGotData(Event event) {
		Object eventData = event.getEventData();
		if (eventData instanceof Pair) {
			Pair<AccessPoint,Content> apContent = (Pair<AccessPoint,Content>) eventData;
			AccessPoint ap = apContent.fst;
			Content content = apContent.snd;
			ap.contentTransferred(content);
		}
	}

	private Pair<Integer,Point> calculateNodeStopPair(Node aNode, AccessPoint aAp, int aFromTime) {
		return calculateNodeStopPair(aNode,aAp,aFromTime,aFromTime+KDeltaT);
	}
	private Pair<Integer,Point> calculateNodeStopPair(Node aNode, AccessPoint aAp, int aFromTime, int aProposedToTime) {
		Pair<Integer,Point> nodeStop;
		
		// !! WARNING, if a connect (of another node) is scheduled we limit this computation to that time
		int nextConnectTime = eventList.getNextConnectTime();
		int secondsTillNextConnectEvent = nextConnectTime - aFromTime;

		// !! WARNING, we can have simulator deltas greater than a scheduled movement.
		// This should be an upper bound of the computation
		Event nextNodeMove = eventList.getNextNodeMoveEvent(aNode);
		int secondsTillNextNodeMove = -1;
		if (nextNodeMove != null)
			secondsTillNextNodeMove = nextNodeMove.getTime();

		// Determine the upperbound
		int computationUpperBound = secondsTillNextNodeMove;
		if (secondsTillNextConnectEvent > 0) {
			computationUpperBound =   aFromTime +  ( secondsTillNextNodeMove < secondsTillNextConnectEvent ? secondsTillNextNodeMove : secondsTillNextConnectEvent);		
		}
		if (computationUpperBound > aProposedToTime)
			computationUpperBound = aProposedToTime;
		
		if (computationUpperBound > 0) {
			nodeStop = aNode.getSecondsInRange(aAp, aFromTime, computationUpperBound);
		} else {
			nodeStop = aNode.getSecondsInRange(aAp, aFromTime, aFromTime + KDeltaT); // takes the simulator delta as maximum
		}
		return nodeStop;
	}

	private void printStatus() {
		int nodeListSize = nodeList.size();
		int computedPos = 0;
		double distance = 0;
		for (int k=0; k < nodeListSize; k++) {
			Node n = nodeList.get(k);
			Point previousPos = n.getPreviousPosition();
			Point currentPos = (Point) n;
			if (previousPos != null) {	
				distance += Distance.distance(previousPos, currentPos);
				computedPos+=1;
			}
		}
		System.out.println("\n------- Time:" + time + " --------");
		eventList.print();
		for (int ii=0;ii<nodeList.size(); ii++) {
			Node n = nodeList.get(ii);
			TimedPoint fpos = n.getForeseenPosition();
			
			// Current Request
			String requestText;
			Request req = n.getCurrentRequest();
			if (req == null) {
				requestText = "none issued ";
			} else {
				String reqStatusText = null;
				ERequestStatus reqStatus = req.getRequestStatus();
				if (reqStatus == ERequestStatus.REQ_INACTIVE) 
					reqStatusText = "INACTIVE";
				if (reqStatus == ERequestStatus.REQ_INCOMPLETE)
					reqStatusText = "INCOMPLETE";
				if (reqStatus == ERequestStatus.REQ_TRANSFERRING)
					reqStatusText = "TRANSFERRING";
				
				requestText = reqStatusText + "(%: " + n.getCurrentRequest().getPercentComplete() + ")";
			}
			if (fpos == null)
				System.out.println(n.getId() + " Pos: (" + n.getLon() + "," + n.getLat() + ") " + requestText + " No Foresee");
			else
				System.out.println(n.getId() + " Pos: (" + n.getLon() + "," + n.getLat() + ") " + requestText + " Foreseen: t" + (fpos.getTime()) + " p(" + fpos.getLon() + "," + fpos.getLat() + ")");
			System.out.println("------------------------");
		}
	}
}
