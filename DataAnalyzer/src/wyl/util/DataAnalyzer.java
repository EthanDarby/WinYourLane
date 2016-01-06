package wyl.util;


import java.util.Date;
import java.io.Console;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.gson.*;

import constant.Region;
import dto.Champion.Champion;
import dto.League.League;
import dto.Match.MatchDetail;
import dto.Match.Participant;
import dto.Match.ParticipantIdentity;
import dto.Match.ParticipantStats;
import dto.Match.ParticipantTimeline;
import dto.Match.Player;
import dto.Match.Team;
import dto.MatchList.MatchList;
import dto.MatchList.MatchReference;
import dto.Static.ChampionList;
import dto.Stats.AggregatedStats;
import dto.Stats.ChampionStats;
import dto.Stats.RankedStats;
import dto.Summoner.*;
import main.java.riotapi.*;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ParallelScanOptions;
import com.mongodb.ServerAddress;

/*import Classes.ChampionData;
import Classes.MatchData;
import Classes.Matchup;
import Classes.PlayerDetails;
*/
import org.bson.Document;
import org.mongodb.morphia.*;
import org.mongodb.morphia.query.Query;



/** Class for doing various analysis on the data contained in the database.
 * 
 * @author EthanDarby
 *
 */
public class DataAnalyzer {
	
	//These constants are the numeric values that are used when calculating the overall tier of the match
	final int BRONZE_VALUE = 10;
	final int SILVER_VALUE = 20;
	final int GOLD_VALUE = 30;
	final int PLATINUM_VALUE = 40;
	final int DIAMOND_VALUE = 50;
	final int MASTER_VALUE = 60;
	final int CHALLENGER_VALUE = 70;
	
	
	
	/**Determines the tier of the match by applying an algorithm based on previous season rank and current season.
	 * 
	 * @param detailsIn MatchDetails to analyze.
	 * @return Returns tier value for the match.
	 * 
	 */
	public double getMatchTier(MatchDetail detailsIn){
		//The algorithm is (0.5 * previousTierValue) + (cuurentTierValue * (1 + (1 - 0.division))
				// For example, a previous season rank of BRONZE_VALUE and current rank of SILVER_VALUE 2 would be
				// (0.5 * 10) + (20 * (1 + (1 - 0.2)) = 5 + (20 * 1.8) = 41
				
				double tier = 0;
				boolean unranked = false;
				RiotApi api = new RiotApi("2c6decef-0974-4fda-b5d1-d0470cab8a89");
				
				//for each participant, get their current rank
				for(ParticipantIdentity participant: detailsIn.getParticipantIdentities()){
					long summonerId = participant.getPlayer().getSummonerId();
					List<League> playerLeagues = new ArrayList<League>();
					String division = "none";
					
					//call the api to get the summoner's current tier
					try {
						playerLeagues = api.getLeagueEntryBySummoner(summonerId);
						TimeUnit.SECONDS.sleep(2);
						
					} catch (RiotApiException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch(Exception error){
						
					}
					
					for(League league:playerLeagues){
						
						if(league.getQueue().equals("RANKED_SOLO_5x5")){
							//Add more robust handling here, we are assuming it always lists soloqueue first
							division = league.getEntries().get(0).getDivision();
							double multiplier = 1;
							if(division != null && division != "none"){
								switch (division){
								case "I":
									multiplier = 1.9;
									break;
									
								case "II":
									multiplier = 1.8;
									break;
									
								case "III":
									multiplier = 1.7;
									break;
									
								case "IV":
									multiplier = 1.6;
									break;
									
								case "V":
									multiplier = 1.5;
									break;
								
								}
							}
							String leagueString = league.getTier();
							switch (leagueString){
							case "BRONZE_VALUE":
								tier += multiplier * BRONZE_VALUE;
								//System.out.println(leagueString + " " + division);
								break;
							case "SILVER_VALUE":
								tier += multiplier * SILVER_VALUE;
								//System.out.println(leagueString + " " + division);
								break;
								
							case "GOLD_VALUE":
								tier += multiplier * GOLD_VALUE;
								//System.out.println(leagueString + " " + division);
								break;
								
							case "PLATINUM_VALUE":
								tier += multiplier * PLATINUM_VALUE;
								//System.out.println(leagueString + " " + division);
								break;
								
							case "DIAMOND_VALUE":
								tier += multiplier * DIAMOND_VALUE;
								//System.out.println(leagueString + " " + division);
								break;
								
							case "MASTER_VALUE":
								tier += multiplier * MASTER_VALUE;
								//System.out.println(leagueString + " " + division);
								break;
								
							case "CHALLENGER_VALUE":
								tier += multiplier * CHALLENGER_VALUE;
								//System.out.println(leagueString + " " + division);
								break;
								
							default:
								unranked = true;
								break;
							}
						}
						
					}
				}
				//if they are unranked, get the previous season
				if(unranked){
					unranked = false;
					for(Participant participant: detailsIn.getParticipants()){
						String previousTier = participant.getHighestAchievedSeasonTier();
						//System.out.print("Unranked currently, using previous season " + previousTier);
						switch (previousTier){
					
							case "BRONZE_VALUE":
								tier += (0.5 * BRONZE_VALUE);
								break;
							case "SILVER_VALUE":
								tier += (0.5 * SILVER_VALUE);
								break;
							
							case "GOLD_VALUE":
								tier += (0.5 *GOLD_VALUE);
								break;
							
							case "PLATINUM_VALUE":
								tier += (0.5 * PLATINUM_VALUE);
								break;
							
							case "DIAMOND_VALUE":
								tier += (0.5 * DIAMOND_VALUE);
								break;
							
							case "MASTER_VALUE":
								tier += (0.5 *MASTER_VALUE);
								break;
							
							case "CHALLENGER_VALUE":
								tier += (0.5 * CHALLENGER_VALUE);
							break;	
							
							default:
								tier += BRONZE_VALUE;
							break;
						}
					}
				}
				
				
				return tier;
	}
	
	/**Gathers data from the match that will be saved to the appropriate tier.
	 * 
	 * @param detailsIn MatchDetails to analyze.
	 */
	public void analyzeTierDataFrommatch(MatchDetail detailsIn){
		
		//first, get the tier that the match is in
		double tier_value = getMatchTier(detailsIn);
		
		//get raw data
		//Total Kills
		//Total Deaths
		//Total Assists
		//Total Creeps
		//Total Dragons
		//Total Barons
		//Total GameTime
		//Total Rift Heralds
		//Time of First Dragon
		//Time of First Blood
		//Time of First Baron
		//Time of First Rift Herald
		
		long[] kills_deaths_assists_cs = new long[4];
		kills_deaths_assists_cs = getTotalKDAandCS(detailsIn.getParticipants());
		
		long kills = kills_deaths_assists_cs[0];
		long deaths = kills_deaths_assists_cs[1];
		long assists = kills_deaths_assists_cs[2];
		long cs = kills_deaths_assists_cs[3];
		
		
		//getDragons(MatchDetail detailsIn);
		//getGameTime(MatchDetail detailsIn);
		
		//have to write own functino for getting rift heralds from riot api json object
		//getRiftHeralds(MatchDetail detailsIn);
		//getTimeFirstRiftHerals(MatchDetails detailsIn);
		
		//getTimeFirstDragon(MatchDetail detailsIn);
		//getTimeFirstBlood(MatchDetail detailsIn);
		//getTimeFirstBaron(MatchDetail detailsIn);
		
		
		
	}
	
	/**Gets the total kills, deaths, assists, and creep score for the game.
	 * 
	 * @param participantsIn A list of Participant objects.
	 * @return
	 */
	public long[] getTotalKDAandCS(List<Participant> participantsIn){
		long[] values = new long[4];
		
		long deaths = 0;
		long kills = 0;
		long assists = 0;
		long cs = 0;
		
		for(Participant participant:participantsIn){
			kills += participant.getStats().getKills();
			deaths += participant.getStats().getDeaths();
			assists += participant.getStats().getAssists();
			cs += participant.getStats().getMinionsKilled();
		}
		
		values[0] = kills;
		values[1] = deaths;
		values[2] = assists;
		values[3] = cs;
		
		return values;
	}
	
	/**Gets the total number of dragons killed during the match.
	 * 
	 * @param teamsIn A list of the teams.
	 * @return
	 */
	public long getDragons(List<Team> teamsIn){
		long dragons = 0;
		for(Team team:teamsIn){
			dragons += team.getDragonKills();
		}
		return dragons;
	}
	
	/**Gets the total number of barons killed during the match.
	 * 
	 * @param teamsIn A list of the team.
	 * @return
	 */
	public long getBarons(List<Team> teamsIn){
		long barons = 0;
		for(Team team:teamsIn){
			barons+=team.getBaronKills();
		}
		return barons;
	}
	
	
	/**Determine the stats for each champion played in that match.
	 * 
	 * @param detailsIn MatchDetails to anaylze.
	 */
	public void analyzeChampionDataFromMatch(MatchDetail detailsIn){
		//get the tier
		//enum tier = getMatchTier(detailsIn);
		
				
	}

	public void updateChampionData(Participant championIn, String tierIn){
		
		//update relevant tier in the ChampionData table with the stats from this match for this champion
		long kills = championIn.getStats().getKills();
		long deaths = championIn.getStats().getDeaths();
		long assists = championIn.getStats().getAssists();
		boolean isWinner = false;
		long minionCS = championIn.getStats().getMinionsKilled();
		
		isWinner = championIn.getStats().isWinner();
		
		if(isWinner){
			//update winner column
		}
		
		else{
			//just update the other numbers, but it's not a win
		}
		
	}
	
	public void updateChampionCounterData(MatchDetail detailsIn, String tierIn){
		
		//for each champion, analyze their data relevant to their counter and tier
		
		//try to find a counter for each one, if there is no counter, then take a guess or just add it and forget, depends how often this happens
		
		//1)split the participant lists into team
		List<Participant> team100 = new ArrayList<Participant>();
		List<Participant> team200 = new ArrayList<Participant>();
		
		for(Participant aPlayer:detailsIn.getParticipants()){
			
			if(aPlayer.getTeamId() == 100){
				team100.add(aPlayer);
			}
			else{
				team200.add(aPlayer);
			}			
		}
		//now, for each champion, update their entry in the database
		for(Participant aPlayer: team100){
			
			for(Participant counter:team200){
				
				boolean counterFound = false;
				if(aPlayer.getTimeline().getLane() == counter.getTimeline().getLane()){
					
					//they should also have the same role, this is espeically true in bot supp/dc
					if(aPlayer.getTimeline().getRole() == counter.getTimeline().getRole()){
						counterFound = true;
						//NOW SAVE TO DB
					}					
				}
				
				if(!counterFound){
					//if we never found a counter for whatever reason, don't update the table
					//store this somewhere for futher investigation
				}
			}
		}
	}
		
		
	
	/** Analyzes all the matches in the database 
	 * @param None
	 * @return None
	 */
	/*
	@SuppressWarnings("all")
	public void analyzeTierData(){
		MongoClient mongoClient = new MongoClient();
		DB database = mongoClient.getDB("LeagueData");
		DBCollection matchDataCollection = database.getCollection("MatchData");
		DBCursor cursor  = matchDataCollection.find();
		Morphia morphia = new Morphia();
		Datastore ds = morphia.createDatastore(mongoClient,"LeagueData");
		morphia.map(MatchData.class);
		
		//iterate through each matchData object and save what needs to be saved
		int i = 0;
		System.out.println("Starting matchData loop");
		int pre2015Matches = 0;
		int season2015Matches = 0;
		int pre2016Matches = 0;
		try{
		while(cursor.hasNext()){
			DBObject temp = cursor.next();
			
			MatchData match = morphia.fromDBObject(MatchData.class, temp);
			System.out.println("MatchId: " + match.matchID);
			MatchDetail details = match.details;
			String season = null;
			season = details.getSeason();
			
			switch (season){
			
			case "PRESEASON2015":
				pre2015Matches++;
				break;
			
			case "SEASON2015":   
				season2015Matches++;
				break;
				
			case "PRESEASON2016":
				//do analysis on current matches
				pre2016Matches++;
				this.analyzeChampionDataFromMatch(details);
			
				break;
			
			}
			//1) Save the info for each player
			//2) Save the info for each champion
			//3) Save the info for each tier
			//4) Mark as analyzed
			i++;
			
		}
		}
		catch(Exception error){
			System.out.println("Error");
			System.out.println(error.getMessage());
		}
		
		System.out.println("Total Matches Analyzed: " + i);
		System.out.println("Preseason 2015: " + pre2015Matches);
		System.out.println("Season 2015: " + season2015Matches);
		System.out.println("Preseason 2016: " + pre2016Matches);
	}
	*/
	
	
	

	public static void main(String[] args) {
		//MongoDB objects 
		MongoClient client = new MongoClient();
		DB db = client.getDB("LeagueData");
		
		
		// TODO Auto-generated method stub
		DataAnalyzer driver = new DataAnalyzer();
		//driver.analyzeTierData();
		
		//test for the analyze tier method
		try {
			MatchDetail details = new RiotApi("2c6decef-0974-4fda-b5d1-d0470cab8a89").getMatch(2026976873);
			driver.getMatchTier(details);
		} catch (RiotApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		//start a loop that monitors the queue, updating and analyzing when things are put there
		
	}

}
