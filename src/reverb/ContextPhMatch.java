package reverb;

import java.util.ArrayList;

/**
 * Check if a pos string is in the required form.
 * There are some minor difference among different positions. So it has two functions.
 * @author ying
 *
 */
public class ContextPhMatch {
	public static final int PP = 0;//preposition phrase
	public static final int NP = 1;//noun phrase
	public static final int VP = 2;//verb phrase
	public static final int illegal = -1;
	
	
	/**
	 * for side context, only before context after I think about it.
	 * Because there might be <A> is <B>'s mother
	 * @param POS
	 * @return
	 */
	public static int sdPhMatch(String POS){
		if(POS.length()==0)
			return illegal;
		if(PhraseMatch.matchNP(POS))
			return NP;
		else if(PhraseMatch.matchVPRV(POS))
			return VP;
		else if(PhraseMatch.matchPP(POS))
			return PP;
		else
			return illegal;
	}
	
	/**
	 * for between context
	 * @param POS
	 * @return
	 */
	public static int btPhMatch(String POS){
		if(POS.length()==0)
			return illegal;
		if(PhraseMatch.matchPOSNP(POS))
			return NP;
		else if(PhraseMatch.matchNP(POS))
			return NP;
		else if(PhraseMatch.matchVPRV(POS))
			return VP;
		else if(PhraseMatch.matchPP(POS))
			return PP;
		else
			return illegal;
	}

}
