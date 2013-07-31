package parseFilter;

/**
 * The offset for a relation pattern in a sentence.
 * @author ying
 *
 */
public class Offset{
	boolean isBefore = false; //check isBefore, or isAfter
	int start1=-1;
	int end1=-1;
	int start2=-1;
	int end2=-1;
	public Offset(int aStart1, int aEnd1, int aStart2, int aEnd2, boolean aIsBefore){
		this.start1 = aStart1;
		this.end1 = aEnd1;
		this.start2 = aStart2;
		this.end2 = aEnd2;
		this.isBefore = aIsBefore;
	}
	
	public int getStart1(){return this.start1;}
	public int getStart2(){return this.start2;}
	public int getEnd1() {return this.end1;}
	public int getEnd2() {return this.end2;}
	public boolean isBefore(){return this.isBefore;}
}
