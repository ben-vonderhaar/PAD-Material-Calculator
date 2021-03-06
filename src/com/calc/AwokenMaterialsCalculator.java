package com.calc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AwokenMaterialsCalculator {

	JsonArray monstersJSON;
	JsonObject userdataJSON;
	JsonObject evolutionsJSON;
	
	static Map<Integer, JsonObject> monsters;
	static Map<Integer, JsonObject> userMonsterUniqueIdToMonsterDataMap;
	static Map<Integer, Set<Integer>> userMonsterIdToMonsterUniqueIdsSetMap;
	static Map<Integer, JsonArray> evolutions;
	static Map<Integer, JsonObject> materials;
	
	
	JsonParser parser;
	
	private static int[] imbalancedMaterialNumberIgnoreList = {
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16, 18, 19, 20, // Starter Dragons
			 22, 23, 25, 26, 28, 29, 31, 32, 34, 35, 211, 212, 213, 214, 215, // Ripper Dragons
			 37, 39, 41, 43, 45, // Slimes
			 47, 49, 51, 53, 55, // Carbuncles
			 57, 59, 61, // Goblins
			 63, 65, 67, 310, 311, 312, 313, 314, 315, // Ogres
			 69, 71, 73, 75, 77, 906, 907, 908, 909, 910, // Knights
			 79, 81, 83, 85, 87, 316, 317, 318, 319, 320, 1272, 1273, 1274, 2127, 2128, // Golems
			 89, 91, 93, 95, 97, // Healer Girls
			 99, 101, 103, 105, 109, 229, 230, 231, 232, 233, 822, 823, 824, 825, 826, // Mystic Knights
			 113, 115, 117, 119, 121, // Late Bloomer Dragons
			 123, 125, 127, 129, 131, // Greco-Roman Gods
			 133, 135, 137, 139, 141, // Japanese Gods
			 107, 111, 222, 224, 226, 895, // Heartbreakers,
			 142, 143, 144, 145, 146, // Legendary Dragons
			 188, // Zeus
			 1937, 1803, // DBZ
			 1254, 1255, // Archangel uevos
			 1260 // uevo astaroth
	};
	
	static Set<Integer> imbalancedMaterialNumberIgnoreSet;
	static Map<Integer, Integer> backwardsAwokenMaterials;
	
	public AwokenMaterialsCalculator() {
		parser = new JsonParser();

		monsters = new HashMap<Integer, JsonObject>();
		evolutions = new HashMap<Integer, JsonArray>();
		materials = new HashMap<Integer, JsonObject>();
		
		userMonsterUniqueIdToMonsterDataMap = new HashMap<Integer, JsonObject>();
		userMonsterIdToMonsterUniqueIdsSetMap = new HashMap<Integer, Set<Integer>>();
		imbalancedMaterialNumberIgnoreSet = new HashSet<Integer>();
		backwardsAwokenMaterials = new HashMap<Integer, Integer>();
		
		System.out.println("constructor");
		
		try {
			this.monstersJSON = getPadHerderJSONArray("http://www.padherder.com/api/monsters/", "monsters.json");
			this.evolutionsJSON = getPadHerderJSONObject("http://www.padherder.com/api/evolutions/", "evolutions.json");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}		
		
		// Convert array into a set, which is easier to lookup against
		for (int i = 0; i < imbalancedMaterialNumberIgnoreList.length; i++) {
			imbalancedMaterialNumberIgnoreSet.add(imbalancedMaterialNumberIgnoreList[i]);
		}
		
		// Copy monster data into a Map, hashed on the monsterId
		for (int i = 0; i < monstersJSON.size(); i++) {
			monsters.put(monstersJSON.get(i).getAsJsonObject().get("id").getAsInt(), monstersJSON.get(i).getAsJsonObject());
		}
		
		// Hash Evolutions
		for (Integer monsterId : monsters.keySet()) {
			if (null != evolutionsJSON.get(String.valueOf(monsterId))) {
				
				JsonArray monsterEvolutions = evolutionsJSON.get(String.valueOf(monsterId)).getAsJsonArray();
				
				for (int i = 0; i < monsterEvolutions.size(); i++) {
					
					JsonArray materials = monsterEvolutions.get(i).getAsJsonObject().get("materials").getAsJsonArray();
					
					for (int j = 0; j < materials.size(); j++) {

						Integer evoMatId = materials.get(j).getAsJsonArray().get(0).getAsInt();
						Integer evolvesToId = monsterEvolutions.get(i).getAsJsonObject().get("evolves_to").getAsInt();
						
						if (!imbalancedMaterialNumberIgnoreSet.contains(evolvesToId) && evolvesToId < evoMatId) {
							
							// Add backwards material if the evoMatId > what is known (no value would count as 0)
							// This enables post-sort reordering of the userMonsterIds such that awoken cards
							// are processed first and thus any potential materials are not reserved for their own evos.
							if (null == backwardsAwokenMaterials.get(evolvesToId)
									|| backwardsAwokenMaterials.get(evolvesToId) < evoMatId) {
								System.out.println("add/update evo mat Id : " + getName(evolvesToId) + " - > " + getName(evoMatId));
								backwardsAwokenMaterials.put(evolvesToId, evoMatId);
							} 
						}
					}
				}
				
				evolutions.put(monsterId, evolutionsJSON.get(String.valueOf(monsterId)).getAsJsonArray());
			}
		}
	}
	
	private JsonArray getPadHerderJSONArray(String URL, String file) throws IOException {
		return getPadHerderJSONElement(URL, file).getAsJsonArray();
	}
	
	private JsonObject getPadHerderJSONObject(String URL, String file) throws IOException {
		return getPadHerderJSONElement(URL, file).getAsJsonObject();
	}
	
	private JsonElement getPadHerderJSONElement(String URL, String file) throws IOException {
		FileReader fileReader = null;
		
		try {
			 fileReader = new FileReader(file);
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find " + file + ", loading from PadHerder");
			
			writeURLToFile(URL, file);
			
			try {
				fileReader = new FileReader(file);
			} catch (FileNotFoundException e1) {
				System.err.println("Unable to retrieve " + file + " from PadHerder");
				throw new IOException("Unable to retrieve " + file + " from PadHerder");
			}
		}
		
		return parser.parse(fileReader);
	}
	
	private void writeURLToFile(String URL, String file) {
		try {
			
			HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
	        
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("User-agent", "awoken materials calculator");
	        
	        connection.connect();
	        connection.disconnect();
			
			String location = connection.getHeaderField("Location");
			String cookies = connection.getHeaderField("Set-Cookie");
			
			connection = (HttpURLConnection) new URL(location).openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Cookie", cookies);
			connection.setRequestProperty("User-agent", "awoken materials calculator");
			
	        connection.connect();
			FileUtils.copyInputStreamToFile(connection.getInputStream(), new File(file));
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public List<JsonObject> getMatchesForSearch(String searchTerm) {
		
		String lowerSeachTerm = searchTerm.toLowerCase();
		
		List<JsonObject> matches = new ArrayList<JsonObject>();
		
		for (int i = 0; i < monstersJSON.size(); i++) {
			
			if (monstersJSON.get(i).getAsJsonObject().get("id").getAsString().equals(searchTerm)
					|| monstersJSON.get(i).getAsJsonObject().get("name").getAsString().toLowerCase().contains(lowerSeachTerm)) {
				matches.add(monstersJSON.get(i).getAsJsonObject());
			}
		}
		
		return matches;
	}
	
	public Card getEvolutionMaterialsString(int monsterId) {
		return getEvoMaterialsTo(0, monsterId, -1);
	}
	
	public Card getEvolutionMaterialsString(int monsterId, int fromMonsterId) {
		return getEvoMaterialsTo(0, monsterId, fromMonsterId);
	}

	
	/**
	 * Gets the direct evolution line and prints the evolution tree as it is calculated.
	 * 
	 * TODO re-incorporate hasMaterial() via padherder call or otherwise
	 * 
	 * @param depth
	 * @param toMonsterId
	 * @param fromMonsterId
	 * @return
	 */
	private static Card getEvoMaterialsTo(int depth, int toMonsterId, int fromMonsterId) {
		
		Card thisCard = new Card(toMonsterId);

		if (toMonsterId == fromMonsterId) {
			return thisCard;
		}
		
		for (Integer i : evolutions.keySet()) {
			
			for (int j = 0; j < evolutions.get(i).size(); j++) {
				
				if (getEvolvesTo(i, j) == toMonsterId && i < toMonsterId) {
					

					// Find potential mats for these mats
					JsonArray evoMaterials = evolutions.get(i).get(j).getAsJsonObject().get("materials").getAsJsonArray();
					
					System.out.println(getPrefixString(depth) + i + ": " + getName(i) + " + " + evoMaterials 
							+ " -> " + toMonsterId + ": " + getName(toMonsterId));
					
					System.out.println(getPrefixString(depth) + "=====Materials=====");
					
					System.out.println(evoMaterials);
					
					for (int k = 0; k < evoMaterials.size(); k++) {

						int thisMonsterId = evoMaterials.get(k).getAsJsonArray().get(0).getAsInt();
						Card material = new Card(thisMonsterId);
						
						// TODO determine if this particular card is farmable
						// MP is not a possible cutoff point
//						if (monsters.get(thisMonsterId).get("monster_points").getAsInt() < 1000) {
//							System.out.println(getName(thisMonsterId) + " (" + thisMonsterId + ") is farmable and therefore a chain leaf");
//							thisCard.addMaterial(material);
//							continue;
//						}
						
						for (int l = 0; l < evoMaterials.get(k).getAsJsonArray().get(1).getAsInt(); l++) {
						
							// TODO check if user has material
//							if (!hasMaterial(evoMaterials.get(k).getAsJsonArray().get(0).getAsInt())) {
								
								System.out.println(getPrefixString(depth) + "Checking evo to " 
										+ monsters.get(evoMaterials.get(k).getAsJsonArray().get(0).getAsInt()).get("name").getAsString()
										+ " (id=" + evoMaterials.get(k).getAsJsonArray().get(0).getAsInt() + ", mp = "
										+ monsters.get(evoMaterials.get(k).getAsJsonArray().get(0).getAsInt()).get("monster_points").getAsInt() + ")");
								
								Card previousEvoMaterial = 
										getEvoMaterialsTo(depth + 1, evoMaterials.get(k).getAsJsonArray().get(0).getAsInt(), -1);
								
								System.out.println(getPrefixString(depth + 1) + previousEvoMaterial + "..." + thisMonsterId);
								
								if (previousEvoMaterial.getMaterials().size() == 0) {
									//System.out.println(getPrefixString(depth + 1) + "Calculated transitive mats: " + getName(thisMonsterId));
								//} else {

									System.out.println(getPrefixString(depth + 1) + "Chain leaf: " + getName(thisMonsterId) + " (id=" + thisMonsterId + ")");
									
									thisCard.addMaterial(material);
								} else {
									thisCard.addMaterial(previousEvoMaterial);
								}
								
//							} 
							
						}
						
					}

					Card previousEvolution = getEvoMaterialsTo(depth + 1, i, fromMonsterId);
					
					if (previousEvolution.getMaterials().size() == 0) {
						System.out.println(getPrefixString(depth) + "Chain leaf: " + getName(previousEvolution.getId())
							+ " (id=" + previousEvolution.getId() + ")");
					}
					
					thisCard.addMaterial(previousEvolution); 
					
					break;
				}
			}
		}
		
		return thisCard;
	}
	
	// TODO enable
//	public static boolean hasMaterial(int monsterId) {
//		
//		JsonObject material = materials.get(monsterId);
//		
//		if (null != material && material.get("count").getAsInt() > 0) {
//			int currentCount = material.remove("count").getAsInt();
//			material.addProperty("count", --currentCount);
//			
//			materials.put(monsterId, material);
//			
//			return true;
//		}
//		
//		// TODO respect ignore list.
//		
//		// If material is available in user monsters list, mark it as used and return true.
//		Set<Integer> availableMonsters = userMonsterIdToMonsterUniqueIdsSetMap.get(monsterId);
//		
//		if (null != availableMonsters && availableMonsters.size() > 0) {
//			
//			availableMonsters.remove(Integer.valueOf(availableMonsters.toArray()[0].toString()));
//			userMonsterIdToMonsterUniqueIdsSetMap.put(monsterId, availableMonsters);
//			
//			return true;
//		}
//		
//		
//		return false;
//	}
	
	public static String getEvoStringFrom(int monsterId) {
		if (null != evolutions.get(monsterId)) {
			for (int i = 0; i < evolutions.get(monsterId).size(); i++) {
				if (evolutions.get(monsterId).get(i).getAsJsonObject().get("evolves_to").getAsInt() > monsterId) {
					return " -> " + getName(getEvolvesTo(monsterId, i)) + getEvoStringFrom(getEvolvesTo(monsterId, i)); 
				}
			}
		}
		
		return "";
	}
	
	public static String getPrefixString(int depth) {
		String prefixString = "";
		
		while (depth > 0) {
			prefixString += "\t";
			depth--;
		}
		
		return prefixString;
	}
	
	public static String getEvoMatsString(int monsterId, int evoIndex) {
		JsonArray materials = evolutions.get(monsterId).get(evoIndex).getAsJsonObject().get("materials").getAsJsonArray();
		
		String materialsString = "";
		
		for (int i = 0; i < materials.size(); i++) {
			if (!materialsString.equals("")) {
				materialsString += ", ";
			}
			
			materialsString += "x" + materials.get(i).getAsJsonArray().get(1).getAsInt() + " " + getName(materials.get(i).getAsJsonArray().get(0).getAsInt());
		}
		
		return "\\---> " + materialsString;
	}
	
	public static int getEvolvesTo(int monsterId, int evoIndex) {
		return evolutions.get(monsterId).get(evoIndex).getAsJsonObject().get("evolves_to").getAsInt();
	}
	
	public static String getName(int monsterId) {
		return monsters.get(monsterId).get("name").getAsString();
	}
	
	public static int getMonsterId(int monsterId) {
		return monsters.get(monsterId).get("monster").getAsInt();
	}
	
	
	public static void main(String [] args) {
		AwokenMaterialsCalculator amc = new AwokenMaterialsCalculator();

		MaterialList materialList = new MaterialList(amc.getEvolutionMaterialsString(2969));
		Map<Integer, Integer> materials = materialList.getMaterials();
		
		System.out.println("Need:");
		
		for (Integer monsterId : materials.keySet()) {
			System.out.println(materials.get(monsterId) + " " + getName(monsterId));
		}
		
		Scanner scanner = new Scanner(System.in);
		
		// Infinitely listen for commands
		/*while (true) {
			System.out.print("Enter command (add/list/quit): ");
			String command = scanner.nextLine();
			
			// "quit" exits program
			if (command.equals("quit")) {
				System.out.println("terminating");
				break;
			} else if (command.equals("add")) {
				
				while (true) {
				
					System.out.print("Enter Monster by partial name or ID: ");
					String searchTerm = scanner.nextLine();
					
					List<JsonObject> matches = amc.getMatchesForSearch(searchTerm);
					
					System.out.println("Found matches.  Type the number corresponding to the monster you wish to add or the action you wish to take:");

					boolean newAction = false;
					
					while (true) {

						for (int i = 0; i < matches.size(); i++) {
							System.out.println("(" + (i + 1) + ") " 
									+ matches.get(i).get("id").getAsString() + ": " + matches.get(i).get("name").getAsString());
						}
		
						System.out.println("(" + (matches.size() + 1) + ") New Search");
						System.out.println("(" + (matches.size() + 2) + ") New Action");
		
						String addAction = scanner.nextLine().trim();
						
						if (addAction.equals(String.valueOf(matches.size() + 2))) {
							newAction = true;
							break;
						} else if (addAction.equals(String.valueOf(matches.size() + 1))) {
							break;
						} else {
							try {
								int addIndex = Integer.valueOf(addAction);
								
								if (addIndex > matches.size()) {
									System.out.println("Please make a valid selection:");
								} else {
									System.out.println(amc.getEvolutionMaterialsString(matches.get(addIndex - 1).get("id").getAsInt()));
								}
							} catch (NumberFormatException e) {
								System.out.println("Please type only the number corresponding to your selection:");
							}
						}
					}
					
					if (newAction) {
						break;
					}
				}
			} else if (command.equals("list")) {
				
			} else {
				System.out.println("unrecognized command");
			}
			
		}*/
		
		scanner.close();
	}
	
}
