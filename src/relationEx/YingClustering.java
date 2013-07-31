package relationEx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Write according to the YingClustering in relibrary
 * @author ying
 *
 */
public class YingClustering {
	
	public static final int maxSideContext = 6;// so in fact it should be 5, as it is less than.
	
	public static String[] getSegment(String sent, int startE1, int endE1, int startE2, int endE2){
		String[] segments = new String[3];
		String[] chunks = sent.split(" ");
		ArrayList sentToken = new ArrayList();
		ArrayList sentTokenLemma = new ArrayList();
		ArrayList sentTag = new ArrayList();
		for(int i=0;i<chunks.length;i++){
			String[] chunks2 = chunks[i].split("/");
			if(chunks2.length!=3){
				System.out.println("segment sent error: "+sent+"\t"+startE1+":"+endE1+":"+
						startE2+":"+endE2);
				return null;
			}
			sentToken.add(chunks2[0]);
			sentTokenLemma.add(chunks2[2]);
			sentTag.add(chunks2[1]);
		}
		segments[0] = getContextBefore(sentToken, sentTokenLemma, sentTag, startE1-1);
		segments[1] = getContext(sentToken, sentTokenLemma, sentTag, endE1+1, startE2-1);
		segments[2] = getContextAfter(sentToken, sentTokenLemma, sentTag, endE2+1);
		return segments;
	}
	public static String getContext(ArrayList tokens, ArrayList tokensLemma, ArrayList tags, int start, int end){
		StringBuffer tempContext = new StringBuffer();
		for(int i=start;i<=end;i++){
			String tempToken = (String)tokens.get(i);
			tempContext.append(tempToken+"/");
			String lemma = (String)tokensLemma.get(i);
			String posTag = (String)tags.get(i);
			tempContext.append(posTag).append("/").append(lemma).append(" ");
		}
		return tempContext.toString().trim();
	}
	
	public static String getContextBefore(ArrayList tokens, ArrayList tokensLemma, ArrayList tags, int end){
		int tokenSizeBefore = end+1;
		StringBuffer tempContext = new StringBuffer();
		int i = tokenSizeBefore-YingClustering.maxSideContext;
		if(i<0)
			i=0;
		for(;i<tokenSizeBefore;i++){
			String tempToken = (String)tokens.get(i);
			tempContext.append(tempToken+"/");
			String lemma = (String)tokensLemma.get(i);
			String posTag = (String)tags.get(i);
			tempContext.append(posTag).append("/").append(lemma).append(" ");
		}
		return tempContext.toString().trim();
	}
	
	public static String getContextAfter(ArrayList tokens, ArrayList tokensLemma, ArrayList tags,int start){
		StringBuffer tempContext = new StringBuffer();
		for(int i=start;i<tokens.size() && i<start+YingClustering.maxSideContext;i++){
			String tempToken = (String)tokens.get(i);
			tempContext.append(tempToken+"/");
			String lemma = (String)tokensLemma.get(i);
			String posTag = (String)tags.get(i);
			tempContext.append(posTag).append("/").append(lemma).append(" ");
		}
		return tempContext.toString().trim();
	}
}
