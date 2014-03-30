package de.saviodimatteo.madnetsim.simulation;


public class ProcessingSimulator extends Simulator{

	public ProcessingSimulator(int aStartTime, int deltaT) {
		super(deltaT);
		while (time <= aStartTime) {
			doSimulationStep();
		}
	}
}
