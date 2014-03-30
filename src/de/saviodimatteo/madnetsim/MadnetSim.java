package de.saviodimatteo.madnetsim;

import processing.core.PApplet;
import de.saviodimatteo.madnetsim.config.ConfigReader;
import de.saviodimatteo.madnetsim.exceptions.ContentUnavailableException;
import de.saviodimatteo.madnetsim.simulation.Simulator;
import de.saviodimatteo.madnetsim.simulation.statistics.Statistics;

public class MadnetSim {

	public static ConfigReader config;
	public static void main(String[] argv) throws ContentUnavailableException {
		try {
			try {
				config = new ConfigReader("config/simulation.txt");
				if (argv.length > 0) {
					String simulationDeltaTime = argv[0];
					System.out.println("Config Overriden: SIMULATION_DELTA_TIME = " + simulationDeltaTime);
					config.addValue("SIMULATION_DELTA_TIME", simulationDeltaTime);
					if (argv.length > 1) {
						String accesspointThreshold = argv[1];
						System.out.println("Config Overridden: CONSIDER_AP_ABOVE_USAGE =" + accesspointThreshold);
						config.addValue("CONSIDER_AP_ABOVE_USAGE", accesspointThreshold);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (config.getIsGraphicalSimulation()) { // graphics
				PApplet.main(new String[] {"--present", "de.saviodimatteo.madnetsim.drawing.MySketch"});
			
			} else { // no graphics
				Simulator s = new Simulator(config.getSimulationDelta());
				System.out.println("MadnetSimulator " + MadnetSim.class.getPackage().getImplementationVersion() + " Started");
				try {
					s.simulate();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
				
					if (config.getIsStatisticsEnabled()) {
						
						Statistics statistics = s.getStatistics();
						statistics.ensureStatisticsDirExists();
				
						if (config.getIsUnhappinessStatsEnabled()) {
							statistics.dumpUnhappiness();
						}
						
						if (config.getIsIndividualNodesStatsEnabled()){
							statistics.dumpPartialStats();
						}
						
						if (config.getIsGlobalStatisticsEnabled()) {
							statistics.dumpNodeList(s.nodeList);
							statistics.dumpConfigFile();
						}
					}
					System.out.println("Simulation Ended");
				}
			}	
		} catch (OutOfMemoryError oome) {
			System.out.println("This simulation requires more space to be run. Try to increase the Java maximum heap space \n" +
					"e.g., java -Xmx2048M -jar MadnetSim.jar");
		}
	}
}
