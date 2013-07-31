package application;

public class Relation {

	String rWords = null;
	int[] rOffsets=null;
	int[] type = null;
	public Relation(String rWords, int[] rOffsets, int [] type){
		this.rWords = rWords;
		this.rOffsets = rOffsets;
		this.type = type;
	}

}
