/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package darkuniverseapp;

import java.util.*;

/**
 *
 * @author coolvibe
 */
public class Player implements java.io.Serializable {

	// Player structure for serializing
	static final long serialVersionUID = 42L;
	public String playername;
	public String playernotes;
        public String baseclass;
	public int resources;
	public int karma;
	public int health;
	public int initiative;
	// FASERIP
	public int fighting;
	public int agility;
	public int strength;
	public int endurance;
	public int reason;
	public int intuition;
	public int psycho;
	public ArrayList Powers;
}
