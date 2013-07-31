package patternEX.lucene;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;
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
import reverb.ContextPhMatch;
import reverb.RelationNormalizer;


/**
 * extract only VP or NP as ngrams. Extract all n, n-1, n-2...1 grams.
 * Ignore the DT and JJ when constructing phrases.
 * It is similar to <code>GramPhraseExtraction</code> 
 * But it has more rules.
 * <li> 1. if between has verb, noun, and is longer than 3, ignore the before and after context. </li>
 * <li> 2. only use continuous grams.</li>
 * <li> 3. one condition has to be obeyed. between has verb phrase, noun phrase, pos noun phrase, noun pp; before has noun, noun pp, verb; after has noun phrase, verb phrase
 * @author ying
 *
 */
public class PhrasePTokenizer extends Tokenizer{
	public static PatternCompiler compiler = new Perl5Compiler();
	public static Pattern typeExp = null;
	public static RelationNormalizer normalizer = new RelationNormalizer();
	HashSet stopWords = null;
	//HashMap openTag = null;
	int nGram = 0; // the order of nGram.
	String context;
	ArrayList finalToken  = null;
	int windowSize = 4;
	int btSize = 5; //when between size is larger than 5, ignore the before and after
	String type1 = null;
	String type2 = null;
	int tokenIndex = 0;
	public static final String punc = ".!?;";
	
	public static PrintStream output = null;
	TermAttribute termAtt=null;
	
	public PhrasePTokenizer(Reader input, HashSet astopWordsList,int n, int btSize) throws MalformedPatternException{
		super(input);
		this.btSize = btSize;
		if(typeExp==null)
			typeExp = compiler.compile(PhrasePatternTokenizer.typeExpS);
		this.context=null;
		this.stopWords = astopWordsList;
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
			
			this.tokenize(context);
		}
		this.termAtt = (TermAttribute) addAttribute(TermAttribute.class);
		
	    this.tokenIndex = 0;
	}
	
	public static void setPrint(PrintStream out){
		output = out;
	}
	
	public static void closeOut(){
		output.close();
	}
	
	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		
		if (this.finalToken==null || this.tokenIndex>=this.finalToken.size()) {
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
	void tokenize(String aContext){
		System.out.println("super tokenize");
		//get the before, between, after and type1, type2 from the raw string
		ArrayList result = new ArrayList();
		HashSet grams = new HashSet();
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
			ArrayList tagBf = new ArrayList();
			ArrayList tagContext = new ArrayList();
			ArrayList tokenAf = new ArrayList();
			ArrayList tagAf = new ArrayList();
			getContext( aContext,  segments,  tokenBf,  tagBf,  tokenContext,
					 tagContext,  tokenAf,  tagAf);
			//extract patterns which has words in before and/or between
			this.exPtSentBefore(grams, tokenBf, tokenContext,tagBf, tagContext, this.type1, this.type2) ;
			//extract patterns which has words in between and after
			if(segments[2].length()>0){
				this.exPtSentAfter(grams, tokenContext,tokenAf, tagContext,tagAf, this.type1, this.type2);
			}
			result.addAll(grams);
		}catch(Exception e){
			System.out.println("content:"+aContext);
			System.out.println("before:"+segments[0]);
			System.out.println("between:"+segments[1]);
			System.out.println("after:"+segments[2]);
			e.printStackTrace();
		}
		
		addToFinalToken(result);
	}
	
	public void getContext(String aContext, String[] segments, ArrayList tokenBf, ArrayList tagBf, ArrayList tokenContext,
			ArrayList tagContext, ArrayList tokenAf, ArrayList tagAf){
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
		
		
		this.tokenCtxt(segments[1], tokenContext, tagContext);
		if(tokenContext.size()>=this.btSize){//ignore side context if the between context is long enough
			segments[0]="";
			segments[2] = "";
		}
		this.tokenBefore(segments[0], tokenBf, tagBf);
		
		if(segments[2].length()>0){
			this.tokenAfter(segments[2], tokenAf, tagAf);
		}
	}

	
	void addToFinalToken(ArrayList result){
		System.out.println("super addToFinalToken.");
		this.finalToken = new ArrayList();
		//System.out.println("size: "+tokenString.size());
		for(int i=0;i<result.size();i++){
			Token temp = new Token((String)result.get(i),
					i, i+1, "context"); //the field name here doesn't matter, it will follow the one in Document.add
			this.finalToken.add(temp);
			if(output!=null)
				output.println((String)result.get(i));
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
	public void exPtSentBefore(HashSet set, ArrayList tokenBf, ArrayList tokenContext, ArrayList tagBf, ArrayList tagContext, String tempType1, String tempType2){
		HashSet illegalSet = new HashSet();//record illegal POS pattern, so we don't need to check again.
		int sizeAll = tokenBf.size()+tokenContext.size();
		for(int tempGram=this.nGram;tempGram>0;tempGram--){
			if(sizeAll>=tempGram){
				for(int i=0;i<sizeAll-tempGram+1;i++){
					//get the candidate patterns n-gram.
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
						}else{
							int index2 = j-tokenBf.size();
							patternBt.append(tokenContext.get(index2)).append(" ");
							tagTempBt.append(tagContext.get(index2)).append(" ");
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
						//System.out.println("typeBf:"+typeBf+"/"+tagTempBfS+"/"+patternBfS);
						int typeBt = -1;
						if(tagTempBtS.length()>0 && !illegalSet.contains(tagTempBtS))
							typeBt =  ContextPhMatch.btPhMatch(tagTempBtS);
						//System.out.println("typeBt:"+typeBt+"/"+tagTempBtS+"/"+patternBtS);
						
						if(typeBf<0 && tagTempBfS.length()>0)
							illegalSet.add(tagTempBfS);
						if(typeBt<0 && tagTempBtS.length()>0)
							illegalSet.add(tagTempBtS);
						
						if(typeBf>0 || typeBt>0){
							//filter the JJs, put it into the set.
							String[] finalResultBf = null;
							finalResultBf = removeIgnoredPOS(typeBf,patternBfS,tagTempBfS );
							String[] finalResultBt = null;
							finalResultBt =removeIgnoredPOS(typeBt,patternBtS,tagTempBtS );
							String resultP = new String();
							if(finalResultBf!=null && finalResultBf[0].length()>0)
								resultP +=finalResultBf[0]+" ";
							resultP+="<"+tempType1+"> ";
							if(finalResultBt!=null && finalResultBt[0].length()>0){
								resultP +=finalResultBt[0]+" ";
							}
							resultP +="<"+tempType2+">";
							set.add(resultP);
						}
					}

				}
			}
		}
	}
	
	public void exPtSentAfter(HashSet set, ArrayList tokenContext, ArrayList tokenAf, ArrayList tagContext, ArrayList tagAf, String tempType1, String tempType2){
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
					int length = 0;
					for(int j=i;j<sizeAll && j<i+tempGram;j++){
						if(j<tokenContext.size()){
							patternBt.append(tokenContext.get(j)).append(" ");
							tagTempBt.append(tagContext.get(j)).append(" ");
							length++;
						}else{
							int index2 = j-tokenContext.size();
							patternAf.append(tokenAf.get(index2)).append(" ");
							tagTempAf.append(tagAf.get(index2)).append(" ");
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
						//System.out.println("typeBt:"+typeBt+"/"+tagTempBtS+"/"+patternBtS);
						int typeAf = -1;
						//Because there might be <A> is <B>'s mother
						if(tagTempAfS.length()>0&& !illegalSet.contains(tagTempAfS))
							typeAf = ContextPhMatch.btPhMatch(tagTempAfS);
						//System.out.println("typeAf:"+typeAf+"/"+tagTempAfS+"/"+patternAfS);
						
						if(typeAf<0 && tagTempAfS.length()>0)
							illegalSet.add(tagTempAfS);
						if(typeBt<0 && tagTempBtS.length()>0)
							illegalSet.add(tagTempBtS);
						
						if(typeAf>0 || typeBt>0){
							//filter the JJs, put it into the set.
							String[] finalResultAf = null;
							finalResultAf =removeIgnoredPOS(typeAf,patternAfS,tagTempAfS );
							String[] finalResultBt = null;
							finalResultBt = removeIgnoredPOS(typeBt,patternBtS,tagTempBtS );
							String resultP = new String();
							resultP+="<"+tempType1+"> ";
							if(finalResultBt!=null && finalResultBt[0].length()>0)
								resultP +=finalResultBt[0]+" ";
							resultP +="<"+tempType2+">";
							if(finalResultAf!=null && finalResultAf[0].length()>0){
								resultP +=" "+finalResultAf[0];
							}
							
							set.add(resultP);
						}
					}
				}
			}
		}
	}
	
	public String[] removeIgnoredPOS(int type, String pattern, String tag){
		String[] finalResult =null;
		if(type==ContextPhMatch.NP)
			finalResult = normalizer.removeIgnoredPosTagsNoun(pattern, tag);
		else if(type ==ContextPhMatch.VP)
			finalResult = normalizer.removeIgnoredPosTagsVerb(pattern, tag);
		return finalResult;
	}
	
	public void tokenBefore(String s1, ArrayList tokens, ArrayList tags){
		if(s1.length()==0)
			return;
		ArrayList resultTokens = new ArrayList();
		ArrayList resultTags = new ArrayList();
		String[] chunks = s1.split(" ");
		int i=chunks.length-1;
		for(;i>=0 && i>=chunks.length-5;i--){
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
		}
	}
	
	/**
	 * tokenize the context, filter out numbers and some stopwords.
	 * @param aContext
	 * @param tokens
	 * @param tags
	 */
	public void tokenCtxt(String aContext, ArrayList tokens, ArrayList tags){
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
					}
				}
			}
		}
	}
	
	public void tokenAfter(String s2,  ArrayList tokens, ArrayList tags){
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
					}
					if(tokens.size()>=this.windowSize)
						break;
					
				}
			}
		}
	}

}
