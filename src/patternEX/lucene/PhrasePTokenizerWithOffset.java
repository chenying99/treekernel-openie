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
 * The offset doesn't include words in small stopwords. 
 * it includes ignored words when normalized.
 * If there is before pattern and after pattern that has the same string, use the before offset.
 * One bug here is that even there is type is -1, there is still offset, just ignore this kind of offset.
 * @author ying
 *
 */
public class PhrasePTokenizerWithOffset extends PhrasePTokenizer{
	PatternTypeAttribute ptTypeAtt = null;
	PatternOffsetAttribute offsetAtt = null;
	ArrayList finalType = null;
	ArrayList finalOffset = null;
	public PhrasePTokenizerWithOffset(Reader input, HashSet astopWordsList,
			int n, int btSize) throws MalformedPatternException {
		super(input, astopWordsList, n, btSize);
		this.ptTypeAtt = (PatternTypeAttribute)addAttribute(PatternTypeAttribute.class);
		this.offsetAtt = (PatternOffsetAttribute)addAttribute(PatternOffsetAttribute.class);
	}
	
	/**
	 * tokenize the context, lower case all the words
	 * Unigram will be extracted, which has POS tag: VB, JJ, NN, or their extentions.
	 * 
	 * @param aContext
	 * @return
	 */
	void tokenize(String aContext){
		//get the before, between, after and type1, type2 from the raw string
		int indexTempEO = aContext.indexOf("<OFFSET>");
		String pairOffsetS = aContext.substring(indexTempEO+"<OFFSET>".length());
		int[] pairOffsets = parseOffsetPair(pairOffsetS);
		aContext = aContext.substring(0,indexTempEO);
		ArrayList result = new ArrayList();
		HashSet grams = new HashSet();
		HashMap gramType = new HashMap();
		HashMap gramOffset = new HashMap();
		PatternMatcherInput input = new PatternMatcherInput(aContext);
		PatternMatcher matcher = new Perl5Matcher();
		while(matcher.contains(input, typeExp)){
			MatchResult matchString=matcher.getMatch();
            this.type1=matchString.group(1);
            this.type2 = matchString.group(2);
		}
		if(this.type1==null || this.type2==null)
			return;
		String[] segments = new String[3];
		for(int i=0;i<segments.length;i++)
			segments[i] = "";
		try{
			ArrayList tokenBf = new ArrayList();
			ArrayList tokenContext = new ArrayList();
			ArrayList tokenAf = new ArrayList();
			ArrayList tagBf = new ArrayList();
			ArrayList tagContext = new ArrayList();
			ArrayList tagAf = new ArrayList();
			ArrayList offsetBf = new ArrayList();
			ArrayList offsetContext = new ArrayList();
			ArrayList offsetAf = new ArrayList();
			getContext( aContext,  segments, pairOffsets, tokenBf,  tagBf, offsetBf, tokenContext,
					 tagContext, offsetContext, tokenAf,  tagAf, offsetAf);
			//extract patterns which has words in before and/or between
			this.exPtSentBefore(grams, gramType, gramOffset, tokenBf, tokenContext,tagBf, tagContext,offsetBf, offsetContext,this.type1, this.type2) ;
			//extract patterns which has words in between and after
			if(segments[2].length()>0){
				this.exPtSentAfter(grams, gramType, gramOffset,tokenContext,tokenAf, tagContext,tagAf, offsetContext,offsetAf,this.type1, this.type2);
			}
			result.addAll(grams);
		}catch(Exception e){
			System.out.println("content:"+aContext);
			System.out.println("before:"+segments[0]);
			System.out.println("between:"+segments[1]);
			System.out.println("after:"+segments[2]);
			e.printStackTrace();
		}
		
		addToFinalToken(result, gramType, gramOffset);
	}
	
	void addToFinalToken(ArrayList result, HashMap gramType, HashMap gramOffset){
		this.finalToken = new ArrayList();
		this.finalType = new ArrayList();
		//System.out.println("size: "+tokenString.size());
		for(int i=0;i<result.size();i++){
			String pattern = (String)result.get(i);
			String type = (String)gramType.get(pattern);
			String offset = (String)gramOffset.get(pattern);
			Token temp = new Token(pattern+"\t"+type+"\t"+offset,
					i, i+1, "context"); //the field name here doesn't matter, it will follow the one in Document.add
			this.finalToken.add(temp);
			
			//System.out.println(temp.term()+"\ttype: "+type);
			if(output!=null)
				output.println((String)result.get(i));
		}
	}
	
	
	
	@Override
	public boolean incrementToken() throws IOException {
		//System.out.println("incrementToken");
		clearAttributes();
		
		if (this.finalToken==null || this.tokenIndex>=this.finalToken.size()) {
			return false;
		}
		Token temp = (Token)this.finalToken.get(this.tokenIndex);
		String all= temp.term();
		String[] chunks = all.split("\t");
		String term = chunks[0];
		String type = chunks[1];
		String offset = chunks[2];
		this.termAtt.setTermBuffer(term);
		this.ptTypeAtt.setType(type);
		this.offsetAtt.setOffset(offset);
		//System.out.println("increment:"+temp.term()+":"+temp.startOffset()+":"+temp.endOffset());
		this.tokenIndex++;
		return true;
	}
	
	public void getContext(String aContext, String[] segments, int[] pairOffset, ArrayList tokenBf, ArrayList tagBf, ArrayList offsetBf, ArrayList tokenContext,
			ArrayList tagContext, ArrayList offsetContext, ArrayList tokenAf, ArrayList tagAf, ArrayList offsetAf){
		int index1 = aContext.indexOf("<"+type1+">");
		int index2 = aContext.lastIndexOf("<"+type2+">");
		
		if(index1>0)
			segments[0] = aContext.substring(0,index1);
		segments[1] = aContext.substring(index1+("<"+type1+">").length(),index2);
		
		if(index2+("<"+type2+">").length()<aContext.length())
			segments[2] = aContext.substring(index2+("<"+type2+">").length());
		
		if(this.output!=null){
			output.println("type1:"+type1+"\ttype2:"+type2);
			output.println("before:\t"+segments[0]);
			output.println("between:"+segments[1]);
			output.println("after:\t"+segments[2]);
			
		}
		//System.out.println("before:"+before);
		//System.out.println("after:\t"+after);
		//System.out.println("between:"+between);
		
		//the start of bt context is endE1 + 1
		this.tokenCtxt(segments[1], tokenContext, tagContext, offsetContext, pairOffset[1]+1);
		if(tokenContext.size()>=this.btSize){//ignore side context if the between context is long enough
			segments[0]="";
			segments[2] = "";
		}
		//the end of bf is start1 -1
		this.tokenBefore(segments[0], tokenBf, tagBf, offsetBf, pairOffset[0]-1);
		
		if(segments[2].length()>0){ // the start of af is endE2+1
			this.tokenAfter(segments[2], tokenAf, tagAf, offsetAf, pairOffset[3]+1);
		}
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
							String tempToken = (String)tokenContext.get(j);
							if(UtilString.isPubc(tempPOS,tempToken) ){
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
							
						System.out.println("typeBf:"+typeBf+"/"+tagTempBfS+"/"+patternBfS);
						int typeBt = -1;
						if(tagTempBtS.length()>0 && !illegalSet.contains(tagTempBtS))
							typeBt =  ContextPhMatch.btPhMatch(tagTempBtS);
						System.out.println("typeBt:"+typeBt+"/"+tagTempBtS+"/"+patternBtS);
						if(typeBt==-1){
							startBt = -1;
							endBt = -1;		
						}
							
						if(typeBf<0 && tagTempBfS.length()>0)
							illegalSet.add(tagTempBfS);
						if(typeBt<0 && tagTempBtS.length()>0)
							illegalSet.add(tagTempBtS);
						
						if(typeBf>0 || typeBt>0){
							//filter the JJs, put it into the set.
							String[] finalResultBf = null;
							finalResultBf =removeIgnoredPOS(typeBf,patternBfS,tagTempBfS );
							String[] finalResultBt = null;
							finalResultBt =removeIgnoredPOS(typeBt,patternBtS,tagTempBtS );
							String resultP = new String();
							if(typeBf!=-1 && finalResultBf!=null && finalResultBf[0].length()>0)
								resultP +=finalResultBf[0]+" ";
							resultP+="<"+tempType1+"> ";
							if(typeBt!=-1 && finalResultBt!=null && finalResultBt[0].length()>0){
								resultP +=finalResultBt[0]+" ";
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
							//filter the JJs, put it into the set.
							String[] finalResultAf = null;
							finalResultAf =removeIgnoredPOS(typeAf,patternAfS,tagTempAfS );
							String[] finalResultBt = null;
							finalResultBt =removeIgnoredPOS(typeBt,patternBtS,tagTempBtS );
							String resultP = new String();
							resultP+="<"+tempType1+"> ";
							if(typeBt!=-1 && finalResultBt!=null && finalResultBt[0].length()>0)
								resultP +=finalResultBt[0]+" ";
							resultP +="<"+tempType2+">";
							if(typeAf!=-1 && finalResultAf!=null && finalResultAf[0].length()>0){
								resultP +=" "+finalResultAf[0];
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
	
	/**
	 * The difference with super's tokenBefore is that offset information is added.
	 * @param s1
	 * @param tokens
	 * @param tags
	 * @param offsets
	 */
	public void tokenBefore(String s1, ArrayList tokens, ArrayList tags, ArrayList offsets, int endOffset){
		if(s1.length()==0)
			return;
		ArrayList resultTokens = new ArrayList();
		ArrayList resultTags = new ArrayList();
		ArrayList resultOffsets = new ArrayList();
		String[] chunks = s1.split(" ");
		int i=chunks.length-1;
		int currentIndex = endOffset;
		for(;i>=0 && i>=chunks.length-5;i--, endOffset--){
			if(chunks[i].length()>1){
				String[] chunks2 = chunks[i].split("/");//word/tag/lemma
				if(chunks2.length==3){
					String tempWord = chunks2[2].trim().toLowerCase();
					if(tempWord.length()==1 && this.punc.indexOf(tempWord)>=0)//if there is a punc, break.
						break;
					//if(!this.stopWords.contains(tempWord) && this.openTag.get(chunks2[1])!=null && !Util.isNumber(tempWord)){
					//because we will use phrase filter, so we don't need to filter it here now.
					if(!this.stopWords.contains(tempWord)){
						resultTokens.add(tempWord);
						resultTags.add(chunks2[1]);
						resultOffsets.add(endOffset);
					}
					if(resultTokens.size()>=this.windowSize)
						break;
				}
			}
		}
		
		i=resultTokens.size()-1;
		for(;i>=0;i--){
			tokens.add((String)resultTokens.get(i));
			tags.add((String)resultTags.get(i));
			offsets.add((Integer)resultOffsets.get(i));
		}
	}
	
	/**
	 * tokenize the context, filter out numbers and some stopwords.
	 * @param aContext
	 * @param tokens
	 * @param tags
	 */
	public void tokenCtxt(String aContext, ArrayList tokens, ArrayList tags, ArrayList offsets, int startOffSet){
		String[] chunks = aContext.split(" ");
		int i=0;
		for(;i<chunks.length;i++){
			if(chunks[i].length()>1){
				String[] chunks2 = chunks[i].split("/");//word/tag/lemma
				if(chunks2.length==3){
					String tempWord = chunks2[2].trim().toLowerCase();
					if(!this.stopWords.contains(tempWord)){
						tokens.add(tempWord);
						tags.add(chunks2[1]);
						offsets.add(i+startOffSet);
					}
				}
			}
		}
	}
	
	public void tokenAfter(String s2,  ArrayList tokens, ArrayList tags, ArrayList offsets, int startOffset){
		if(s2.length()==0)
			return;
		String[] chunks = s2.split(" ");
		int i=0;
		for(;i<chunks.length && i<5;i++){
			if(chunks[i].length()>1){
				String[] chunks2 = chunks[i].split("/");//word/tag/lemma
				if(chunks2.length==3){
					String tempWord = chunks2[2].trim().toLowerCase();
					if(tempWord.length()==1 && this.punc.indexOf(tempWord)>=0)//if there is a punc, break.
						break;
					if(!this.stopWords.contains(tempWord)){
						tokens.add(tempWord);
						tags.add(chunks2[1]);
						offsets.add(i+startOffset);
					}
					if(tokens.size()>=this.windowSize)
						break;
					
				}
			}
		}
	}
	
	/**
	 * Because it may has -1, so it is in the form of a,b;c,d;e,f
	 * @param offset
	 * @return
	 */
	public static int[] parseOffset(String offset){
		int [] offsets = new int[6];
		String[] chunks = offset.split(";");
		for(int i=0;i<3;i++){
			String[] chunks2 = chunks[i].split(",");
			offsets[2*i] = Integer.parseInt(chunks2[0]);
			offsets[2*i+1]=Integer.parseInt(chunks2[1]);
		}
		return offsets;
	}
	
	public static int[] parseOffsetPair(String offset){
		int[] offsets = new int[4];
		String[] chunks = offset.split(";");
		for(int i=0;i<chunks.length;i++){
			String[] chunks2 = chunks[i].split("-");
			offsets[2*i] = Integer.parseInt(chunks2[0]);
			offsets[2*i+1]=Integer.parseInt(chunks2[1]);
		}
		return offsets;
	}
	
	/**
	 * Get the more previous start offset
	 * @param pre
	 * @param current
	 * @return
	 */
	int getStart(int pre, int current){
		int start=current;
		if(pre!=start){
			if(start==-1){
				if(pre!=-1)
					start = pre;
			}else{
				if(pre!=-1 && pre<start)
					start = pre;
			}
		}
		return start;
	}
	
	/**
	 * Get the bigger range offset.
	 * @param pre
	 * @param current
	 * @return
	 */
	int getEnd(int pre, int current){
		int end = current;
		if(pre!=end){
			if(end==-1){
				if(pre!=-1)
					end = pre;
			}else{
				if(pre!=-1 && pre>end)
					end = pre;
			}
		}
		return end;
	}
	
	boolean isOverlap(int start1, int start2, int end1, int end2){
		if(start1<=start2 && start2<=end1)
			return true;
		if(start2<=start1 && start1<=end2)
			return true;
		return false;
	}

}
