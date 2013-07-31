package patternEX.lucene;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import patternEX.CombinationGenerator;
import patternEX.Util;

/**
 * Input is in the form of:
 * beforeTemp+"\n"+contextTemp+"\n"+afterTemp;
 * every part is in the form of
 * token1/tag1/lemma1 token2/tag2/lemma2
 * 
 * I want to extract tokens in the form of c1 <e1> c2 <e2> etc.
 * 
 * Method: see GramExtraction.java
 * I add one filter comparing with GramExtraction. 
 * words that are JJ, VB, NN type, but with some special type letters are filtered.
 * But the position counts.
 * @author ying
 *
 */
public class PhrasePatternTokenizer extends Tokenizer{
	public static final String typeExpS = ".*<(PER|ORG|LOC|NONE|DATE)>.*<(PER|ORG|LOC|NONE|DATE)>.*";
	public static PatternCompiler compiler = new Perl5Compiler();
	public static Pattern typeExp = null;
	String context;
	int tokenIndex = 0;
	ArrayList finalToken  = null; //elements are Tokens
	HashSet stopWords = null;
	HashMap openTag = null;
	int nGram = 0; // the order of nGram.
	public static final String punc = ".!?;";
	//window size of side context, don't need to constraint window size in between, as the data only has window size less than 5 in between
	int windowSize = 3;
	String type1 = null;
	String type2 = null;
	private TermAttribute termAtt;
	
	public PhrasePatternTokenizer(Reader input, HashSet astopWordsList, HashMap aOpenTag, String type1, String type2, int n) throws MalformedPatternException{
		super(input);
		if(typeExp==null)
			typeExp = compiler.compile(typeExpS);
		this.context=null;
		this.stopWords = astopWordsList;
		this.openTag = aOpenTag;
		this.type1 = type1;
		this.type2 = type2;
		this.nGram = n;
		StringBuffer contextBuffer = new StringBuffer();
		try{
			int c = this.input.read();
			while(c!=-1){
				contextBuffer.append((char)c);
				c = this.input.read();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		context = contextBuffer.toString();
		//System.out.println(context);
		//get the tokens for every sentence
		if(contextBuffer.length()>3){
			this.finalToken = new ArrayList();
			//tokenize the context
			ArrayList tokenString = this.tokenize(context);
			//System.out.println("size: "+tokenString.size());
			for(int i=0;i<tokenString.size();i++){
				Token temp = new Token((String)tokenString.get(i),
						i, i+1, "context"); //the field name here doesn't matter, it will follow the one in Document.add
				this.finalToken.add(temp);
			}
		}
		this.termAtt = (TermAttribute) addAttribute(TermAttribute.class);
	    this.tokenIndex = 0;
	}
	
	
	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		if (this.tokenIndex>=this.finalToken.size()) {
			return false;
		}
		Token temp = (Token)this.finalToken.get(this.tokenIndex);
		this.termAtt.setTermBuffer(temp.term());
		//System.out.println("increment:"+temp.term()+":"+temp.startOffset()+":"+temp.endOffset());
		this.tokenIndex++;
		return true;
	}
	
	/**
	 * tokenize the context, lower case all the words
	 * Unigram will be extracted, which has POS tag: VB, JJ, NN, or their extentions.
	 * 
	 * @param aContext
	 * @return
	 */
	ArrayList tokenize(String aContext){
		ArrayList result = new ArrayList();
		HashSet grams = new HashSet();
		PatternMatcherInput input = new PatternMatcherInput(aContext);
		PatternMatcher matcher = new Perl5Matcher();
		while(matcher.contains(input, typeExp)){
			MatchResult matchString=matcher.getMatch();
            this.type1=matchString.group(1);
            this.type2 = matchString.group(2);
		}
		int index1 = aContext.indexOf("<"+type1+">");
		int index2 = aContext.lastIndexOf("<"+type2+">");
		String before = "";
		String between = "";
		String after = "";
		try{
			
			if(index1>0)
				before = aContext.substring(0,index1);
			between = aContext.substring(index1+("<"+type1+">").length(),index2);
			
			if(index2+("<"+type2+">").length()<aContext.length())
				after = aContext.substring(index2+("<"+type2+">").length());
			//System.out.println(aContext);
			//System.out.println(type1);
			//System.out.println(type2);
			//System.out.println("before:"+before);
			//System.out.println("between:"+between);
			//System.out.println("after:"+after);
			this.exPtSentBefore(grams, before, between, this.type1, this.type2) ;
			if(after.length()>0)
				this.exPtSentAfter(grams, between, after, this.type1, this.type2);
			result.addAll(grams);
		}catch(Exception e){
			System.out.println("content:"+aContext);
			System.out.println("before:"+before);
			System.out.println("between:"+between);
			System.out.println("after:"+after);
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Extract patterns with words before and between entity pairs.
	 * Put them into set.
	 * @param set: result set
	 * @param s1: contextBefore
	 * @param s2: contextBetween
	 * @param tempType1
	 * @param tempType2
	 */
	public void exPtSentBefore(HashSet set, String s1, String s2, String tempType1, String tempType2){
		ArrayList newResult = this.tokenBefore(s1);
		ArrayList tokenContext = this.tokenCtxt(s2);
		//System.out.println("\ts2 size:"+tokenContext.size());
		int sizeAll = newResult.size()+tokenContext.size();
		if(sizeAll>=this.nGram){
			CombinationGenerator generator = new CombinationGenerator(sizeAll, this.nGram);
			//System.out.println(generator.getNumLeft()+"\thas more:"+generator.hasMore());
			while(generator.hasMore()){
				int[] indexArray = generator.getNext();
				StringBuffer pattern = new StringBuffer();
				boolean entry1Add = false;
				for(int i=0;i<indexArray.length;i++){
					if(indexArray[i]<newResult.size()){
						pattern.append(newResult.get(indexArray[i])).append(" ");
					}else{
						if(!entry1Add){
							entry1Add = true;
							pattern.append("<"+tempType1+">").append(" ");
						}
						int index2 = indexArray[i]-newResult.size();
						pattern.append(tokenContext.get(index2)).append(" ");
					}
				}
				if(!entry1Add)
					pattern.append("<"+tempType1+">").append(" ");
				pattern.append("<"+tempType2+">");
				set.add(pattern.toString().trim());
			}
		}
		
	}
	
	public ArrayList tokenBefore(String s1){
		ArrayList result = new ArrayList();//side context
		String[] chunks = s1.split(" ");
		int i=chunks.length-1;
		for(;i>=0 && i>=chunks.length-5;i--){
			if(chunks[i].length()>1){
				String[] chunks2 = chunks[i].split("/");//word/tag/lemma
				if(chunks2.length==3){
					String tempWord = chunks2[2].trim().toLowerCase();
					if(tempWord.length()==1 && this.punc.indexOf(tempWord)>=0)//if there is a punc, break.
						break;
					if(!this.stopWords.contains(tempWord) && this.openTag.get(chunks2[1])!=null && !Util.isNumber(tempWord)){
						if(!this.filter(tempWord, chunks2[1])){
							result.add(tempWord);
						}
					}
					//if(tempWord.equals(",")||tempWord.equals("(")||tempWord.equals("(")){
					//	result.add(tempWord);
					//}
					if(result.size()>=this.windowSize)
						break;
				}
			}
		}
		
		ArrayList newResult = new ArrayList();
		i=result.size()-1;
		for(;i>=0;i--){
			newResult.add((String)result.get(i));
		}
		return newResult;
	}
	
	public ArrayList tokenCtxt(String aContext){
		ArrayList result = new ArrayList();
		String[] chunks = aContext.split(" ");
		int i=0;
		for(;i<chunks.length;i++){
			if(chunks[i].length()>1){
				String[] chunks2 = chunks[i].split("/");//word/tag/lemma
				if(chunks2.length==3){
					String tempWord = chunks2[2].trim().toLowerCase();
					if(!this.stopWords.contains(tempWord) && this.openTag.get(chunks2[1])!=null && !Util.isNumber(tempWord)){
						if(!this.filter(tempWord, chunks2[1])){
							result.add(tempWord);
							
						}
					}
					//if(tempWord.equals(",")||tempWord.equals("(")||tempWord.equals("("))
					//	result.add(tempWord);
				}
			}
		}
		return result;
	}
	
	public ArrayList tokenAfter(String s2){
		ArrayList result = new ArrayList();//side context
		String[] chunks = s2.split(" ");
		int i=0;
		for(;i<chunks.length && i<5;i++){
			if(chunks[i].length()>1){
				String[] chunks2 = chunks[i].split("/");//word/tag/lemma
				if(chunks2.length==3){
					String tempWord = chunks2[2].trim().toLowerCase();
					
					if(tempWord.length()==1 && this.punc.indexOf(tempWord)>=0)//if there is a punc, break.
						break;
					if(!this.stopWords.contains(tempWord) && this.openTag.get(chunks2[1])!=null && !Util.isNumber(tempWord)){
						if(!this.filter(tempWord, chunks2[1])){
							result.add(tempWord);
						}
					}
					//if(tempWord.equals(",")||tempWord.equals("(")||tempWord.equals("(")){
						//result.add(tempWord);
					//}
					if(result.size()>=this.windowSize)
						break;
					
				}
			}
		}
		return result;
	}
	
	/**
	 * Extract patterns in after or after and between.
	 * Ingore those only in between, as they are already extracted in exPtSentBefore()
	 * @param set
	 * @param s1: contextBetween
	 * @param s2: contextAfter
	 */
	public void exPtSentAfter(HashSet set, String s1, String s2, String tempType1, String tempType2){
		
		ArrayList tokenContext = this.tokenCtxt(s1);
		ArrayList result =this.tokenAfter(s2);
		int sizeAll = result.size()+tokenContext.size();
		if(sizeAll>=this.nGram){
			CombinationGenerator generator = new CombinationGenerator(sizeAll, this.nGram);
			while(generator.hasMore()){
				int[] indexArray = generator.getNext();
				//if the last word of pattern is also in context, ignore
				if(indexArray[indexArray.length-1]<tokenContext.size())
					continue;
				StringBuffer pattern = new StringBuffer();
				pattern.append("<"+tempType1+">");
				boolean entry2Add = false;
				for(int i=0;i<indexArray.length;i++){
					if(indexArray[i]<tokenContext.size()){
						pattern.append(" ").append(tokenContext.get(indexArray[i]));
					}else{
						if(!entry2Add){
							entry2Add = true;
							pattern.append(" ").append("<"+tempType2+">");
						}
						int index2 = indexArray[i]-tokenContext.size();
						pattern.append(" ").append(result.get(index2));
					}
				}
				if(!entry2Add){
					pattern.append(" ").append("<"+tempType2+">");
				}
				//System.out.println(pattern.toString());
				set.add(pattern.toString());
			}
		}
	}

	/**
	 * Check if we should filter out the word.
	 * If the tag is open word type, and if the word is not only alphabet, then filter, return true.
	 * Else return false.
	 * @param word
	 * @param tag
	 * @return
	 */
	public boolean filter(String word, String tag){
		if(tag.indexOf("JJ")>=0 || tag.indexOf("NN")>=0 || tag.indexOf("VB")>=0){
			if(!Util.isAlphabet(word))
				return true;
		}
		return false;
	}
	
	public void add(HashMap patternFreq, String pattern){
		Object freq = patternFreq.get(pattern);
		if(freq==null)
			patternFreq.put(pattern, new Integer(1));
		else{
			int freqTemp = ((Integer)freq).intValue();
			freqTemp++;
			patternFreq.put(pattern, new Integer(freqTemp));
		}
		
	}

}
