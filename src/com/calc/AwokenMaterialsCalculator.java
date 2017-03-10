package com.calc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
		System.out.println("constructor");
	}
	
	private void oldConstructor () {
		parser = new JsonParser();
		
		try {
			monstersJSON = parser.parse(new FileReader("monsters.txt")).getAsJsonArray();
			userdataJSON = parser.parse(new FileReader("userdata.txt")).getAsJsonObject();
			evolutionsJSON = parser.parse(new FileReader("evolutions.txt")).getAsJsonObject();
		} catch (JsonIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Populated from data files
		monsters = new HashMap<Integer, JsonObject>();
		evolutions = new HashMap<Integer, JsonArray>();
		materials = new HashMap<Integer, JsonObject>();
		
		userMonsterUniqueIdToMonsterDataMap = new HashMap<Integer, JsonObject>();
		userMonsterIdToMonsterUniqueIdsSetMap = new HashMap<Integer, Set<Integer>>();
		imbalancedMaterialNumberIgnoreSet = new HashSet<Integer>();
		backwardsAwokenMaterials = new HashMap<Integer, Integer>();
		
		// Convert array into a set, which is easier to lookup against
		for (int i = 0; i < imbalancedMaterialNumberIgnoreList.length; i++) {
			imbalancedMaterialNumberIgnoreSet.add(imbalancedMaterialNumberIgnoreList[i]);
		}
		
		// Copy monster data into a Map, hashed on the monsterId
		for (int i = 0; i < monstersJSON.size(); i++) {
			monsters.put(monstersJSON.get(i).getAsJsonObject().get("id").getAsInt(), monstersJSON.get(i).getAsJsonObject());
		}

		// Copy User Monster Data to maps.
		for (int i = 0; i < userdataJSON.get("monsters").getAsJsonArray().size(); i++) {
			
			// Map unique ID to monster data.
			userMonsterUniqueIdToMonsterDataMap.put(userdataJSON.get("monsters").getAsJsonArray().get(i).getAsJsonObject().get("id").getAsInt(), 
					userdataJSON.get("monsters").getAsJsonArray().get(i).getAsJsonObject());
			
			// Add ID to list of known IDs for a particular monster ID
			Set<Integer> existingMonsterUniqueIdsSet = userMonsterIdToMonsterUniqueIdsSetMap.get(userdataJSON.get("monsters").getAsJsonArray().get(i).getAsJsonObject().get("monster").getAsInt());

			// Initialize set of monster ids if necessary.
			if (null == existingMonsterUniqueIdsSet) {
				existingMonsterUniqueIdsSet = new HashSet<Integer>();
			}
			
			// Map monster Id to a set of unique IDs
			existingMonsterUniqueIdsSet.add(userdataJSON.get("monsters").getAsJsonArray().get(i).getAsJsonObject().get("id").getAsInt());
			
			userMonsterIdToMonsterUniqueIdsSetMap.put(userdataJSON.get("monsters").getAsJsonArray().get(i).getAsJsonObject().get("monster").getAsInt(), existingMonsterUniqueIdsSet);
		}
		
		// Copy material data into a Map, hashed on the monster ID.
		for (int i = 0; i < userdataJSON.get("materials").getAsJsonArray().size(); i++) {
			
			JsonObject materialData = userdataJSON.get("materials").getAsJsonArray().get(i).getAsJsonObject();
			JsonObject materialMonsterData = monsters.get(userdataJSON.get("materials").getAsJsonArray().get(i).getAsJsonObject().get("monster").getAsInt());
			
			materialMonsterData.addProperty("count", materialData.get("count").getAsNumber());
			
			materials.put(materialData.get("monster").getAsInt(), materialMonsterData);
		}
		
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
		
		// Order monsters by descending ID so awokens and other evos that require evolved descend monsters are processed before the descend monsters themselves
		List<Integer> userMonsterIds = new ArrayList<Integer>(userMonsterUniqueIdToMonsterDataMap.keySet());
		
		System.out.println(backwardsAwokenMaterials);
		
		userMonsterIds.sort(new Comparator<Integer>() {

			@Override
			public int compare(Integer thisId, Integer otherId) {

				int thisHighestEvo = getHighestEvo(userMonsterUniqueIdToMonsterDataMap.get(getHighestEvo(thisId)).get("monster").getAsInt());
				int otherHighestEvo = getHighestEvo(userMonsterUniqueIdToMonsterDataMap.get(getHighestEvo(otherId)).get("monster").getAsInt());
				
				return otherHighestEvo - thisHighestEvo;
				
			}
		});
		
		for (Integer evolutionMonsterId : backwardsAwokenMaterials.keySet()) {
			Iterator<Integer> userMonsterIdsIterator = userMonsterIds.iterator();
			
			Integer reorderingMonster = 0;
			
			System.out.println(userMonsterIds);
			
			System.out.println("iterating for " + getName(backwardsAwokenMaterials.get(evolutionMonsterId)) + " (" + backwardsAwokenMaterials.get(evolutionMonsterId) + ")");
			
			while (userMonsterIdsIterator.hasNext()) {
				
				Integer userMonsterId = userMonsterIdsIterator.next();
				
				JsonObject thisUserMonster = userMonsterUniqueIdToMonsterDataMap.get(userMonsterId);
				
				if (null == thisUserMonster) {
		//	.getClass().		userMonsterIdsIterator.
					
					System.out.println(userMonsterId);
				} 
				
				Integer thisUserMonsterId = thisUserMonster.get("monster").getAsInt();
				
				List<Integer> thisUserMonsterAllEvoIds = getAllEvoIds(thisUserMonsterId);
				
				
				for (Integer backwardsAwokenMaterialsEvolutionChainMonsterId : getAllEvoIds(backwardsAwokenMaterials.get(evolutionMonsterId))) {
					// If anywhere in the evo chain of this monster the material for this backwards evolution if found, pop it out of the list
					if (thisUserMonsterAllEvoIds.contains(backwardsAwokenMaterialsEvolutionChainMonsterId)) {
						System.out.println("pop out " + userMonsterId + " (monsterId = " + thisUserMonsterId + ")");
						userMonsterIdsIterator.remove();
						reorderingMonster = userMonsterId;
						break;
					}
				}
				
				if (0 != reorderingMonster) {
					break;
				}
			}
			
			System.out.println("reordering: " + reorderingMonster);
			System.out.println("evolutionMonsterId: " + evolutionMonsterId);
			
			// If a monster was popped out of the list, add it back in
			if (0 != reorderingMonster) {
				
				userMonsterIds.add(reorderingMonster);
				reorderingMonster = 0;
			}
			
		}
		
		for (Integer id : userMonsterIds) {
			//System.out.println(monsters.get(monsterId));
			
			int monsterId = userMonsterUniqueIdToMonsterDataMap.get(id).get("monster").getAsInt();
			
			if (null != evolutions.get(monsterId)) {
				for (int i = 0; i < evolutions.get(monsterId).size(); i++) {
					if (evolutions.get(monsterId).get(i).getAsJsonObject().get("evolves_to").getAsInt() > monsterId) {
						System.out.println(getEvoStringTo(0, getHighestEvo(monsterId), monsterId) + "\n");
						break;
					}
				}
			} else {
				//System.out.println(monsters.get(monsterId).get("name").getAsString() + " at max evo");
			}
		}
		
	}
	
	private static List<Integer> getAllEvoIds(int monsterId) {
		return new ArrayList<Integer>(getAllEvoIds(getLowestEvo(monsterId, false), new HashSet<Integer>()));
	}
	
	private static HashSet<Integer> getAllEvoIds(int monsterId, HashSet<Integer> evoIds) {
		evoIds.add(monsterId);
		
		if (null != evolutions.get(monsterId)) {
			for (int i = 0; i < evolutions.get(monsterId).getAsJsonArray().size(); i++) {
				
				if (null != evolutions.get(monsterId).getAsJsonArray().get(i).getAsJsonObject().get("evolves_to") 
						&& monsterId < evolutions.get(monsterId).getAsJsonArray().get(i).getAsJsonObject().get("evolves_to").getAsInt()) {
					getAllEvoIds(evolutions.get(monsterId).getAsJsonArray().get(i).getAsJsonObject().get("evolves_to").getAsInt(), evoIds);
				}
			}
		}
		
		return evoIds;
	}
	
	private static int getLowestEvo(int monsterId, boolean log) {
		
		// TODO probably a faster way to compute this with maps
		
		for (Integer i : evolutions.keySet()) {
			JsonArray thisEvolution = evolutions.get(i).getAsJsonArray();
			
			for (int j = 0; j < thisEvolution.size(); j++) {
				if (thisEvolution.get(j).getAsJsonObject().get("evolves_to").getAsInt() == monsterId
						&& i < monsterId) {
					if (log) {
						System.out.println("recurse on " + i);
					}
					
					return getLowestEvo(i, log);
				}
			}
		}
		
		return monsterId;
	}

	private static int getHighestEvo(int monsterId) {
		if (null != evolutions.get(monsterId)) {
			
			int totalHighestEvolution = monsterId;
			
			for (int i = 0; i < evolutions.get(monsterId).size(); i++) {
				if (evolutions.get(monsterId).get(i).getAsJsonObject().get("evolves_to").getAsInt() > totalHighestEvolution) {
					int highestEvolution = getHighestEvo(evolutions.get(monsterId).get(i).getAsJsonObject().get("evolves_to").getAsInt());
					
					if (highestEvolution > totalHighestEvolution) {
						totalHighestEvolution = highestEvolution;
					}
				}
			}
			
			return totalHighestEvolution;
		}
		
		return monsterId;
	}
	
	private static String getEvoStringTo(int depth, int toMonsterId) {
		return getEvoStringTo(depth, toMonsterId, -1);
	}
	
	private static String getEvoStringTo(int depth, int toMonsterId, int fromMonsterId) {

		// TODO determine if user has base monster in evo during chain computation
		
		String evoString = getName(toMonsterId);
		
		for (Integer i : evolutions.keySet()) {
			
			for (int j = 0; j < evolutions.get(i).size(); j++) {
				if (getEvolvesTo(i, j) == toMonsterId && i < toMonsterId) {

					if (hasMaterial(i)) {
						System.out.println(getPrifixString(depth) + "__" + getName(i) + "__ -> " + getName(toMonsterId));
					} else {
						System.out.println(getPrifixString(depth) + getName(i) + " -> " + getName(toMonsterId));
					}
					
					System.out.println(getPrifixString(depth) + getEvoMatsString(i, j));
					
					// Find potential mats for these mats
					JsonArray evoMaterials = evolutions.get(i).get(j).getAsJsonObject().get("materials").getAsJsonArray();
					
					for (int k = 0; k < evoMaterials.size(); k++) {
						
						if (!hasMaterial(evoMaterials.get(k).getAsJsonArray().get(0).getAsInt())) {

							String subEvoString = getEvoStringTo(depth + 1, evoMaterials.get(k).getAsJsonArray().get(0).getAsInt());
							
							//System.out.println(".." + subEvoString);
						}
						
					}
					
					// If this material is in user's box, no need to further calculate evo chain.
					if (hasMaterial(i)) {
						return "__" + getName(i) + "__ -> " + evoString;
					}
					
					// Evolution chain relative start found.
					if (i == fromMonsterId) {
						return "__" + getName(fromMonsterId) + "__ -> " + evoString;
					}
					
					// Calculate previous evo in chain.
					evoString = getEvoStringTo(depth + 1, i, fromMonsterId) + " -> " + evoString;
					break;
				}
			}
		}
		
		return evoString;
	}
	
	public static boolean hasMaterial(int monsterId) {
		
		JsonObject material = materials.get(monsterId);
		
		if (null != material && material.get("count").getAsInt() > 0) {
			int currentCount = material.remove("count").getAsInt();
			material.addProperty("count", --currentCount);
			
			materials.put(monsterId, material);
			
			return true;
		}
		
		// TODO respect ignore list.
		
		// If material is available in user monsters list, mark it as used and return true.
		Set<Integer> availableMonsters = userMonsterIdToMonsterUniqueIdsSetMap.get(monsterId);
		
		if (null != availableMonsters && availableMonsters.size() > 0) {
			
			availableMonsters.remove(Integer.valueOf(availableMonsters.toArray()[0].toString()));
			userMonsterIdToMonsterUniqueIdsSetMap.put(monsterId, availableMonsters);
			
			return true;
		}
		
		
		return false;
		
		//return true;
		
		//return null != userdataJ
	}
	
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
	
	public static String getPrifixString(int depth) {
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
		new AwokenMaterialsCalculator();
	}
	
}
