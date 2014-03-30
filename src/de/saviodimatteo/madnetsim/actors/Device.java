package de.saviodimatteo.madnetsim.actors;

import de.saviodimatteo.madnetsim.data.DataSize;
import de.saviodimatteo.madnetsim.data.Point;
import de.saviodimatteo.madnetsim.data.DataSize.UNIT;
import de.saviodimatteo.madnetsim.exceptions.InvalidDomainException;
import de.saviodimatteo.madnetsim.utils.Pair;

public abstract class Device extends Point {
	protected DataSize iMemoryAmount; // in Bytes
	protected DataSize iMemoryLimit;
	protected float iMinLinkRadius; // in Meters
	protected float iMaxLinkRadius;
	public long iUsage; // used for graphics
	Device(float aLon, float aLat) {
		super(aLon, aLat);
		iMemoryAmount = new DataSize();
		iMemoryLimit = new DataSize();
		iMinLinkRadius = 0;
		iMaxLinkRadius = 0;
		iUsage = 0;
	}
	public void addAmount(long aAmount) {
		iMemoryAmount.addAmount(aAmount);
	}
	public void setMemoryLimit(DataSize aMemoryLimit) {
		iMemoryLimit = aMemoryLimit;
	}
	public DataSize getMemoryAmount() {
		return iMemoryAmount;
	}
	public void setUsage(long aUsage) {
		iUsage = aUsage;
	}
	public float getMemoryUsage() {
		float result = 100 * (iMemoryAmount.getAmount(UNIT.B)/iMemoryLimit.getAmount(UNIT.B));
		if (result < 0) result = 0f;
		else if (result > 100)
			result = 100f;
		return result;
	}
	public void setLinkRadius(float aMinMeters, float aMaxMeters) throws InvalidDomainException {
		if (aMinMeters <= aMaxMeters) {
			iMaxLinkRadius = aMaxMeters;
			iMinLinkRadius = aMinMeters;
		}
		else
			throw new InvalidDomainException(new Float(aMinMeters));
	}
	public Pair<Float,Float> getLinkRadius() {
		return new Pair(iMinLinkRadius,iMaxLinkRadius);
	}

}
