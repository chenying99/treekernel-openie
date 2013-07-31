package parseFilter;

public class Feature implements Comparable{
	int id;
	String value;
	
	public Feature(int id, String value){
		this.id = id;
		this.value = value;
	}
	
	public int getID(){return id;}
	public String getV(){return value;}

	@Override
	public int compareTo(Object o) {
		if(o instanceof Feature){
			Feature other = (Feature)o;
			if(other.id<id){
				return 1;
			}else if(other.id>id){
				return -1;
			}else
				return 0;
		}
		return -1;
	}

	
	
}
