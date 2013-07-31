package reverb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Normalize relation strings, removing inflection, auxiliary verbs, adjectives, and adverbs
 * @author ying
 *
 */
public class RelationNormalizer {
	private static HashSet<String> verbIgnorePosTags;
	private static HashSet<String> nounIgnorePosTags;
	private static HashSet<String> auxVerbs;
	
	public RelationNormalizer() {
		if(verbIgnorePosTags==null){
		verbIgnorePosTags = new HashSet<String>();
		verbIgnorePosTags.add("MD"); // can, must, should
		verbIgnorePosTags.add("DT"); // the, an, these
		verbIgnorePosTags.add("PDT"); // predeterminers 
		verbIgnorePosTags.add("WDT"); // wh-determiners
		verbIgnorePosTags.add("JJ"); // adjectives
		verbIgnorePosTags.add("JJR"); 
		verbIgnorePosTags.add("JJS"); 
		verbIgnorePosTags.add("RB"); // adverbs
		verbIgnorePosTags.add("RBR"); 
		verbIgnorePosTags.add("RBS"); 
		verbIgnorePosTags.add("PRP$"); // my, your, our
		
		nounIgnorePosTags = new HashSet<String>();
		nounIgnorePosTags.add("JJ"); // adjectives
		nounIgnorePosTags.add("JJR"); 
		nounIgnorePosTags.add("JJS"); 
		
		auxVerbs = new HashSet<String>();
		auxVerbs.add("be");
		auxVerbs.add("have");
		auxVerbs.add("do");
		}
	}
	
	/**
	 * Input a verb phrase.
	 * Return normalized result {tokenS, posTagS}
	 * @param tokensS
	 * @param posTagS
	 * @return
	 */
	public String[] removeIgnoredPosTagsVerb(String tokensS,String posTagS){
		if(tokensS.length()==0)
			return null;
		String[] chunksToken = tokensS.split(" ");
		if(tokensS.length()==1)
			return new String[]{tokensS,posTagS};
		ArrayList<String> tokensArrayList = new ArrayList( Arrays.asList(chunksToken));
		String[] posList = posTagS.split(" ");
		ArrayList<String> posTagList = new ArrayList(Arrays.asList(posList));
		removeIgnoredPosTagsVerb(tokensArrayList, posTagList);
		removeLeadingBeHave(tokensArrayList, posTagList);
		if(tokensArrayList.size()>0){
			StringBuffer tempToken = new StringBuffer();
			StringBuffer tempTag = new StringBuffer();
			for(int i=0;i<tokensArrayList.size();i++){
				tempToken.append(tokensArrayList.get(i)).append(" ");
				tempTag.append(posTagList.get(i)).append(" ");
			}
			return new String[]{tempToken.toString().trim(),tempTag.toString().trim()};
		}else
			return null;
	}
	
	/**
	 * Input a noun phrase.
	 * Return normalized phrase, which deleted adjectives.
	 * @param tokensS
	 * @param posTagS
	 * @return
	 */
	public String[] removeIgnoredPosTagsNoun(String tokensS,String posTagS){
		if(tokensS.length()==0)
			return null;
		String[] chunksToken = tokensS.split(" ");
		if(tokensS.length()==1)
			return new String[]{tokensS,posTagS};
		ArrayList<String> tokensArrayList = new ArrayList( Arrays.asList(chunksToken));
		String[] posList = posTagS.split(" ");
		ArrayList<String> posTagList = new ArrayList(Arrays.asList(posList));
		removeIgnoredPosTagsNoun(tokensArrayList, posTagList);
		if(tokensArrayList.size()>0){
			StringBuffer tempToken = new StringBuffer();
			StringBuffer tempTag = new StringBuffer();
			for(int i=0;i<tokensArrayList.size();i++){
				tempToken.append(tokensArrayList.get(i)).append(" ");
				tempTag.append(posTagList.get(i)).append(" ");
			}
			return new String[]{tempToken.toString().trim(),tempTag.toString().trim()};
		}else
			return null;
	}
	
	public void removeIgnoredPosTagsVerb(ArrayList<String> tokens, ArrayList<String> posTags) {
		int i = 0;
		while (i < posTags.size()) {
			if (verbIgnorePosTags.contains(posTags.get(i))) {
				tokens.remove(i);
				posTags.remove(i);
			} else {
				i++;
			}
		}
	}
	
	public void removeIgnoredPosTagsNoun(ArrayList<String> tokens, ArrayList<String> posTags) {
		int i = 0;
		while (i < posTags.size()) {
			if (nounIgnorePosTags.contains(posTags.get(i))) {
				tokens.remove(i);
				posTags.remove(i);
			} else {
				i++;
			}
		}
	}
	
	public void removeLeadingBeHave(ArrayList<String> tokens, ArrayList<String> posTags) {
		int lastVerbIndex = -1;
		int n = tokens.size();
		for (int i = 0; i < n; i++) {
			String tag = posTags.get(n-i-1);
			if (tag.startsWith("V")) {
				lastVerbIndex = n-i-1;
				break;
			}
		}
		if (lastVerbIndex < 0) return;
		int i = 0;
		while (i < lastVerbIndex) {
			String tok = tokens.get(i);
			if (i+1 < posTags.size() && !posTags.get(i+1).startsWith("V")) break;
			if (auxVerbs.contains(tok)) {
				tokens.remove(i);
				posTags.remove(i);
				lastVerbIndex--;
			} else {
				i++;
			}
		}
	}
	
	public boolean isAuxVB(String word){
		if(auxVerbs.contains(word))
			return true;
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
