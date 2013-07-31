package reverb;

import java.util.regex.*;

/**
 * Check if a string's POS matches the phrase pattern.
 * @author ying
 *
 */
public class PhraseMatch {
	
	public static final String Apattern = "(JJ|JJR|JJS)";
	public static final String Npattern = "(NN|NNS|NNP|NNPS)"; //"(NN|NNS)";
	public static final String Ppattern = "(IN|RP|TO)";
	public static final String POSpattern = "(POS)"; // possessive ending
	public static final String Vpattern = "(VB|VBD|VBG|VBN|VBP|VBZ)";
	
	//The noun phrase pattern in c/nc values + (preposition)?
	public static final String ps_np2 = "(("+Apattern+"|"+Npattern+") )*"+Npattern+"( IN)?";
	public static final String ps_posp = POSpattern+" "+"(("+Apattern+"|"+Npattern+") )*"+Npattern;
	public static final String ps_vp0 = Vpattern;
	public static final String ps_vp1 = Vpattern+" "+Npattern;
	public static final String ps_vp2 = Vpattern+" "+Ppattern;
	
    /**
     * Definition of the "verb" of the relation pattern.
     */
    public static final String VERB = 
        "(RB )?(MD|VB|VBD|VBP|VBZ|VBG|VBN)( RP)?( RB)?";

    /**
     * Definition of the "any word" part of the relation pattern.
     */
    public static final String WORD = 
        "(PRP\\$|CD|DT|JJ|JJS|JJR|NN|NNS|POS|PRP|RB|RBR|RBS|VBN|VBG)";

    /**
     * Definition of the "preposition" part of the relation pattern.
     */
    public static final String PREP = "(RB )?(IN|TO|RP)( RB)?";


    // The long pattern is (V(W*P)?)+
    public static final String LONG_RELATION_PATTERN ="("+VERB+"( ("+WORD+" )*"+PREP+")? )*"+VERB+"( ("+WORD+" )*"+PREP+")?";
    
    
    // The short pattern is (VP?)+
    public static final String SHORT_RELATION_PATTERN = "("+VERB+"( "+PREP+")? )*"+VERB+"( "+PREP+")?";
	
	public static boolean matchColon(String tempWord){
		if(tempWord.length()==0)
			return true;
		if(tempWord.equals(",")||tempWord.equals("(")||tempWord.equals("(")){
			return true;
		}
		return false;
	}
	
	public static boolean matchPP(String POS){
		if(POS.length()==0)
			return true;
		if(POS.equals("IN") || POS.equals("RP") || POS.equals("POS"))
			return true;
		return false;
	}
	
	public static boolean containPP(String POS){
		if(POS.length()==0)
			return false;
		if(POS.contains("IN") || POS.contains("RP") || POS.contains("POS"))
			return true;
		return false;
	}
	
	/**
	 * match noun phrases.
	 * which can be adj+noun; noun + preposition
	 * @param POS
	 * @return
	 */
	public static boolean matchNP(String POS){
		if(POS.length()==0)
			return true;
		//1. extract noun phrases tags
		return Pattern.matches(ps_np2,POS);
	}
	
	
	/**
	 * matches possessive+noun
	 * @param POS
	 * @return
	 */
	public static boolean matchPOSNP(String POS){
		if(POS.length()==0)
			return true;
		//1. extract possessive noun phrases tags
		return Pattern.matches(ps_posp,POS);
	}
	
	public static boolean matchVP(String POS){
		if(POS.length()==0)
			return true;
		String posS = POS;
		if(Pattern.matches(ps_vp0,posS))
			return true;
		if(Pattern.matches(ps_vp1,posS))
			return true;
		return Pattern.matches(ps_vp2,posS);
	}
	
	/**
	 * Matches reverb system's verb.
	 * @param POS
	 * @return
	 */
	public static boolean matchVPRV(String POS){
		if(POS.length()==0)
			return true;
		String posS = POS;
		if(Pattern.matches(SHORT_RELATION_PATTERN,posS))
			return true;
		return Pattern.matches(LONG_RELATION_PATTERN,posS);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*String POS = "JJ NNS IN";
		System.out.println(PhraseMatch.matchNP(POS));
		POS = "VBD";
		System.out.println(Pattern.matches(Vpattern, POS));
		POS = "RB VBD";
		System.out.println(Pattern.matches(VERB, POS));
		System.out.println(Pattern.matches(VERB, POS));
		POS = "PRP$";
		System.out.println(Pattern.matches(WORD, POS));*/
		String POS = "RB VBD TO VB";//"VBD NN/no";//"VBD RB/yes";
		System.out.println(Pattern.matches(SHORT_RELATION_PATTERN,POS));
		System.out.println(Pattern.matches(LONG_RELATION_PATTERN,POS));
	}

}
