package wyl.svc;

public class LeagueService {

	/**
	 * Get data relevant to each tier. 
	 * @return JSON Object containing all the data for each tier. 
	 */
	public String getAllTierData(){
		String result = "successful call to getAllTierData";
		
		return result;
	}
	
	
	/**
	 * Get the counters for the desired champion.
	 * @param championNameIn The name as a string of the desired champion.
	 * @return JSON Object containing information and counters.
	 */
	public String getCounterInfo(String championNameIn){
		String result = "succesfful call to getCounterInfo";
		
		return result;
	}
	
	/**
	 * Get basic site information, like current data size, current patch, current season, etc. 
	 * @return JSON Object containing site and data information.
	 */
	public String getWylInfo(){
		String result = "successful call to getWylInfo";
		
		return result;
	}
}
