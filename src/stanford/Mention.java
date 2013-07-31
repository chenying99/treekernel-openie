package stanford;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Mention
 * 
 * contains information about a mention to an entity in a sentence. 
 * A mention is the list of tokens in a sentence that refer to an entity.
 * A sentence may contain several mentions to the same entity.
 * 
 *  <ul>
 *  <li>Name mentions: the mention uses a proper name to refer to the entity.
 *  <li>Nominal mentions: the mention is a noun phrase whose head is a common noun.
 *  <li>Pronominal mentions: the mention is a headless noun phrase, or a noun phrase 
 *  whose head is a pronoun, or a possessive pronoun.
 *  <li>Prenominal mentions: the mention occurs in a modifying position before another noun.
 *  </ul>
 * 
 * @see relibrary.data.Entity
 * @see relibrary.data.Sentence  
 *  
 * @author Filipe Mesquita <mesquita@cs.ualberta.ca>
 *
 */

public class Mention implements Comparable<Mention>, Serializable{

	private static final long serialVersionUID = 1;


	public static final String NAME = "NAM";
	public static final String NOMINAL = "NOM";
	public static final String PROMONIAL = "PRO";
	public static final String PRENOMINAL = "PRE";
	public static final String NONE = "NONE";

	/** The entity referred by this mention */
	protected Entity entity;

	protected String type; 
	
	int start;
	int end;
	
	ArrayList tokens = null;
	
	String chunk = null;

	/** 
	 * Creates a mention with no sentence. Its sentence will be set when this mention is added into a sentence.   
	 * 
	 * @param entity The entity referred by this mention.
	 * @param type The mention type (e.g., NAME, PRONOUN).
	 * @param startToken The position of the first token of this mention in the sentence above.
	 * @param endToken The position of the last token of this mention in the sentence above.
	 * @param tokens: the tokens of the whole sentences.
	 */
	public Mention(Entity entity, String type, int startToken, int endToken, ArrayList tokens){
		this.entity = entity;
		this.type = type;
		this.start = startToken;
		this.end = endToken;
		this.tokens = tokens;
		
		StringBuffer buffer = new StringBuffer(10);
		for(int i=start;i<=end;i++){
			buffer.append(tokens.get(i) + " ");
		}
		this.chunk = buffer.toString();
	}

	public String getChunk(){
		return chunk;
	}

	public String toString(){
		return entity.toString() + " : " + entity.getId();
	}

	public int getStartToken(){
		return this.start;
	}

	public int getEndToken(){
		return this.end;
	}


	
	public Entity getEntity() {
		return entity;
	}
	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	

	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	@Override
	public int hashCode() {

		if(this.chunk==null){
			return 0;
		}

		return this.chunk.hashCode();
	}

	@Override 
	public boolean equals(Object aThat) {
		//check for self-comparison
		if ( this == aThat ) return true;

		//use instanceof instead of getClass here for two reasons
		//1. if need be, it can match any supertype, and not just one class;
		//2. it renders an explict check for "that == null" redundant, since
		//it does the check for null already - "null instanceof [type]" always
		//returns false. (See Effective Java by Joshua Bloch.)
		if ( !(aThat instanceof Mention) ) return false;
		//Alternative to the above line :
		//if ( aThat == null || aThat.getClass() != this.getClass() ) return false;

		//cast to native object is now safe
		Mention that = (Mention) aThat;

		// in order keep hashCode, equals and compareTo consistent... 
		if(this.chunk==null && that.chunk==null){
			return true;
		}

		if(this.chunk==null || that.chunk==null){
			return false;
		}

		return this.chunk.equals(that.chunk);
	}

	public int compareTo(Mention that) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		if(this == that) return EQUAL;
		if(that == null) return BEFORE;

		// in order keep hashCode, equals and compareTo consistent...
		if(this.chunk==null && that.chunk==null) return EQUAL;
		if((this.chunk==null) && !(that.chunk==null)) return AFTER;
		if(!(this.chunk==null) && (that.chunk==null)) return BEFORE;

		return this.chunk.compareTo(that.chunk);
	}



}
