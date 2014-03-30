package de.saviodimatteo.madnetsim.config;

import java.util.HashMap;

public class SmartStringMap extends HashMap<String, String> {
	public String get(String key) {
		String result = super.get(key);
		if (result != null) 
			result.trim();
		else
			result = "";
		return result;
	}
}
