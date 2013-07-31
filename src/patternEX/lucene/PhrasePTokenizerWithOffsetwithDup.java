package patternEX.lucene;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.analysis.Token;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Matcher;

import reverb.ContextPhMatch;
import utilYing.UtilString;

/**
 * Because we don't need to use frequency in the first paper experiment, 
 * So we don't need to delete the duplicates.
 * @author ying
 *
 */
public class PhrasePTokenizerWithOffsetwithDup extends PhrasePTokenizerWithOffset{
	public PhrasePTokenizerWithOffsetwithDup(Reader input, HashSet astopWordsList,
			int n, int btSize) throws MalformedPatternException {
		super(input, astopWordsList, n, btSize);
	}
	
	/**
	 * Generate all n to 1 continuous grams that matches some phrases form
	 * @param set
	 * @param tokenBf
	 * @param tokenContext
	 * @param tagBf
	 * @param tagContext
	 * @param tempType1
	 * @param tempType2
	 */
	public void exPtSentBefore(HashSet set, HashMap typeMap, HashMap gramOffset, ArrayList tokenBf, ArrayList tokenContext, 
			ArrayList tagBf, ArrayList tagContext, ArrayList offsetBf, ArrayList offsetContext, String tempType1, String tempType2){
		HashSet illegalSet = new HashSet();//record illegal POS pattern, so we don't need to check again.
		int sizeAll = tokenBf.size()+tokenContext.size();
		for(int tempGram=this.nGram;tempGram>0;tempGram--){
			if(sizeAll>=tempGram){
				for(int i=0;i<sizeAll-tempGram+1;i++){
					//get the candidate patterns n-gram.
					StringBuffer patternBf = new StringBuffer();
					StringBuffer tagTempBf = new StringBuffer();
					int startBf = -1;
					int endBf = -1;
					StringBuffer patternBt = new StringBuffer();
					StringBuffer tagTempBt = new StringBuffer();
					int startBt = -1;
					int endBt = -1;
					int length = 0;
					for(int j=i;j<sizeAll && j<i+tempGram;j++){
						if(j<tokenBf.size()){
							String tempPOS = (String)tagBf.get(j);
							String tempToken = (String)tokenBf.get(j);
							if(UtilString.isPubc(tempPOS,tempToken) ){
								//System.out.println("tempPOS "+tempPOS+" is punc");
								break;
							}
							patternBf.append(tokenBf.get(j)).append(" ");
							tagTempBf.append(tagBf.get(j)).append(" ");
							int indexBf = (Integer)offsetBf.get(j);
							if(startBf==-1)
								startBf = indexBf;
							endBf = indexBf;
							length++;
						}else{
							int index2 = j-tokenBf.size();
							String tempPOS = (String)tagContext.get(index2);
							String tempToken = (String)tokenContext.get(index2);
							if(UtilString.isPubc(tempPOS,tempToken) ){
								//System.out.println("tempPOS "+tempPOS+" is punc");
								break;
							}
							patternBt.append(tokenContext.get(index2)).append(" ");
							tagTempBt.append(tagContext.get(index2)).append(" ");
							int indexBt = (Integer)offsetContext.get(index2);
							if(startBt==-1)
								startBt = indexBt;
							endBt = indexBt;
							length++;
						}
					}
					if(length==tempGram){//check if it is in the right form
						String patternBfS = patternBf.toString().trim();
						String tagTempBfS = tagTempBf.toString().trim();
						String patternBtS = patternBt.toString().trim();
						String tagTempBtS = tagTempBt.toString().trim();
						//System.out.println("bf:"+patternBfS+"\t"+tagTempBfS);
						//System.out.println("bt:"+patternBtS+"\t"+tagTempBtS);
						int typeBf = -1;
						if(tagTempBfS.length()>0&& !illegalSet.contains(tagTempBfS))
							typeBf = ContextPhMatch.sdPhMatch(tagTempBfS);
						if(typeBf == -1){
							startBf = -1;
							endBf = -1;
						}
							
						//System.out.println("typeBf:"+typeBf+"/"+tagTempBfS+"/"+patternBfS);
						int typeBt = -1;
						if(tagTempBtS.length()>0 && !illegalSet.contains(tagTempBtS))
							typeBt =  ContextPhMatch.btPhMatch(tagTempBtS);
						//System.out.println("typeBt:"+typeBt+"/"+tagTempBtS+"/"+patternBtS);
						if(typeBt==-1){
							startBt = -1;
							endBt = -1;		
						}
							
						if(typeBf<0 && tagTempBfS.length()>0)
							illegalSet.add(tagTempBfS);
						if(typeBt<0 && tagTempBtS.length()>0)
							illegalSet.add(tagTempBtS);
						
						if(typeBf>0 || typeBt>0){
							String resultP = new String();
							if(typeBf!=-1 && patternBfS!=null && patternBfS.length()>0)
								resultP +=patternBfS+" ";
							resultP+="<"+tempType1+"> ";
							if(typeBt!=-1 && patternBtS!=null && patternBtS.length()>0){
								resultP +=patternBtS+" ";
							}
							resultP +="<"+tempType2+">";
							set.add(resultP);
							typeMap.put(resultP,typeBf+";"+typeBt+";-1");
							Object offsetPreO = gramOffset.get(resultP);
							if(offsetPreO!=null){
								String offsetPreS = (String)offsetPreO;
								int[] offsetPre = parseOffset(offsetPreS);
								//System.out.println("pre:"+resultP+"\t"+offsetPreS);
								//System.out.println("current:"+resultP+"\t"+startBf+","+endBf+";"+startBt+","+endBt+";-1,-1");
								//if it has overlap with the first one, then check the larger offset range
								if(this.isOverlap(offsetPre[0],startBf, offsetPre[1], endBf) && 
										this.isOverlap(offsetPre[2],startBt, offsetPre[3], endBt)){
									startBf = getStart(offsetPre[0],startBf);
									endBf = getEnd(offsetPre[1], endBf);
									startBt = getStart(offsetPre[2],startBt);
									endBt = getEnd(offsetPre[3], endBt);
									
								}
								//System.out.println("new:"+resultP+"\t"+startBf+","+endBf+";"+startBt+","+endBt+";-1,-1");
								
							}
							String offsetCurrent = startBf+","+endBf+";"+startBt+","+endBt+";-1,-1";
							gramOffset.put(resultP, offsetCurrent);
						}
					}

				}
			}
		}
	}
	
	public void exPtSentAfter(HashSet set, HashMap typeMap,HashMap gramOffset, ArrayList tokenContext, ArrayList tokenAf, ArrayList tagContext, 
			ArrayList tagAf, ArrayList offsetContext, ArrayList offsetAf, String tempType1, String tempType2){
		HashSet illegalSet = new HashSet();//record illegal POS pattern, so we don't need to check again.
		int sizeAll = tokenAf.size()+tokenContext.size();
		for(int tempGram=this.nGram;tempGram>0;tempGram--){
			if(sizeAll>=tempGram){
				for(int i=0;i<sizeAll-tempGram+1;i++){
					//get the candidate patterns.
					StringBuffer patternAf = new StringBuffer();
					StringBuffer tagTempAf = new StringBuffer();
					StringBuffer patternBt = new StringBuffer();
					StringBuffer tagTempBt = new StringBuffer();
					int startBt = -1;
					int endBt = -1;
					int startAf = -1;
					int endAf = -1;
					int length = 0;
					for(int j=i;j<sizeAll && j<i+tempGram;j++){
						if(j<tokenContext.size()){
							String tempPOS = (String)tagContext.get(j);
							String tempToken = (String)tokenContext.get(j);
						
							if(UtilString.isPubc(tempPOS,tempToken) ){
								break;
							}
							patternBt.append(tokenContext.get(j)).append(" ");
							tagTempBt.append(tagContext.get(j)).append(" ");
							int indexTemp = (Integer) offsetContext.get(j);
							if(startBt==-1)
								startBt = indexTemp;
							endBt = indexTemp;
							length++;
						}else{
							int index2 = j-tokenContext.size();
							String tempPOS = (String)tagAf.get(index2);
							String tempToken = (String)tokenAf.get(index2);
							if(UtilString.isPubc(tempPOS,tempToken) ){
								break;
							}
							patternAf.append(tokenAf.get(index2)).append(" ");
							tagTempAf.append(tagAf.get(index2)).append(" ");
							int indexTemp = (Integer) offsetAf.get(index2);
							if(startAf==-1)
								startAf = indexTemp;
							endAf = indexTemp;
							length++;
						}
					}
					if(length==tempGram){//check if it is in the right form
						String patternAfS = patternAf.toString().trim();
						String tagTempAfS = tagTempAf.toString().trim();
						String patternBtS = patternBt.toString().trim();
						String tagTempBtS = tagTempBt.toString().trim();
						//System.out.println("bt:"+patternBtS+"\t"+tagTempBtS);
						//System.out.println("Af:"+patternAfS+"\t"+tagTempAfS);
						int typeBt = -1;
						if(tagTempBtS.length()>0 && !illegalSet.contains(tagTempBtS))
							typeBt =  ContextPhMatch.btPhMatch(tagTempBtS);
						if(typeBt==-1){
							startBt = -1;
							endBt = -1;
						}
							
						//System.out.println("typeBt:"+typeBt+"/"+tagTempBtS+"/"+patternBtS);
						int typeAf = -1;
						//Because there might be <A> is <B>'s mother
						if(tagTempAfS.length()>0&& !illegalSet.contains(tagTempAfS))
							typeAf = ContextPhMatch.btPhMatch(tagTempAfS);
						//System.out.println("typeAf:"+typeAf+"/"+tagTempAfS+"/"+patternAfS);
						if(typeAf ==-1){
							startAf = -1;
							endAf = -1;
						}
						if(typeAf<0 && tagTempAfS.length()>0)
							illegalSet.add(tagTempAfS);
						if(typeBt<0 && tagTempBtS.length()>0)
							illegalSet.add(tagTempBtS);
						
						if(typeAf>0 || typeBt>0){
							String resultP = new String();
							resultP+="<"+tempType1+"> ";
							if(typeBt!=-1 && patternBtS!=null && patternBtS.length()>0)
								resultP +=patternBtS+" ";
							resultP +="<"+tempType2+">";
							if(typeAf!=-1 && patternAfS!=null && patternAfS.length()>0){
								resultP +=" "+patternAfS;
							}
							
							set.add(resultP);
							typeMap.put(resultP,"-1;"+typeBt+";"+typeAf);
							Object offsetPreO = gramOffset.get(resultP);
							boolean hasBefore = false;
							if(offsetPreO!=null){
								String offsetPreS = (String)offsetPreO;
								int[] offsetPre = parseOffset(offsetPreS);
								if(offsetPre[0]!=-1)
									hasBefore=true;
								else{
									if(this.isOverlap(offsetPre[2],startBt, offsetPre[3], endBt) && 
											this.isOverlap(offsetPre[4],startAf, offsetPre[5], endAf)){
										startBt = getStart(offsetPre[2],startBt);
										endBt = getEnd(offsetPre[3], endBt);
										startAf = getStart(offsetPre[4],startAf);
										endAf = getEnd(offsetPre[5], endAf);
									}
								}
							}
							if(offsetPreO==null || !hasBefore){
								String offsetCurrent = "-1,-1;"+startBt+","+endBt+";"+startAf+","+endAf;
								gramOffset.put(resultP, offsetCurrent);
							}
						}
					}
				}
			}
		}
	}

}
