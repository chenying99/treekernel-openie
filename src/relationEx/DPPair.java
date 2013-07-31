package relationEx;

/**
 * Dependency pair.
 * HashCode infor is from the start and end of a pair.
 * example: this company (start: company, end: this.)
 * @author ying
 *
 */
public class DPPair implements Comparable{
	int start;//gov
	int end;//target
	String startS;
	String endS;
	
	public DPPair(int aStart, int aEnd){
		this.start = aStart;
		this.end = aEnd;
	}
	
	public DPPair(int aStart, int aEnd, String startS, String endS){
		this.start = aStart;
		this.end = aEnd;
		this.startS = startS;
		this.endS = endS;
	}
	
	public int getStartIndex(){return this.start;}
	public int getEndIndex(){return this.end;}
	public String getStartToken(){return this.startS;}
	public String getEndToken(){return this.endS;}

	@Override
	public int compareTo(Object arg0) {
		if(arg0 instanceof DPPair){
			DPPair other = (DPPair)arg0;
			if(start<other.start)
				return -1;
			else if(start>other.start)
				return 1;
			else{
				if(end<other.end)
					return -1;
				else if(end>other.end)
					return 1;
				else
					return 0;
			}
		}
		return -1;
	}
	
	public int hashCode() {
		String offset = start+":"+end;
        return offset.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;

        DPPair other = (DPPair) obj;
        if(start==other.start && end == other.end)
        	return true;
        return false;
    }


}
