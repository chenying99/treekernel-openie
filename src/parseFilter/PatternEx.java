package parseFilter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Matcher;

import reverb.ContextPhMatch;
import reverb.RelationNormalizer;

/**
 * Extract relation candidates from sentences. Very similar to the one in PhrasePTokenizer, except pattern
 * positions are extracted too.
 * @author ying
 *
 */
public class PatternEx {
	
	int nGram = 0; // the order of nGram.
	int windowSize = 4;//the window size for before, after context
	public static final String punc = ".!?;";
	public String type1="NNP";
	public String type2="NNP";
	public static RelationNormalizer normalizer = new RelationNormalizer();
	HashSet stopWords = null;
	int btSize = 5; //when between size is larger than 5, ignore the before and after
	PrintStream output = null;
	public PatternEx(int nGram, HashSet astopWordsList){
		this.stopWords = astopWordsList;
		this.nGram = nGram;
	}
	
	public void setPrintOut(PrintStream out){
		this.output = out;
	}
	
	/**
	 * tokenize the context, lower case all the words
	 * all consecutive n-grams which can be relation words will be extracted.
	 * The relations are saved as a HashMap. key is the pattern string, value is the Offset instance of the pattern.
	 * @param aContext
	 * @return
	 */
	public HashMap tokenize(ArrayList tokenBfRaw, ArrayList tagBfRaw, ArrayList indexBfRaw,
			ArrayList tokenBtRaw, ArrayList tagBtRaw, ArrayList indexBtRaw,
			ArrayList tokenAfRaw, ArrayList tagAfRaw, ArrayList indexAfRaw){
		
		HashMap grams = new HashMap();
		try{
			ArrayList tokenBf = new ArrayList();
			ArrayList tokenBt= new ArrayList();
			ArrayList indexBf = new ArrayList();
			ArrayList indexBt = new ArrayList();
			ArrayList tagBf = new ArrayList();
			ArrayList tagBt = new ArrayList();
			this.tokenCtxt(tokenBtRaw, tagBtRaw, indexBtRaw, tokenBt,tagBt, indexBt);
			if(tokenBt.size()>=this.btSize){//ignore side context if the between context is long enough
				tokenBfRaw=null;
				tokenAfRaw = null;
			}
			this.tokenBefore(tokenBfRaw, tagBfRaw, indexBfRaw, tokenBf, tagBf, indexBf);
			ArrayList tokenAf = new ArrayList();
			ArrayList tagAf = new ArrayList();
			ArrayList indexAf = new ArrayList();
			if(tokenAfRaw!=null && tokenAfRaw.size()>0){
				this.tokenAfter(tokenAfRaw, tagAfRaw, indexAfRaw, tokenAf, tagAf, indexAf );
			}
			//extract patterns which has words in before and/or between
			this.exPtSentBefore(grams, tokenBf, tokenBt,tagBf, tagBt, indexBf, indexBt, this.type1, this.type2) ;
			//extract patterns which has words in between and after
			if(tokenAfRaw!=null && tokenAfRaw.size()>0){
				this.exPtSentAfter(grams, tokenBt,tokenAf, tagBt,tagAf, indexBt, indexAf, this.type1, this.type2);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return grams;
	}
	
	/**
	 * Generate all n to 1 continuous grams that matches some phrases form from the before and between context.
	 * Save it in a HashMap, key is the pattern string, value is the Offset instance of the pattern.
	 * There might be one pattern with several offset, as some JJ  or RB will be ignored. Then choose the longest span.
	 * @param set
	 * @param tokenBf
	 * @param tokenContext
	 * @param tagBf
	 * @param tagContext
	 * @param tempType1
	 * @param tempType2
	 */
	public void exPtSentBefore(HashMap set, ArrayList tokenBf, ArrayList tokenBt, ArrayList tagBf, ArrayList tagBt, ArrayList indexBf, ArrayList indexBt, String tempType1, String tempType2){
		//output.println("in exPtSentBefore()");
		HashSet illegalSet = new HashSet();//record illegal POS pattern, so we don't need to check again.
		int sizeAll = tokenBf.size()+tokenBt.size();
		for(int tempGram=this.nGram;tempGram>0;tempGram--){
			if(sizeAll>=tempGram){
				//output.println("tempGram: "+tempGram);
				for(int i=0;i<sizeAll-tempGram+1;i++){
					//get the candidate patterns n-gram.
					int startBfTemp = -1;
					int endBfTemp = -1;
					int startBtTemp = -1;
					int endBtTemp = -1;
					StringBuffer patternBf = new StringBuffer();
					StringBuffer tagTempBf = new StringBuffer();
					StringBuffer patternBt = new StringBuffer();
					StringBuffer tagTempBt = new StringBuffer();
					int length = 0;
					for(int j=i;j<sizeAll && j<i+tempGram;j++){
						if(j<tokenBf.size()){
							patternBf.append(tokenBf.get(j)).append(" ");
							tagTempBf.append(tagBf.get(j)).append(" ");
							length++;
							if(startBfTemp==-1)
								startBfTemp = (Integer)indexBf.get(j);
							endBfTemp = (Integer)indexBf.get(j);
						}else{
							int index2 = j-tokenBf.size();
							patternBt.append(tokenBt.get(index2)).append(" ");
							tagTempBt.append(tagBt.get(index2)).append(" ");
							length++;
							if(startBtTemp==-1)
								startBtTemp = (Integer)indexBt.get(index2);
							endBtTemp = (Integer)indexBt.get(index2);
						}
					}
					if(length==tempGram){
						
						//check if it is in the right form
						String patternBfS = patternBf.toString().trim();
						String tagTempBfS = tagTempBf.toString().trim();
						String patternBtS = patternBt.toString().trim();
						String tagTempBtS = tagTempBt.toString().trim();
						
						//check if it is already in the set
						String patternTemp = patternBfS + " <"+tempType1+"> "+patternBtS+" <"+tempType2+">";
						Object ob = set.get(patternTemp);
						if(ob!=null){
							//update the offset.
							Offset old = (Offset)ob;
							this.updateOffset(old, startBfTemp, endBfTemp, startBtTemp, endBtTemp);
							continue;
						}
						
						//System.out.println("bf:"+patternBfS+"\t"+tagTempBfS);
						//System.out.println("bt:"+patternBtS+"\t"+tagTempBtS);
						//check if it is in the right form
						int typeBf = -1;
						if(tagTempBfS.length()>0&& !illegalSet.contains(tagTempBfS))
							typeBf = ContextPhMatch.sdPhMatch(tagTempBfS);
						//System.out.println("typeBf:"+typeBf+"/"+tagTempBfS+"/"+patternBfS);
						int typeBt = -1;
						if(tagTempBtS.length()>0 && !illegalSet.contains(tagTempBtS))
							typeBt =  ContextPhMatch.btPhMatch(tagTempBtS);
						//System.out.println("typeBt:"+typeBt+"/"+tagTempBtS+"/"+patternBtS);
						
						if(typeBf<0 && typeBt<0){//no noun or verb
							if(tagTempBfS.length()>0)
								illegalSet.add(tagTempBfS);
							if(tagTempBtS.length()>0)
								illegalSet.add(tagTempBtS);
						}else if(typeBf<0 && typeBt>=0){
							if(tagTempBfS.length()>0)
								illegalSet.add(tagTempBfS);
							if(typeBt>0){//it has to have a verb or noun
								//filter the JJs, put it into the set.
								String[] finalResult = null;
								if(typeBt==ContextPhMatch.NP)
									finalResult = normalizer.removeIgnoredPosTagsNoun(patternBtS, tagTempBtS);
								else
									finalResult = normalizer.removeIgnoredPosTagsVerb(patternBtS, tagTempBtS);
								if(finalResult!=null && finalResult[0].length()>0){
									String resultP = "<"+tempType1+"> "+finalResult[0]+" <"+tempType2+">";
									this.addPattern(set, resultP, startBfTemp, endBfTemp, startBtTemp, endBtTemp,true);
								}
							}
						}else if(typeBf>=0 && typeBt<0){
							if(tagTempBtS.length()>0)
								illegalSet.add(tagTempBtS);
							if(typeBf>0){//it has to have a verb or noun
								//filter the JJs, put it into the set.
								String[] finalResult = null;
								if(typeBf==ContextPhMatch.NP)
									finalResult = normalizer.removeIgnoredPosTagsNoun(patternBfS, tagTempBfS);
								else
									finalResult = normalizer.removeIgnoredPosTagsVerb(patternBfS, tagTempBfS);
								if(finalResult!=null &&finalResult[0].length()>0){
									String resultP = finalResult[0]+" <"+tempType1+"> "+"<"+tempType2+">";
									this.addPattern(set, resultP, startBfTemp, endBfTemp, startBtTemp, endBtTemp, true);
								}
							}
						}else{
							if(typeBt+typeBf>0){//it has to have a verb or noun
								//filter the JJs, put it into the set.
								String[] finalResultBf = null;
								if(typeBf==ContextPhMatch.NP)
									finalResultBf = normalizer.removeIgnoredPosTagsNoun(patternBfS, tagTempBfS);
								else if(typeBf == ContextPhMatch.VP)
									finalResultBf = normalizer.removeIgnoredPosTagsVerb(patternBfS, tagTempBfS);
								String[] finalResultBt = null;
								if(typeBt==ContextPhMatch.NP)
									finalResultBt = normalizer.removeIgnoredPosTagsNoun(patternBtS, tagTempBtS);
								else if(typeBt == ContextPhMatch.VP)
									finalResultBt = normalizer.removeIgnoredPosTagsVerb(patternBtS, tagTempBtS);
								String resultP = new String();
								if(finalResultBf!=null && finalResultBf[0].length()>0)
									resultP +=finalResultBf[0]+" ";
								resultP+="<"+tempType1+"> ";
								if(finalResultBt!=null && finalResultBt[0].length()>0){
									resultP +=finalResultBt[0]+" ";
								}
								resultP +="<"+tempType2+">";
								this.addPattern(set, resultP, startBfTemp, endBfTemp, startBtTemp, endBtTemp, true);
							}
						}
					}

				}
			}
		}
	}
	
	public void exPtSentAfter(HashMap set, ArrayList tokenBt, ArrayList tokenAf, ArrayList tagBt, ArrayList tagAf, ArrayList indexBt, ArrayList indexAf,String tempType1, String tempType2){
		HashSet illegalSet = new HashSet();//record illegal POS pattern, so we don't need to check again.
		int sizeAll = tokenAf.size()+tokenBt.size();
		for(int tempGram=this.nGram;tempGram>0;tempGram--){
			if(sizeAll>=tempGram){
				for(int i=0;i<sizeAll-tempGram+1;i++){
					//get the candidate patterns.
					StringBuffer patternAf = new StringBuffer();
					StringBuffer tagTempAf = new StringBuffer();
					StringBuffer patternBt = new StringBuffer();
					StringBuffer tagTempBt = new StringBuffer();
					int startBtTemp = -1;
					int endBtTemp = -1;
					int startAfTemp = -1;
					int endAfTemp = -1;
					int length = 0;
					for(int j=i;j<sizeAll && j<i+tempGram;j++){
						if(j<tokenBt.size()){
							patternBt.append(tokenBt.get(j)).append(" ");
							tagTempBt.append(tagBt.get(j)).append(" ");
							length++;
							if(startBtTemp==-1)
								startBtTemp = (Integer)indexBt.get(j);
							endBtTemp = (Integer)indexBt.get(j);
						}else{
							int index2 = j-tokenBt.size();
							patternAf.append(tokenAf.get(index2)).append(" ");
							tagTempAf.append(tagAf.get(index2)).append(" ");
							length++;
							if(startAfTemp == -1)
								startAfTemp = (Integer)indexAf.get(index2);
							endAfTemp = (Integer)indexAf.get(index2);
						}
					}
					if(length==tempGram){//check if it is in the right form
						String patternAfS = patternAf.toString().trim();
						String tagTempAfS = tagTempAf.toString().trim();
						String patternBtS = patternBt.toString().trim();
						String tagTempBtS = tagTempBt.toString().trim();
						//System.out.println("bt:"+patternBtS+"\t"+tagTempBtS);
						//System.out.println("Af:"+patternAfS+"\t"+tagTempAfS);
						
						//check if it is already in the set
						String patternTemp = "<"+tempType1+"> "+patternBtS+" <"+tempType2+"> "+patternAfS;
						Object ob = set.get(patternTemp);
						if(ob!=null){
							//update the offset.
							Offset old = (Offset)ob;
							this.updateOffset(old, startBtTemp, endBtTemp, startAfTemp, endAfTemp);
							continue;
						}
						
						int typeBt = -1;
						if(tagTempBtS.length()>0 && !illegalSet.contains(tagTempBtS))
							typeBt =  ContextPhMatch.btPhMatch(tagTempBtS);
						//System.out.println("typeBt:"+typeBt+"/"+tagTempBtS+"/"+patternBtS);
						int typeAf = -1;
						//Because there might be <A> is <B>'s mother
						if(tagTempAfS.length()>0&& !illegalSet.contains(tagTempAfS))
							typeAf = ContextPhMatch.btPhMatch(tagTempAfS);
						//System.out.println("typeAf:"+typeAf+"/"+tagTempAfS+"/"+patternAfS);
						
						if(typeAf<0 && typeBt<0){
							if(tagTempAfS.length()>0)
								illegalSet.add(tagTempAfS);
							if(tagTempBtS.length()>0)
								illegalSet.add(tagTempBtS);
						}else if(typeAf<0 && typeBt>=0){
							if(tagTempAfS.length()>0)
								illegalSet.add(tagTempAfS);
							if(typeBt>0){//it has to have a verb or noun
								//filter the JJs, put it into the set.
								String[] finalResult = null;
								if(typeBt==ContextPhMatch.NP)
									finalResult = normalizer.removeIgnoredPosTagsNoun(patternBtS, tagTempBtS);
								else
									finalResult = normalizer.removeIgnoredPosTagsVerb(patternBtS, tagTempBtS);
								if(finalResult!=null && finalResult[0].length()>0){
									String resultP = "<"+tempType1+"> "+finalResult[0]+" <"+tempType2+">";
									this.addPattern(set, resultP, startBtTemp, endBtTemp, startAfTemp, endAfTemp, false);
								}
							}
						}else if(typeAf>=0 && typeBt<0){
							if(tagTempBtS.length()>0)
								illegalSet.add(tagTempBtS);
							if(typeAf>0){//it has to have a verb or noun
								//filter the JJs, put it into the set.
								String[] finalResult = null;
								if(typeAf==ContextPhMatch.NP)
									finalResult = normalizer.removeIgnoredPosTagsNoun(patternAfS, tagTempAfS);
								else
									finalResult = normalizer.removeIgnoredPosTagsVerb(patternAfS, tagTempAfS);
								if(finalResult!=null &&finalResult[0].length()>0){
									String resultP = "<"+tempType1+"> "+"<"+tempType2+"> "+finalResult[0];
									this.addPattern(set, resultP, startBtTemp, endBtTemp, startAfTemp, endAfTemp, false);
								}
							}
						}else{
							if(typeBt+typeAf>0){//it has to have a verb or noun
								//filter the JJs, put it into the set.
								String[] finalResultAf = null;
								if(typeAf==ContextPhMatch.NP)
									finalResultAf = normalizer.removeIgnoredPosTagsNoun(patternAfS, tagTempAfS);
								else if(typeAf ==ContextPhMatch.VP)
									finalResultAf = normalizer.removeIgnoredPosTagsVerb(patternAfS, tagTempAfS);
								String[] finalResultBt = null;
								if(typeBt==ContextPhMatch.NP)
									finalResultBt = normalizer.removeIgnoredPosTagsNoun(patternBtS, tagTempBtS);
								else if(typeBt ==ContextPhMatch.VP)
									finalResultBt = normalizer.removeIgnoredPosTagsVerb(patternBtS, tagTempBtS);
								String resultP = new String();
								resultP+="<"+tempType1+"> ";
								if(finalResultBt!=null && finalResultBt[0].length()>0)
									resultP +=finalResultBt[0]+" ";
								resultP +="<"+tempType2+">";
								if(finalResultAf!=null && finalResultAf[0].length()>0){
									resultP +=" "+finalResultAf[0];
								}
								
								this.addPattern(set, resultP, startBtTemp, endBtTemp, startAfTemp, endAfTemp, false);
							}
						}
					}

				}
			}
		}
	}
	
	public void updateOffset(Offset old, int start1, int end1, int start2, int end2){
		if(old.start1==-1 || old.start1>start1)
			old.start1 = start1;
		if(old.end1==-1 || old.end1<end1)
			old.end1 = end1;
		if(old.start2==-1 || old.start2>start2)
			old.start2 = start2;
		if(old.end2==-1 || old.end2<end2)
			old.end2 = end2;
	}
	
	/**
	 * add a pattern into the set. If it is already there, update the range, using the widest one.
	 * If it is not, add the pattern into the HashMap.
	 * @param set
	 * @param result
	 * @param start1
	 * @param end1
	 * @param start2
	 * @param end2
	 */
	public void addPattern(HashMap set, String result, int start1, int end1, int start2,int end2, boolean isBefore ){
		Object ob = set.get(result);
		if(ob!=null){
			//update the offset.
			Offset old = (Offset)ob;
			this.updateOffset(old, start1, end1, start2, end2);
		}else{
			set.put(result, new Offset(start1,end1,start2,end2,isBefore));
		}
	}
	
	
	/**
	 * Get the tokens before e1 within window size 
	 * @param s1
	 * @param tokens
	 * @param tags
	 */
	public void tokenBefore(ArrayList tokenRaw, ArrayList tagRaw,ArrayList indexRaw, ArrayList tokens, ArrayList tags, ArrayList indexList){
		if(tokenRaw ==null || tokenRaw.size()==0)
			return;
		//because we add tokens close to e1, we need to reverse it later, so we have these temp arrayList.
		ArrayList resultTokens = new ArrayList();
		ArrayList resultTags = new ArrayList();
		ArrayList tempIndex = new ArrayList();
		int i=tokenRaw.size()-1;
		for(;i>=0 && i>=tokenRaw.size()-this.windowSize-1;i--){
				String tempWord = ((String)tokenRaw.get(i)).toLowerCase();
					if(tempWord.length()==1 && this.punc.indexOf(tempWord)>=0)//if there is a punc, break.
						break;
					if(!this.stopWords.contains(tempWord)){
						resultTokens.add(tempWord);
						resultTags.add((String)tagRaw.get(i));
						tempIndex.add((Integer)indexRaw.get(i));
					}
					if(resultTokens.size()>=this.windowSize)
						break;
				
		}
		
		i=resultTokens.size()-1;
		for(;i>=0;i--){
			tokens.add((String)resultTokens.get(i));
			tags.add((String)resultTags.get(i));
			indexList.add((Integer)tempIndex.get(i));
		}
	}
	
	/**
	 * tokenize the context, filter out numbers and some stopwords.
	 * @param aContext
	 * @param tokens
	 * @param tags
	 */
	public void tokenCtxt(ArrayList tokenRaw, ArrayList tagRaw,ArrayList indexRaw, ArrayList tokens, ArrayList tags, ArrayList indexList){
		if(tokenRaw==null || tokenRaw.size()==0)
			return;
		int i=0;
		for(;i<tokenRaw.size();i++){
			String tempWord = ((String)tokenRaw.get(i)).toLowerCase();
			if(!this.stopWords.contains(tempWord)){
				tokens.add(tempWord);
				tags.add((String)tagRaw.get(i));
				indexList.add((Integer)indexRaw.get(i));
			}
		}
	}
	
	public void tokenAfter(ArrayList tokenRaw, ArrayList tagRaw,ArrayList indexRaw, ArrayList tokens, ArrayList tags, ArrayList indexList){
		if(tokenRaw==null || tokenRaw.size()==0)
			return;
		int i=0;
		for(;i<tokenRaw.size() && i<this.windowSize+1;i++){
			String tempWord = ((String)tokenRaw.get(i)).toLowerCase();
			if(tempWord.length()==1 && this.punc.indexOf(tempWord)>=0)//if there is a punc, break.
				break;
			if(!this.stopWords.contains(tempWord)){
				tokens.add(tempWord);
				tags.add((String)tagRaw.get(i));
				indexList.add((Integer)indexRaw.get(i));
			}
			if(tokens.size()>=this.windowSize)
				break;

		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}


