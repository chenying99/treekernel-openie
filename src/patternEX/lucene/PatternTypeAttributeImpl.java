package patternEX.lucene;

import org.apache.lucene.util.AttributeImpl;

public class PatternTypeAttributeImpl extends AttributeImpl implements PatternTypeAttribute {

	private String type = null;
	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String getType() {
		return this.type;
	}

	@Override
	public void clear() {
		this.type = null;
	}

	@Override
	public void copyTo(AttributeImpl arg0) {
		((PatternTypeAttributeImpl) arg0).setType(type);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other instanceof PatternTypeAttributeImpl) {
			return this.type.equals(((PatternTypeAttributeImpl) other).getType());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.type.hashCode();
	}

}
