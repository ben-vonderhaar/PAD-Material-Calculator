package com.calc;

import java.util.HashMap;
import java.util.Map;

public class MaterialList {
	
	private Card card;
	
	private Map<Integer, Integer> materials;
	
	public MaterialList(Card card) {
		this.card = card;
		this.materials = new HashMap<Integer, Integer>();
		
		this.processCard(this.card);
		
		System.out.println(materials);
	}
	
	public Map<Integer, Integer> getMaterials() {
		return this.materials;
	}
	
	private void processCard(Card card) {
		
		System.out.println(card);
		
		if (card.getMaterials().size() == 0) {
			if (null == materials.get(card.getId())) {
				materials.put(card.getId(), 1);
			} else {
				materials.put(card.getId(), materials.get(card.getId()) + 1);
			}
		}
		
		for (int i = 0; i < card.getMaterials().size(); i++) {
			
			processCard(card.getMaterials().get(i));
		}
	}
}
