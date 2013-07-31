package patternEX.lucene;

import org.apache.lucene.util.AttributeImpl;

public class PatternOffsetAttributeImpl extends AttributeImpl implements PatternOffsetAttribute {
	private String offset = null;
	@Override
	public void setOffset(String offset) {
		this.offset = offset;
	}

	@Override
	public String getOffset() {
		return this.offset;
	}

	@Override
	public void clear() {
		this.offset = null;
	}

	@Override
	public void copyTo(AttributeImpl arg0) {
		((PatternOffsetAttribute) arg0).setOffset(offset);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other instanceof PatternOffsetAttributeImpl) {
			return this.offset.equals(((PatternOffsetAttributeImpl) other).getOffset());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.offset.hashCode();
	}
}
