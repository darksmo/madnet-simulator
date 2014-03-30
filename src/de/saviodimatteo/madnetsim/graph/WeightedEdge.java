package de.saviodimatteo.madnetsim.graph;

public class WeightedEdge implements Edge {
	String id;
	float weight;
	public WeightedEdge(String aId, float aWeight) {
		id = aId;
		weight = aWeight;
	}
	public WeightedEdge(float aWeight) {
		this("",aWeight);
	}
	public String toString () {
		return this.id;
	}
	
	public float getWeight() {
		return this.weight;
	}
}
