package patternEX;

public class Gram implements Comparable{
	String gram;
	int freq;
	
	public Gram(String aGram, int aFreq){
		this.gram = aGram;
		this.freq = aFreq;
	}

	@Override
	public int compareTo(Object arg0) {
		Gram other = (Gram)arg0;
		if(other.freq<this.freq)
			return -1;
		else if(other.freq>this.freq)
			return 1;
		else 
			return 0;
	}
	
	public String getGram(){return this.gram;}
	public int getFreq(){return this.freq;}

}
