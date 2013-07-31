package patternEX.lucene;

import org.apache.lucene.util.Attribute;

public interface PatternOffsetAttribute extends Attribute {
	  public void setOffset(String offset);
	  public String getOffset();

}
