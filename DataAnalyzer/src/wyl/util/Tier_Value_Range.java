package wyl.util;

public enum Tier_Value_Range {
	BRONZE (0,184),
	SILVER (185, 315),
	GOLD (316, 485),
	PLATINUM (486, 615),
	DIAMOND(616, 780),
	MASTER(781,900),
	CHALLENGER(900,1500);
	
	private final int lowerBound;
	private final int upperBound;
	
	Tier_Value_Range(int lowerBound, int upperBound){
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}
}


