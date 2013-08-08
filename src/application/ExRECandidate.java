package application;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import parseFilter.NPExStanford;
import patternEX.lucene.PatternOffsetAttribute;
import patternEX.lucene.PatternTypeAttribute;
import patternEX.lucene.PhrasePAnalyzerWithOffsetwithDup;
import patternEX.lucene.PhrasePTokenizerWithOffset;
import stanford.Mention;

import relationEx.YingClustering;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeReader;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Input untagged sentences.
 * Output parsed sentences and possible relation triple candidates, extracted by 
 * the relation pattern.
 * @author ying
 *
 */
public class ExRECandidate{
	int maxEntryTokDist = 20; //the max distance between two named entities is 15.
	int maxDpDist = 6; //the max dependency path distance between relation words and entities.
	public StanfordCoreNLP pipeline= null;

	public Analyzer contextAnalyzer = null;//to get relation patterns.
	
	LexicalizedParser lp = null;
	 TokenizerFactory<CoreLabel> tokenizerFactory = 
		      PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
	
	/**
	  * @param pipeline the StanfordCoreNLP instance
	  * @param stopFile stop word list
	   * @throws IOException
	  */
	public ExRECandidate(StanfordCoreNLP pipeline, String stopFile) throws IOException {
		this.pipeline = pipeline;
		this.contextAnalyzer = new PhrasePAnalyzerWithOffsetwithDup(4, stopFile, 5);
		lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length==0){
			args = new String[2];
			String path = "example/";
			args[0]=path+"devRaw.txt";
			args[1]=path+"devRaw_triple.txt";
		}
		if(args.length<2){
			System.out.println("Input: rawsentenceFile outFile");
			System.exit(0);
		}
		testOpenIE(args);
	}

	public static void testOpenIE(String[]args){
		String stopFile = "smallStopWords.txt";
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		//props.put("annotators", "tokenize, ssplit, pos,parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		try {
			ExRECandidate relationExtractor = new ExRECandidate(pipeline, stopFile);
			PrintStream output = new PrintStream(args[1]);
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
			String line = input.readLine();
			while(line!=null){
				String sent = line;
				Annotation document = new Annotation(sent);
				// run all Annotators on this text
				pipeline.annotate(document);
				relationExtractor.extractRE(document,sent, output);
				line = input.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Open IE: Extract relations from one sentence.
	 * @param document the pipeline annotated document
	 * @param rawSent: the raw sentence.
	 * @throws IOException 
	 */
	public void extractRE(Annotation document, String rawSent, PrintStream output) throws IOException{
		List<CoreLabel> rawWords2 = 
				tokenizerFactory.getTokenizer(new StringReader(rawSent)).tokenize();
		Tree tree = this.lp.apply(rawWords2);
		 output.println("<sent>");
		 output.println(tree.toString());
		 output.println("<pair>");
		 
		 List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		 ArrayList tokenList = new ArrayList();
		 ArrayList posList = new ArrayList();
		 ArrayList lemmaList = new ArrayList();
		 for(CoreMap sentence: sentences) {
			 ExRECandidate.getToken(sentence, tokenList, posList, lemmaList);
		 }
		 ArrayList mentionList = new ArrayList();
		 ArrayList tokenList2 = new ArrayList();
		 mentionList = NPExStanford.extract(document, tokenList2);
		 tokenList2.clear();
		 ArrayList mentionOffsetList = this.getMentionOffsets(mentionList);
		 mentionList = this.filterMention(mentionList);

		 for(int i=0;i<mentionList.size();i++){
			 Mention mention = (Mention)mentionList.get(i);
			 String type1 = mention.getEntity().getType();
			 String name1 = mention.getEntity().getName();
			 int start = mention.getStartToken();
			 int end = mention.getEndToken();
			 //System.out.println(type1+"\t"+start+"\t"+end);
			 for(int j=i+1;j<mentionList.size();j++){
				 Mention mention2 = (Mention)mentionList.get(j);
				 String name2 = mention2.getEntity().getName();
				 if(name1.equals(name2)){
					 continue;
				 }
				 String type2 = mention2.getEntity().getType();
				 int start2 = mention2.getStartToken();
				 int end2 = mention2.getEndToken();
				 //System.out.println("\t"+type2+"\t"+start2+"\t"+end2+"\t");
				 if(start2-end<=this.maxEntryTokDist){//in a certain distance
					 String pairS = mention.getEntity().getName()+":"+start+":"+end+";"+
							 mention2.getEntity().getName()+":"+start2+":"+end2;

					 //now for every pair, I need to extract relations.
					 ArrayList relations = this.extractREPair(start, end, start2, end2, tokenList, posList, lemmaList,mentionOffsetList);
					 if(relations.size()==0){
						 //output.println("\tno relation.");
					 }else{
						 //output.println("possible relations:");
						 for(int in=0;in<relations.size();in++){
							 Relation rtemp = (Relation)relations.get(in);
							 output.println(pairS+"\t"+rtemp.rWords+"\t"+getOffsetS(rtemp.rOffsets));
						 }
					 }
				 }
			 }//j
		 }//i
		 output.println();
	}

	/**
	 * Return arrayList of Relation instances
	 * @param start1
	 * @param end1
	 * @param start2
	 * @param end2
	 * @param tokenList
	 * @param posList
	 * @param mentionOffsetList
	 * @return
	 * @throws IOException
	 */
	public ArrayList extractREPair(int start1, int end1,int start2, int end2, ArrayList tokenList, ArrayList posList, ArrayList lemmaList, ArrayList mentionOffsetList) throws IOException{
		//get the form required by Analyzer
		String sent = this.getTaggedSent(tokenList, posList, lemmaList);
		//offset is in the form of 4-5;7-7
		int[] pairOffset = {start1,end1,start2,end2};
		String sentNew = getPTExForm(sent,  start1, end1, start2, end2);

		//get patterns in the sentence, with all the information: pattern offset, pattern type
		boolean isRelation = false;
		StringReader reader = new StringReader(sentNew); 
		TokenStream tokens = contextAnalyzer.tokenStream("context",reader);
		TermAttribute termAtt = (TermAttribute) tokens.addAttribute(TermAttribute.class);
		PatternTypeAttribute typeAtt = (PatternTypeAttribute) tokens.addAttribute(PatternTypeAttribute.class);
		PatternOffsetAttribute offsetAtt = (PatternOffsetAttribute)tokens.addAttribute(PatternOffsetAttribute.class);
		tokens.reset();
		// print all tokens until stream is exhausted
		ArrayList relation = new ArrayList();

		while (tokens.incrementToken()) {
			String term = termAtt.term();
			String types = typeAtt.getType();
			String[] typeChunk = types.split(";");
			int[] type = new int[3];
			for(int tempI = 0;tempI<typeChunk.length;tempI++)
				type[tempI] = Integer.parseInt(typeChunk[tempI]);
			String offset = offsetAtt.getOffset();
			int[] relationOffsets = PhrasePTokenizerWithOffset.parseOffset(offset);
			//System.out.println("candidate pattern:"+term);
			//System.out.println("candidate offset:"+offset);
			//the term can not be in one named entity
			boolean isMention = false;
			for(int i=0;i<3;i++){
				for(int j=relationOffsets[2*i];j<=relationOffsets[2*i+1];j++){
					//need to find the relation extraction set, check if I used this one
					if(mentionOffsetList.contains(j))
						isMention = true;
				}
			}
			if(!isMention){
				//now check if the relation is correct with SVM tree kernel.
				boolean clsLabel = true;//DependEx.dpFilter(dpPairSet, pairOffset, relationOffsets, type);
				//System.out.println("clsLabel:"+clsLabel);
				if(clsLabel){
					isRelation = true;
					relation.add(new Relation(term,relationOffsets, type));
				}
			}
		}
		tokens.end();
		tokens.close();
		return relation;
	}

	/**
	 * Get the new sentence form for relation pattern extraction
	 * @param sent
	 * @return
	 */
	public String getPTExForm(String sent,int start1, int end1, int start2, int end2){
		//offset is in the form of 4-5;7-7
		String offsets = start1+"-"+end1+";"+start2+"-"+end2;
		String[] segments = YingClustering.getSegment(sent,start1, end1, start2,end2);
		if(segments==null)
			return null;
		String before = segments[0];
		String between = segments[1];
		String after = segments[2];
		String sentNew = before +"<PER>"+between+"<PER>"+after+"<OFFSET>"+offsets;
		return sentNew;	
	}


	/**
	 * Get the sentence in the form of word/pos/lemma word/pos/lemma; 
	 * 
	 * @param tokenList
	 * @param posList
	 * @return
	 */
	public String getTaggedSent(ArrayList tokenList, ArrayList posList, ArrayList lemmaList){
		StringBuffer sent = new StringBuffer();
		for(int i=0;i<tokenList.size();i++){
			String token = (String)tokenList.get(i);
			String pos = (String)posList.get(i);
			String lemma = (String)lemmaList.get(i);
			sent.append(token).append("/").append(pos).append("/").append(lemma).append(" ");
		}
		return sent.toString().trim();
	}

	public String getOffsetS(int[]offset){
		StringBuffer buffer = new StringBuffer();
		for(int i=0;i<3;i++){
			buffer.append(offset[2*i]);
			buffer.append(",");
			buffer.append(offset[2*i+1]);
			buffer.append(";");
		}
		return buffer.toString();
	}
	
	public ArrayList filterMention(ArrayList mentionList){
		ArrayList list = new ArrayList();
		if(mentionList!=null && mentionList.size()>0){
			for(int i=0;i<mentionList.size();i++){
				Mention mention = (Mention)mentionList.get(i);
				String type = mention.getType();
				if(type.startsWith("PERSON")||type.startsWith("ORGANIZATION")||type.startsWith("LOCATION")){
					//System.out.println(type);
					list.add(mention);
				}
			}
		}
		return list;
	}

	/**
	 * Return all the offset whose words are mentions.
	 * @param mentionList
	 * @return
	 */
	public ArrayList getMentionOffsets( ArrayList mentionList){
		ArrayList mentionOffset = new ArrayList();
		if(mentionList!=null && mentionList.size()>0){
			for(int i=0;i<mentionList.size();i++){
				Mention mention = (Mention)mentionList.get(i);
				int start = mention.getStartToken();
				int end = mention.getEndToken();
				for(int j=start;j<=end;j++)
					mentionOffset.add(j);
			}
		}
		return mentionOffset;
	}

	public static int getToken(CoreMap sentence, ArrayList tokenListR, ArrayList posListR,ArrayList lemmaListR) throws IOException{
		int tokenId = 0;
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			tokenListR.add(word);
			// this is the POS tag of the token
			String pos = token.get(PartOfSpeechAnnotation.class);
			posListR.add(pos);
			String lemma = token.get(LemmaAnnotation.class);
			lemmaListR.add(lemma);
			tokenId++;
		}
		return tokenListR.size();
	}

}
