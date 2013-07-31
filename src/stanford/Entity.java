package stanford;

import java.io.Serializable;

/**
 * Entity
 * 
 * contains information about an entity.
 * 
 * @author Filipe Mesquita <mesquita@cs.ualberta.ca>
 *
 */

public class Entity  implements Serializable{
	
	private static final long serialVersionUID = 1;
	
	public static final String NONE = "NONE";
	public static final String PERSON = "PER";
	public static final String ORGANIZATION = "ORG";
	public static final String LOCATION = "LOC";
	public static final String DATE = "DATE";
	public static final String MONEY = "MNY";
	public static final String PERCENT = "PRC";
	public static final String GPE = "GPE";
	public static final String MISC = "MISC";
	public static final String TIME = "TIME";
	
	/** The id of this entity */
	protected String id;
	
	/** The name of this entity */
	protected String name;
	
	/** The type of this entity */
	protected String type;
	
	public Entity(){
		
	}
	
	/** Creates a new entity with type NONE.
	 * @param id A unique identification for this entity.
	 * @param name The name of this entity. 
	 */
	public Entity(String id, String name){
		this(id, name, NONE);
	}
	
	/** Complete constructor. Creates a new entity with id, name and type.
	 * @param id A unique identification for this entity.
	 * @param name The name of this entity.
	 * @param type THe type of this entity. 
	 */
	public Entity(String id, String name, String type){
		this.id = id;
		this.name = name;
		this.type = type;
	}
	
	@Override
	public String toString(){
		return this.name + "#" + type;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode() + this.type.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		Entity that = (Entity)obj;
		return this.name.equals(that.name) && this.type.equals(that.type);
	}
	
	

}
