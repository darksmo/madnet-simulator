package de.saviodimatteo.madnetsim.simulation.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import de.saviodimatteo.madnetsim.MadnetSim;
import de.saviodimatteo.madnetsim.actors.AccessPoint;
import de.saviodimatteo.madnetsim.actors.Node;
import de.saviodimatteo.madnetsim.actors.NodeCallbackReceiver;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.simulation.Simulator;
import de.saviodimatteo.madnetsim.utils.Distance;
import de.saviodimatteo.madnetsim.utils.Pair;


public class Statistics {
	// Knowledge of the previous state
	int iNodesCount;
	
	// Unhappiness
	HashMap<Point,Integer> iUnhappinessPoints;
	// 0.05 -> 25m diameter 12.5 radius
	private final double KUnhappinessSoil = 0.0375; // 75m diameter; SMOMETERS <-- 0.15 : 300 = X : METERS
		
	// Global Statistics over time
	private static int KGlobalStatsArrayTimeSize;
	private final int KGlobalStatisticsCount = 2;
	private final int KApBandwidthUsageField = 0;        // long
	private final int KUsedApsField = 1;                 // int
	
	private Object[][] iGlobalStatistics;
	
	// Individual Statistics over time
	public static int KNodeStatsArrayTimeSize;
	private int iTimeArrayDelta;
	private final int KNodeStatsCount = 8;
	public static final int KSatisfiedRequestsField = 0; // int
	public static final int KIssuedRequestsField = 1;    // int
	public static final int KDataUnavailableField = 2;   // int
	public static final int KCutoutsField = 3;           // int
	public static final int KApOutOfRangeField = 4;      // int
	public static final int KDataIgnoredField = 5;       // int
	public static final int K3GDataReceivedField = 6;    // long
	public static final int KWIFIDataReceivedField = 7;  // long
	
	private long[][][] iIndividualNodeStatistics;
	
	// Directories
	private String iStatisticsDir;
	
	public Statistics( int aNumNodes ) {
		iNodesCount = aNumNodes;
		
		iTimeArrayDelta = 0;
		int simulationEndTime = Simulator.endTime;
		KNodeStatsArrayTimeSize = (int) (simulationEndTime / MadnetSim.config.getStatisticsNumDumps());
		KGlobalStatsArrayTimeSize = KNodeStatsArrayTimeSize;
		
		// iIndividualNodeStatistics = new long[aNumNodes][KNodeStatsCount][KNodeStatsArrayTimeSize];
		iIndividualNodeStatistics = new long[1][KNodeStatsCount][KNodeStatsArrayTimeSize];
		iGlobalStatistics = new Object[KGlobalStatisticsCount][KGlobalStatsArrayTimeSize];
		initialiseIndividualNodeArray();
		initialiseGlobalStatsArray();
		iUnhappinessPoints = new HashMap<Point,Integer>();
	}
	
	public void ensureStatisticsDirExists() {
		// Create the directory for statistics
		iStatisticsDir = "results.DeltaT_" + Simulator.KDeltaT + "APusage_" + MadnetSim.config.getApUsageThreshold() + "_" + String.valueOf(MadnetSim.config.hashCode());
		
		try {
			File directory = new File(iStatisticsDir);
			directory.mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void updateGlobalStats(List<AccessPoint> aApList) {
		// Prepare
		int arrayTime = (Simulator.time-iTimeArrayDelta)/Simulator.KDeltaT;
		int usedAps = 0;
		
		// Record
		for (AccessPoint ap : aApList) {
			long usedBandwidth = ap.getBandwidth() - ap.getAvailableBandwidth();
			iGlobalStatistics[KApBandwidthUsageField][arrayTime] = ((Long) iGlobalStatistics[KApBandwidthUsageField][arrayTime]) + new Long(usedBandwidth);
			if (usedBandwidth > 0) {
				usedAps++;
			}
		}
		iGlobalStatistics[KUsedApsField][arrayTime] = usedAps;
	}
	public void updateIndividualNodeStats(List<Node> aNodeList) {
		int arrayTime = (Simulator.time-iTimeArrayDelta)/Simulator.KDeltaT;
		for (Node node : aNodeList) {
			// Prepare
			int nodeNumber = node.getNumber();
			NodeCallbackReceiver ncb = node.getCallbackReceiver();
			
			// Record
//			updateDataReceived(node);
//			iIndividualNodeStatistics[nodeNumber][K3GDataReceivedField][arrayTime] = ncb.getNum3GDataReceived();
//			iIndividualNodeStatistics[nodeNumber][KWIFIDataReceivedField][arrayTime] = ncb.getNumWifiDataReceived();
//			iIndividualNodeStatistics[nodeNumber][KSatisfiedRequestsField][arrayTime] = ncb.getSatisfiedRequestsCount();
//			iIndividualNodeStatistics[nodeNumber][KIssuedRequestsField][arrayTime] = ncb.getRequestedIssuedCount();
//			iIndividualNodeStatistics[nodeNumber][KApOutOfRangeField][arrayTime] = ncb.getApNoMoreInRangeCount();
//			iIndividualNodeStatistics[nodeNumber][KCutoutsField][arrayTime] = ncb.getCutoutsCount();
//			iIndividualNodeStatistics[nodeNumber][KDataUnavailableField][arrayTime] = ncb.getDataNotAvailableInApCount();
//			iIndividualNodeStatistics[nodeNumber][KDataIgnoredField][arrayTime] = ncb.getDataIgnoredCount();

			iIndividualNodeStatistics[0][K3GDataReceivedField][arrayTime] += ncb.getNum3GDataReceived();
			iIndividualNodeStatistics[0][KWIFIDataReceivedField][arrayTime] += ncb.getNumWifiDataReceived();
			iIndividualNodeStatistics[0][KSatisfiedRequestsField][arrayTime] += ncb.getSatisfiedRequestsCount();
			iIndividualNodeStatistics[0][KIssuedRequestsField][arrayTime] += ncb.getRequestedIssuedCount();
			iIndividualNodeStatistics[0][KApOutOfRangeField][arrayTime] += ncb.getApNoMoreInRangeCount();
			iIndividualNodeStatistics[0][KCutoutsField][arrayTime] += ncb.getCutoutsCount();
			iIndividualNodeStatistics[0][KDataUnavailableField][arrayTime] += ncb.getDataNotAvailableInApCount();
			iIndividualNodeStatistics[0][KDataIgnoredField][arrayTime] += ncb.getDataIgnoredCount();
		}
	}
//	private void updateDataReceived(Node aNode) {
//		Pair<Integer,Long> receivedBytes = aNode.getLastBytesReceived();
//		int nodeNumber = aNode.getNumber();
//		int time = receivedBytes.fst;
//		long numBytes = receivedBytes.snd;
//		if (time == Simulator.time) {
//		}
//	}
	
	public void recordUnhappyNodesPosition(List<Node> nodeList) {
		for (Node n : nodeList) {
			if (!n.isHappy()) {
				Point nPos = (Point) n;
				Point lastUnPos = null;
				
				boolean found = false;
				for (Point unPos : iUnhappinessPoints.keySet()) {
					double distance = Distance.distance(nPos,unPos);
					if ( distance < KUnhappinessSoil) {
						found = true;
						lastUnPos = unPos;
						break;
					}
				}
				if (!found) {
					iUnhappinessPoints.put(new Point(nPos), 1);
					
				} else if (lastUnPos != null) {
					int unCount = iUnhappinessPoints.get(lastUnPos);
					iUnhappinessPoints.put(lastUnPos, unCount+1);
					
				} else
					System.out.println("WARNING: UnHappinessFail!");
				
				n.setHappy();
			}
		}
	}
	public void dumpDelayHistogram(List<Node> aNodeList) {
		// Merge all the histograms
		HashMap<Integer,Integer> aDelayHistogram = new HashMap<Integer,Integer>();
		for (Node n : aNodeList) {
			NodeCallbackReceiver ncb = n.getCallbackReceiver();
			HashMap<Integer,Integer> nhist = ncb.getDelaysHistogram();
			for (Integer k : nhist.keySet()) {
				int v = nhist.get(k);
				if (aDelayHistogram.containsKey(k)) 
					aDelayHistogram.put(k,aDelayHistogram.get(k) + v);
				else
					aDelayHistogram.put(k,v);
			}
		}
		
		// Dump the global histogram
		try {
			FileWriter fileStream = new FileWriter(iStatisticsDir + "/delays_hist.out", false); 
			BufferedWriter fileWriter = new BufferedWriter(fileStream);
			for (int key : aDelayHistogram.keySet()) {
				int val = aDelayHistogram.get(key);
				fileWriter.write(key  + " " + val + "\n");				
			}
			fileWriter.flush();
			fileWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void dumpPartialStats() {
		System.out.println("Dumping Partial Statistics...");
		
		// --- Global Statistics ---
		long usedBandwidth;
		int usedAps;
		try {
			boolean append = ( iTimeArrayDelta > 0 ? true : false);
			FileWriter fileStream = new FileWriter(iStatisticsDir + "/globals.out", append); 
			BufferedWriter fileWriter = new BufferedWriter(fileStream);
			for (int t=0; t < KNodeStatsArrayTimeSize; t++) {
				long actualTime = (iTimeArrayDelta + (t * Simulator.KDeltaT));
				usedBandwidth  = (long) ((Long)iGlobalStatistics[KApBandwidthUsageField][t]);
				usedAps        =  (int) ((Integer)iGlobalStatistics[KUsedApsField][t]);
				fileWriter.write(actualTime  + " " + 
						usedAps              + " " +
						usedBandwidth       + "\n");				
			}
			fileWriter.flush();
			fileWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// --- Individual Statistics ---
		long dataReceivedValWIFI;
		long dataReceivedVal3G;
		int numSatisfiedRequests;
		int numIssuedRequests;
		int numDataUnavailable;
		int numCutsOut;       
		int numApOutOfRange; 
		int numIgnoredData;		
		System.out.println("Dumping Statistics for " + iNodesCount + " nodes");
		//for (int nn=0; nn<iNodesCount; nn++) {
		{ int nn=0;
			try {
				boolean append = ( iTimeArrayDelta > 0 ? true : false);
				FileWriter fileStream = new FileWriter(iStatisticsDir + "/node"+nn+".out", append); 
				BufferedWriter fileWriter = new BufferedWriter(fileStream);
				
				for (int t=0; t < KNodeStatsArrayTimeSize; t++) {
					
					// Do not write useless information
					boolean write = true;
					/*int uselessValues = 0;
					for (int ss=0; ss<KNodeStatsCount;ss++) {
						if (iIndividualNodeStatistics[nn][ss][t] == -1 )
							uselessValues++;
					}
					if (uselessValues >= KNodeStatsCount)
						write = false;*/
					
					// Write the line
					if (write) {
						long actualTime = (iTimeArrayDelta + (t * Simulator.KDeltaT)); // 1
						dataReceivedValWIFI  = (long) iIndividualNodeStatistics[nn][KWIFIDataReceivedField][t]; // 2
						numSatisfiedRequests =  (int) iIndividualNodeStatistics[nn][KSatisfiedRequestsField][t];// 3
						numIssuedRequests    =  (int) iIndividualNodeStatistics[nn][KIssuedRequestsField][t];   // 4
						numDataUnavailable   =  (int) iIndividualNodeStatistics[nn][KDataUnavailableField][t];  // 5
						numCutsOut           =  (int) iIndividualNodeStatistics[nn][KCutoutsField][t];          // 6
						numApOutOfRange      =  (int) iIndividualNodeStatistics[nn][KApOutOfRangeField][t];     // 7
						numIgnoredData       =  (int) iIndividualNodeStatistics[nn][KDataIgnoredField][t];      // 8 
						dataReceivedVal3G    = (long) iIndividualNodeStatistics[nn][K3GDataReceivedField][t];   // 9
						
						fileWriter.write(actualTime  + " " + 
								dataReceivedValWIFI  + " " + 
								numSatisfiedRequests + " " + 
								numIssuedRequests    + " " +
								numDataUnavailable   + " " +
								numCutsOut           + " " +   
								numApOutOfRange      + " " + 
								numIgnoredData       + " " + 
								dataReceivedVal3G    + "\n");
					}
				}
				
				fileWriter.flush();
				fileWriter.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();

			} catch (IOException e) {
				e.printStackTrace();
			
			}
		}
		initialiseIndividualNodeArray(); // Clears the vector to -1...
		initialiseGlobalStatsArray();    // Clears to 0...
		iTimeArrayDelta = Simulator.time;
	}
	private void initialiseGlobalStatsArray() {
		// Initialise statistics
		for (int t = 0; t < KGlobalStatsArrayTimeSize; t++) {
			iGlobalStatistics[KApBandwidthUsageField][t] = new Long(0);
			iGlobalStatistics[KUsedApsField][t] = new Integer(0);
		}
	}
	private void initialiseIndividualNodeArray() {
		// Initialise statistics
		/*for (int nn=0;nn<iNodesCount;nn++)*/ { int nn=0;
			for (int t = 0; t < KNodeStatsArrayTimeSize; t++) {
				for (int ss=0; ss < KNodeStatsCount; ss++) {
					iIndividualNodeStatistics[nn][ss][t] = 0;
				}
			}
		}
	}
	public void dumpUnhappiness() {
		System.out.println("Dumping " + iUnhappinessPoints.keySet().size() + " unhappiness positions");
		try {
			boolean append = false;
			FileWriter fileStream = new FileWriter(iStatisticsDir + "/unhappiness.out", append); 
			BufferedWriter fileWriter = new BufferedWriter(fileStream);
			for (Point unPos : iUnhappinessPoints.keySet()) {
				fileWriter.write(iUnhappinessPoints.get(unPos) + " " + unPos.getLat() + " " + unPos.getLon() + " " + KUnhappinessSoil + " " + KUnhappinessSoil);
			}
			fileWriter.flush();
			fileWriter.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void dumpNodeList(List<Node> aNodeList) {
		System.out.println("Dumping Node List");
		try {
			boolean append = false;
			FileWriter fileStream = new FileWriter(iStatisticsDir + "/nodeList.out", append); 
			BufferedWriter fileWriter = new BufferedWriter(fileStream);

			for (Node n : aNodeList) {
				fileWriter.write(n.getId() + " = " + n.getNumber() + "\n");
			}
			
			fileWriter.flush();
			fileWriter.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void dumpConfigFile() {
		MadnetSim.config.printToFile(iStatisticsDir + "/config.out");
	}
}
