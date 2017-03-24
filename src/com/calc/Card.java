package com.calc;

import java.util.ArrayList;
import java.util.List;

public class Card {
	private List<Card> materials;
	private int id;
	
	public Card(int id) {
		this.id = id;
		this.materials = new ArrayList<Card>();
		
		System.out.println("Create card id=" + this.id);
	}
	
	public void addMaterial(Card material) {
		System.out.println("Add " + material.getId() + " as material to " + this.id);
		this.materials.add(material);
	}
	
	public List<Card> getMaterials() {
		return this.materials;
	}
	
	public int getId() {
		return this.id;
	}
	
	@Override
	public String toString() {
		return String.valueOf(this.id + ":" + this.materials);
	}
}
