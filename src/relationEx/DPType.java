package relationEx;

/**
 * instance of dependency type, including its direction
 * @author ying
 *
 */
public class DPType {
	String type;
	int dir;
	
	public DPType(String type, int dir){
		this.type = type;
		this.dir = dir;
	}
	
	public String getType(){return this.type;}
	public int getDir(){return this.dir;}
}
