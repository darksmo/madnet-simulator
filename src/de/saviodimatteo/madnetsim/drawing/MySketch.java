package de.saviodimatteo.madnetsim.drawing;

import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import controlP5.ControlP5;
import controlP5.Textfield;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import de.saviodimatteo.madnetsim.MadnetSim;
import de.saviodimatteo.madnetsim.actors.AccessPoint;
import de.saviodimatteo.madnetsim.actors.BaseStation;
import de.saviodimatteo.madnetsim.actors.Node;
import de.saviodimatteo.madnetsim.data.Content;
import de.saviodimatteo.madnetsim.data.Poi;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.data.Request;
import de.saviodimatteo.madnetsim.data.Request.ERequestStatus;
import de.saviodimatteo.madnetsim.exceptions.InvalidDomainException;
import de.saviodimatteo.madnetsim.graph.Edge;
import de.saviodimatteo.madnetsim.graph.WeightedEdge;
import de.saviodimatteo.madnetsim.simulation.EEventKind;
import de.saviodimatteo.madnetsim.simulation.Event;
import de.saviodimatteo.madnetsim.simulation.EventQueue;
import de.saviodimatteo.madnetsim.simulation.ProcessingSimulator;
import de.saviodimatteo.madnetsim.simulation.statistics.Statistics;
import de.saviodimatteo.madnetsim.utils.Pair;
import edu.uci.ics.jung.graph.Graph;

public class MySketch extends PApplet {
	private static final long serialVersionUID = 1L;
	
	private final int KMapLeft = -850;
	private final int KMapTop = -300;
	private final int KButtonsMargin = 20;
	private final int KNumApCirclePoints = 8;
	
	private enum CHART_TYPE {
		LINES,
		DOTS
	}
	
	private enum MODE {
		SETUP_MAP_TEXT,
		SETUP_MAP,
		SETUP_INIT_TEXT,
		SETUP_INIT,
		SETUP_SIMULATOR_TEXT,
		SETUP_SIMULATOR,
		SIMULATION_STEP,
		SIMULATION_RUN,
		SIMULATION_END
	}
	
	MODE iMode;
	float scrTop, scrBottom, scrLeft, scrRight;
	float minLon, maxLon, minLat, maxLat;
	int lookY,lookX;
	int bgcolor = 255;
	ProcessingSimulator iSim;
	PFont timeFont, nodeIdFont, apIdFont, eventFont, centralTextFont, chartAxisFont;
	int KButtonsHeight = 34;
	AccessPoint iApInfo;
	
	// SIMULATION_STEP_MODE
	PImage mapImage;
	int mapX = 128;
	int mapY = 114;
	List<AccessPoint> deployedAps;
	List<Poi> poiToPlot;
	AccessPoint currentDeployedAp;
	Button btnRunSimulation;
	Button btnDumpApList;
	
	// MOUSE
	boolean swMoveMap = false;
	int mouseClickX, mouseClickY;
	int lastLookX, lastLookY;
	
	// Just a Point to plot and follow
	Float pointX = null;
	Float pointY = null;
	
	// ControlP5
	ControlP5 controlP5;
	Textfield txtDrawPoint;
	
	public void setup() {
		size(1200,1024);
		
		// ControlP5 stuff
		controlP5 = new ControlP5(this);
		txtDrawPoint = controlP5.addTextfield("plotlocation",(int) width-210,10,200,20);
		txtDrawPoint.setFocus(true);
		  
		// Screen Size
		scrLeft = 0;
		scrRight = width;
		scrBottom = height;
		scrTop = 60;
	
		// Minimum/Maximum Values
		lookX = 0;
		lookY = 0;
		minLat = 4178.9893f;
		minLon = 544.541f;
		maxLat = 4186.2246f;
		maxLon = 553.6387f;
		
		iApInfo = null;
		
		centralTextFont = createFont("Times",55);
		iMode = MODE.SETUP_MAP_TEXT;
		smooth();
	}
	
	public void draw() {
		background(255);
		
		if (iMode == MODE.SETUP_MAP_TEXT) {
			drawCentralText("Loading Map...");
			iMode = MODE.SETUP_MAP;
			
		} else if (iMode == MODE.SETUP_MAP) {
			// ST FRANCISCO MAP
			mapImage = loadImage("map.png");
			iMode = MODE.SETUP_INIT_TEXT;

		} else if (iMode == MODE.SETUP_INIT_TEXT) {
			drawCentralText("Creating Fonts and Buttons...");
				iMode = MODE.SETUP_INIT;

		} else if (iMode == MODE.SETUP_INIT) {
			// Text fonts
			timeFont = createFont("Times",40);
			nodeIdFont = createFont("SansSerif", 10);
			apIdFont = createFont("SansSerif", 10);
			eventFont = createFont("Courier", 8);
			chartAxisFont = createFont("SansSerif", 8);
			
			// SIMULATION_STEP
			btnRunSimulation = new Button(this,KButtonsMargin,KButtonsMargin/2,100,KButtonsHeight,"RUN");
			btnDumpApList = new Button(this,btnRunSimulation.w + 2*KButtonsMargin, KButtonsMargin/2, 120, KButtonsHeight, "DUMP APS");
			deployedAps = new Vector<AccessPoint>();
			poiToPlot = new Vector<Poi>();
				
			iMode = MODE.SETUP_SIMULATOR_TEXT;
		} else if (iMode == MODE.SETUP_SIMULATOR_TEXT) {
			drawCentralText("Preparing Simulator Engine...");
				iMode = MODE.SETUP_SIMULATOR;

		} else if (iMode == MODE.SETUP_SIMULATOR) {
			// Simulator
			iSim = new ProcessingSimulator(MadnetSim.config.getSimulationStartAt(),MadnetSim.config.getSimulationDelta());
			iMode = MODE.SIMULATION_STEP;

		} else if (iMode == MODE.SIMULATION_STEP || iMode == MODE.SIMULATION_RUN) {
			drawMap();
			
			stroke(0);
			fill(255,255,255,128);
			rect(0,0,width, KButtonsHeight + KButtonsMargin);
			
			drawBaseStations();
			drawAccessPoints();
			drawPois();

			// draw a big point on the screen
			if (pointX != null && pointY != null) {
				stroke(255,0,0);
				int x = (int) X(pointX.doubleValue());
				int y = (int) Y(pointY.doubleValue());
				drawX(x,y, 100);
				line(x,y,width/2,height/2);
			}
			drawNodes();
			if (iApInfo != null) {
				drawApInfo(iApInfo);
			}
			if (iSim.iCityGraph != null) {
				drawCityGraph(iSim.iCityGraph);
			}
			drawTime((int)scrRight/2,KButtonsMargin, color(0));
			
			// draw buttons
			btnRunSimulation.drawButton();
			btnDumpApList.drawButton();
			
			if (MadnetSim.config.getIsGraphicsCalibrateMapEnabled()) {
				// REF POINTS
				fill(0,255,0);
				stroke(0);			
				
				ellipse(X(550.65656625017118), Y(4184.9536725035814), 4,4);
				fill(255,0,0);
				ellipse(X(548.01471231636242), Y(4180.9522787716005), 4,4);
				fill(255,0,255);
				ellipse(X(552.80017937842933), Y(4179.8918867292196), 4,4);
			}
			
			if (currentDeployedAp != null) {
				drawAccessPoint(currentDeployedAp,true, false);
			}
			
			
			// SIMULATION_RUN
		    if (iMode == MODE.SIMULATION_RUN) {
				if (!iSim.doSimulationStep()) {
					iMode = MODE.SIMULATION_END;
				} else {
					drawSimProgressBar();
					drawCentralText("Simulating...");
					drawTime((int)scrRight/2,((int)scrBottom/2) + 20, color(25,189,240));
				}
		    }
		} else if (iMode == MODE.SIMULATION_END) {
			drawCentralText("Simulation Ended");
		}
	}
	private void drawApInfo(AccessPoint aAp) {
		rectMode(CORNERS);
		fill(255,255,255,128);
		stroke(255,255,255,0);
		rect(scrLeft+10,scrTop+20,scrLeft+200,scrBottom-20);
		
		textFont(apIdFont);
		text("Name:" + aAp.getId(),scrLeft+20, scrTop+40);
		text("Content:",scrLeft+25, scrTop+80);
	}
	public void drawCityGraph(Graph<Point,WeightedEdge> aGraph) {
		for (WeightedEdge e : aGraph.getEdges()) {
			edu.uci.ics.jung.graph.util.Pair<Point> ab = aGraph.getEndpoints(e);
			Point a = ab.getFirst();
			Point b = ab.getSecond();
			fill(0);
			stroke(0);
			line(X(a.getLon()),Y(a.getLat()),X(b.getLon()),Y(b.getLat()));
		}
		for (Point p : aGraph.getVertices()) {
			stroke(255,0,0);
			fill(128,128,0);
			ellipse(X(p.getLon()),Y(p.getLat()),2,2);
		}
	}
	public void keyPressed() {
		if (key == ' ') {
			iMode = MODE.SIMULATION_STEP;
			btnRunSimulation.label = "RUN";
			if (!iSim.doSimulationStep())
				noLoop();
		} else if (key == 'p') {
			
		}
		if (MadnetSim.config.getIsGraphicsCalibrateMapEnabled()) {
			float unit = 0.01f;
			if (key == 'z') {
				minLon -= unit;
				maxLon += unit;
				minLat -= unit;
				maxLat += unit;
			} else if (key == 'x') {
				minLon += unit;
				maxLon -= unit;
				minLat += unit;
				maxLat -= unit;
			} else if (key == 'a' ) {
				minLat-=unit;
			} else if (key == 'A') {
				minLat+=unit;
			} else if (key == 'q') {
				maxLat-=unit;
			} else if (key == 'Q') {
				maxLat+=unit;
			} else if (key == 'd') {
				maxLon+=unit;
			} else if (key == 'D') {
				maxLon-=unit;
			} else if (key == 's') {
				minLon-=unit;
			} else if (key == 'S') {
				minLon+=unit;
			} else if (key == '6') {
				lookY++;
			} else if (key == '7') {
				lookY--;
			}
			println("\nminLat = " + minLat + ";");
			println("minLon = " + minLon + ";");
			println("maxLat = " + maxLat + ";");
			println("maxLon = " + maxLon + ";");
			println("map():" + mapX + ", " + mapY);
		}

	}
	public void mouseDragged() {
		if (swMoveMap) {
		    lookX = lastLookX + (-1* (mouseClickX-mouseX));
			lookY = lastLookY + (-1* (mouseClickY-mouseY));
		} else if (currentDeployedAp != null) {
			cursor(CROSS);
			currentDeployedAp.setPosition(MLON(), MLAT());
		}
	}
	public void mouseMoved() {
//		if (iSim.apList != null) {
//			deployedAps = iSim.apList.getElementsAround(new Point(MLON(),MLAT()));
//		}
	}
	public void mousePressed() {
		if (mouseY > KButtonsHeight + KButtonsMargin )
		if (mouseButton == RIGHT) {
			currentDeployedAp = new AccessPoint(MLON(), MLAT());
			try {
				currentDeployedAp.setLinkRadius(0.1f,0.1f );
			} catch (InvalidDomainException e) {
				e.printStackTrace();
			}
		} else if (mouseButton == LEFT) {
			swMoveMap = true;
			mouseClickX = mouseX;
			mouseClickY = mouseY;
			lastLookX = lookX;
			lastLookY = lookY;
			// println(MLON() + " " + MLAT());
		}
	}
	public void mouseReleased()
	{
		if (currentDeployedAp != null) {
			deployedAps.add(currentDeployedAp);
			currentDeployedAp = null;
			cursor(ARROW);
		}
		swMoveMap = false;
		if (mouseButton == RIGHT) {
			txtDrawPoint.setText(MLON()+ "," + MLAT());
			println("(" + MLON() + ", " + MLAT() + ")");
		}
	}
	public void mouseClicked() {
		if (iMode == MODE.SIMULATION_STEP || iMode == MODE.SIMULATION_RUN) {
			// Run Simulation
			if (btnRunSimulation.clicked(mouseX, mouseY)) {
				if (iMode == MODE.SIMULATION_STEP) {
					iMode = MODE.SIMULATION_RUN;
					btnRunSimulation.label = "PAUSE";
				} else if (iMode == MODE.SIMULATION_RUN) {
					iMode = MODE.SIMULATION_STEP;
					btnRunSimulation.label = "RUN";
				}
			} else if (btnDumpApList.clicked(mouseX, mouseY)) {
				dumpApList();
			}
		}
	}
	private void drawMap() {
		image(mapImage, mapX + lookX + KMapLeft, mapY + lookY + KMapTop);
	}
	
	// ------------------ Draw BaseStations -------------------
	private void drawBaseStations() {
		float bsRay = iSim.bsList.get(0).getCellRadius(); 
		for (BaseStation bs : iSim.bsList) {
			double x = bs.getLon();
			double y = bs.getLat();
			stroke(0,0,255,128);
			float occupancy = map(bs.getConnectedNodes(),0, 2, 0,1);
			fill(lerpColor(color(0,0,255), color(255,0,0), occupancy),120);
			beginShape();
				vertex(X(x-bsRay), Y(y-bsRay));
				vertex(X(x+bsRay), Y(y-bsRay));
				vertex(X(x+bsRay), Y(y+bsRay));
				vertex(X(x-bsRay), Y(y+bsRay));
			endShape(CLOSE);
		}
	}
	// ------------------ Draw AccessPoints -------------------
	private void drawAccessPoints() {
		int apCount = iSim.apList.size();
		int depCount = deployedAps.size();
		for (int aa=0; aa < apCount; aa++) {
			AccessPoint a = iSim.apList.get(aa);
			if (  /*Distance.distance(a, iSim.nodeList.get(0)) > 0.2 ||*/
				  (X(a.getLon()) < scrLeft)  || 
				  (X(a.getLon()) > scrRight) || 
				  (Y(a.getLat()) > scrBottom)||
				  (Y(a.getLat()) < scrTop)   )
				{}
			else
				drawAccessPoint(a,false,true);
		}
		for (int aa=0; aa < depCount; aa++) {
			AccessPoint a = deployedAps.get(aa);
			drawAccessPoint(a,false, false);
		}
	}
	private void drawPois() {
		int poiCount = iSim.poiList.size();
		for (int pp=0; pp < poiCount; pp++) {
			Poi p = iSim.poiList.get(pp);
			drawPoi(p);
		}
	}
	private void drawAccessPoint(AccessPoint a, boolean transparency, boolean fixedAp) {
		double apLat = a.getLat();
		double apLon = a.getLon();
		int alpha = 255;
		// Ap ray
		double apRay = a.getLinkRadius().fst;
		noStroke();
		float apBandWidth = map( a.getAvailableBandwidth(), 0, a.getBandwidth(), 0 , 1);  

		stroke(218,125,37);
		if (transparency) {
			alpha = 80;
		}
		if (!fixedAp || a.equals(iApInfo)) 
			fill(0,0,255,alpha);
		else { 
			if (MadnetSim.config.getIsGraphicsApUsageEnabled()) {
				float apUsage = map(a.iUsage, iSim.iApMinimumUsage, iSim.iApMaximumUsage, 1, 0 );
				fill(lerpColor(color(255,0,0), color(0,255,0), apUsage),alpha);
			}
			else
				fill(lerpColor(color(255,0,0), color(255,255,0), apBandWidth),alpha);
		}
		beginShape();
		double increment = (2*Math.PI) / KNumApCirclePoints;
		double stopValue = 2*Math.PI + increment;
		for (double t=0; t <= stopValue; t+=increment) {
			if (t==0)
				curveVertex(X(apRay * Math.cos(t) + apLon),Y(apRay * Math.sin(t) + apLat));
			curveVertex(X(apRay * Math.cos(t) + apLon),Y(apRay * Math.sin(t) + apLat));
		}
		endShape(CLOSE);
		
		if (MadnetSim.config.getIsGraphicsAPInfoEnabled()) {
			// Ap id
			stroke(0);
			fill(0);
			textAlign(LEFT);
			textFont(apIdFont);
			text(a.getId(), X(apLon)-10,Y(apLat));
			
			// Ap content
			List<Content> lc = a.iCachedContents;
			if (lc != null) {
				int lcSize = lc.size();
				textFont(apIdFont);
				text("Content:",X(apLon)-20,Y(apLat)+32);
				for (int cc = 0; cc < lcSize; cc++) {
					Content c = lc.get(cc);
					text(c.getId(), X(apLon)-10,Y(apLat) + 52 + (cc*8));
				}
			}
			
			// Ap connected Nodes
			textFont(apIdFont);
			text("#Connections: " + a.iConnectedNodes.size(),X(apLon)-20,Y(apLat)+12);
		}
	}
	
	// ------------------ Draw Nodes -------------------
	private void drawNodes() {
		int nodeCount = iSim.nodeList.size();
		for (int nn=0; nn < nodeCount; nn++) {
			Node n = iSim.nodeList.get(nn);
			drawNodeRequestStatus(n);
			drawNodeApInfo(n);
			drawEventList(iSim.eventList);
		}
		for (int nn=0; nn < nodeCount; nn++) {
			Node n = iSim.nodeList.get(nn);
			if ( (X(n.getLon()) < scrLeft) || 
			      (X(n.getLon()) > scrRight) || 
			      (Y(n.getLat()) > scrBottom)  ||
			      (Y(n.getLat()) < scrTop))
			{ /* do not draw */ }
			else
				drawNode(n);
		}
	}
	private void drawPoi(Poi p) {
		int nodeDotSize = 2;
		// Node Dot
		stroke(color(71,33,106)); // violet
		fill(color(71,33,106)); // violet
		strokeWeight(1);
		ellipseMode(RADIUS);
		ellipse(X(p.getLon()),Y(p.getLat()),nodeDotSize,nodeDotSize);
	}
	private void drawNode(Node n) {
		int nodeDotSize = 4;
		// Node Label
		fill(0);
		stroke(0);
		textFont(nodeIdFont);
		text(n.getId(),X(n.getLon())+3,Y(n.getLat())+2);
		
		// Node Dot
		if (n.isWaitingFirstTransfer())
			stroke(255,0,0);
		else
			stroke(128);
		strokeWeight(1);
		fill(0,0,255,64);
		ellipseMode(RADIUS);
		ellipse(X(n.getLon()),Y(n.getLat()),nodeDotSize,nodeDotSize);
	}
	private void drawNodeApInfo(Node n) {
		if (n.iApList != null) {
			fill(255);
			stroke(0,0,0);
			int numAp = n.iApList.size();
			for (int ii=0;ii<numAp;ii++) {
				ellipse(X(n.getLon()) + (ii * 5) + 5, Y(n.getLat()) + 10, 4, 4);
			}
		}
	}
	private void drawNodeRequestStatus(Node n) {
		Request r = n.getCurrentRequest();
		if (r != null) {
			// Draw the outer square
			float x,y;
			x = X(n.getLon());
			y = Y(n.getLat());
			stroke(200);
			noFill();
			rectMode(CORNERS);
			rect(x,y-7,x+100,y-23);
			
			// Draw the inner square
			ERequestStatus reqStatus = r.getRequestStatus();
			if (reqStatus == ERequestStatus.REQ_INACTIVE)
				fill(120);
			else if (reqStatus == ERequestStatus.REQ_INCOMPLETE)
				fill(0,0,240);
			else if (reqStatus == ERequestStatus.REQ_TRANSFERRING)
				fill(0,240,0);
			double perc = r.getPercentComplete();
			rect(x,y-7,x+(float)perc,y-23);
			
			// Draw the end position (request expiration)
			double destLon = r.getDestLon();
			double destLat = r.getDestLat();
			noFill();
			stroke(200);
			strokeWeight(1);
			line(x,y,X(destLon),Y(destLat));
			drawX(X(destLon),Y(destLat),3);
			
			// Draw the content id
			Content c = n.getRequestedContent();
			if (c != null) {
				fill(200);
				textFont(nodeIdFont);
				text(c.getId() + " (" +  this.nfc((float)r.getPercentComplete(),2) + "%)",x+10,y-11);
			}
		}
	}
	private float XD (Double aLon) {
		float longitude = aLon.floatValue();
		float result = map( longitude, 0, maxLon, scrLeft, scrRight);
		return result;
	}
	private float YD (Double aLat) {
		float latitude = aLat.floatValue();
		float result = map( latitude, minLat, maxLat, scrTop, scrBottom);
		return result;
	}
	private float X (Double aLon) {
		float longitude = aLon.floatValue();
		float result = map( longitude, minLon, maxLon, scrLeft, scrRight);
		return result + lookX;
	}
	private float Y (Double aLat) {
		float latitude = aLat.floatValue();
		float result = map( latitude , minLat, maxLat, scrBottom, scrTop);
		return result + lookY;
		
	}
	private float MLAT() {
		return map(mouseY-lookY,scrBottom,scrTop,minLat,maxLat);
	}
	private float MLON() {
		return map(mouseX-lookX,scrLeft,scrRight,minLon,maxLon);
	}
	private void drawX(float x, float y, int size) {
		line(x-size, y-size, x+size, y+size);
		line(x-size, y+size, x+size, y-size);
	}
	
	private void drawTime(int x, int y, int c) {
		fill(c);
		stroke(c);
		int time = iSim.time;
		textFont(timeFont);
		textAlign(CENTER,CENTER);
		text("Time: " + (time - iSim.KDeltaT) + " - " + epochToGMT(iSim.time + (int)MadnetSim.config.getMinTime() - 28800), x, y+10);
	}
	
	// ------------------ Draw EventList ----------------------
	private void drawEventList(EventQueue el) {
		int elSize = el.size();
		boolean swXorCircle = true;
		for (int ii=0; ii < elSize; ii++) {
			Event e = el.get(ii);
			int crossSize = 3;
			EEventKind ek = e.getKind();
			Object ed = e.getEventData();
			// Will plot eventText and epos
			de.saviodimatteo.madnetsim.data.Point epos = null;
			Node n = null;
			fill(0,0,255);
			stroke(255,0,0);
			textFont(eventFont);
			if (ek == EEventKind.NODE_MOVE) {
				if (ed instanceof Node) {
					n = (Node) ed; 
				} else if (ed instanceof Pair) {
					n = (Node) (((Pair<Node,Object>) ed).fst) ;
				}
				stroke(100);
			} else if (ek == EEventKind.NODE_CONNECTED ) {
				n = ((Pair<Node,AccessPoint>) ed).fst;
				stroke(246,154,69);
			} else if (ek == EEventKind.NODE_REQ) {
				n = (Node) ed;
				stroke(0,0,255);
			} else if (ek == EEventKind.NODE_REQ_RECEIVED) {
				Object[] edArray = (Object[]) ed;
				n = (Node) edArray[2];
				stroke(0,128,255);
			} else if (ek == EEventKind.NODE_GOTDATA) {
				Pair<Node,Object> edData = (Pair<Node,Object>) ed;
				n = (Node) edData.fst;
				stroke(5,240,5);
				crossSize = 6;
			} else if (ek == EEventKind.AP_GOTDATA) {
				Pair<AccessPoint,Object> edPair = (Pair<AccessPoint, Object>) ed;
				AccessPoint ap = edPair.fst;
				epos = new de.saviodimatteo.madnetsim.data.Point(ap.getLon(),ap.getLat());
				stroke(5,240,200);
			}
			if (epos == null) {
				int et = e.getTime();
				epos = n.positionAtT(et);
			}
			if (epos != null) {
				strokeWeight(2);
				if (swXorCircle)
					drawX(X(epos.getLon()), Y(epos.getLat()),crossSize);
				else {
					noFill();
					ellipseMode(CENTER);
					ellipse(X(epos.getLon()),Y(epos.getLat()), crossSize+1, crossSize+1);
				}
			}
		}
	}
	
	// SIMULATION_END MODE
	private void drawSimProgressBar() {
		rectMode(CORNER);
		noFill();
		stroke(0,0,0);
		int barWidth = (int) scrRight/2;
		int barHeight = (int) scrBottom/16;
		int startX = barWidth/2;
		int startY = (int) scrBottom/2;
		rect(startX, startY,barWidth,barHeight);

		fill(1,74,107);
		noStroke();
		int currentTime = iSim.time;
		float percentComplete = map(currentTime, 0, iSim.endTime + iSim.KDeltaT, 0, barWidth);
		rect(startX+1, startY, percentComplete-1, barHeight);
	}
	private void drawSelectChartText() {
		fill(0,0,0);
		textFont(centralTextFont);
		textAlign(CENTER,CENTER);
		text("Please select a chart to plot", scrRight/2, scrBottom/2);
	}
	private void drawCentralText(String aText) {
		fill(0,0,0);
		textFont(centralTextFont);
		textAlign(CENTER,CENTER);
		text(aText, scrRight/2, scrBottom/2 - 60);
	}
	
	// UTILS
	String epochToGMT (int epoch) {
		return (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date ( ((long) epoch * 1000) - 21600 ))).toString();
	}
	private void dumpApList() {
		String folderPath = selectFolder("Where should I export the AP list?");
		if (folderPath != null) {
			PrintWriter writer = createWriter(folderPath + "/accesspoints.txt");
			for (AccessPoint ap: deployedAps) {
				Pair<Float,Float> linkRadius = ap.getLinkRadius();
				writer.println(ap.getLat() + " " + ap.getLon() + " " + linkRadius.fst + " " + linkRadius.snd);
			}
			writer.flush();
			writer.close();
		}
	}
	public void plotlocation(String theText) {
		String[] xy = theText.split(",");
		if (xy.length == 2) {
			pointX = Float.parseFloat(xy[0]);
			pointY = Float.parseFloat(xy[1]);
		}
	}

}

