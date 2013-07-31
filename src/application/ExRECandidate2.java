package application;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
import relationEx.YingClustering;
import stanford.Mention;
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
 * This is experiment to get the original NE list in my tagged treebank sentences.
 * I cannot get the exactly the same stanford parsing any more, so I will use the one in the 
 * tagged sentences, such as:new1000sent_wsj3_dist20_judged_withRpt.txt.Stanf.
 * The main difference between this and ExRECandidate.java in terms of entity pair extraction is that 
 * this one will constraint the types to be per2per, per2org, per2loc, org2org, org2per, org2loc.
 * @author ying
 *
 */
public class ExRECandidate2 {

	int maxEntryTokDist = 20; //the max distance between two named entities is 15.
	int maxDpDist = 6; //the max dependency path distance between relation words and entities.
	public StanfordCoreNLP pipeline= null;

	public Analyzer contextAnalyzer = null;//to get relation patterns.
	
	LexicalizedParser lp = null;
	 TokenizerFactory<CoreLabel> tokenizerFactory = 
		      PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
	 public NPExStanford extraction = new NPExStanford();
	 /**
	  * @param pipeline the StanfordCoreNLP instance
	  * @param stopFile stop word list
	  * @param parsePipe whether to use the parser in stanford pipeline or not. (Note that sometimes the results are different)
	  * @throws IOException
	  */
	public ExRECandidate2(StanfordCoreNLP pipeline, String stopFile, boolean parsePipe) throws IOException {
		this.pipeline = pipeline;
		this.contextAnalyzer = new PhrasePAnalyzerWithOffsetwithDup(4, stopFile, 5);
		if(!parsePipe){
			lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
			
		}
	}
	
	/**
	  * @param pipeline the StanfordCoreNLP instance
	  * @param stopFile stop word list
	   * @throws IOException
	  */
	public ExRECandidate2(StanfordCoreNLP pipeline, String stopFile) throws IOException {
		this.pipeline = pipeline;
		this.contextAnalyzer = new PhrasePAnalyzerWithOffsetwithDup(4, stopFile, 5);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length==0){
			args = new String[3];
			String path = "/media/My Passport/Ying/naacl2013Data/treebankRaw/";
			args[0]=path+"train_raw.txt";
			args[1]=path+"train_raw_triple.txt";
			args[2] = path+"new1000sent_wsj3_dist20_judged_withRpt.txt.Stanf";
		}
		if(args.length!=3){
			System.out.println("Input: rawsentenceFile outFile");
			System.exit(0);
		}
		testOpenIETB(args);
	}
	
	public static void testOpenIETB(String[]args){
		String stopFile = "smallStopWords.txt";
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		//props.put("annotators", "tokenize, ssplit, pos,parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		try {
			ExRECandidate2 relationExtractor = new ExRECandidate2(pipeline, stopFile, false);
			PrintStream output = new PrintStream(args[1]);
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
			BufferedReader input2 = new BufferedReader(new InputStreamReader(new FileInputStream(args[2])));
			
			String line = input.readLine();
			String line2 = input2.readLine();
			int sentCount=0;
			while(line!=null){
				String sent = line;
				line = input.readLine();//<pair>
				line = input.readLine();//one pair
				HashSet pairSet = new HashSet();
				while(line.length()>0){
					String[] chunks = line.split("\t");
					pairSet.add(chunks[0]);
					line = input.readLine();
				}
				
				String sent2 = input2.readLine();
				while(line2.length()>0){
					line2 = input2.readLine();
				}
				
				Annotation document = new Annotation(sent);
				
				// run all Annotators on this text
				pipeline.annotate(document);
				
				TreeReader tr = new PennTreeReader(new StringReader(sent2), new LabeledScoredTreeFactory());
				
				//TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
				Tree tree = tr.readTree();
				
				relationExtractor.extractRE(pairSet,sent,document, tree, output);
				line = input.readLine();
				line2 = input2.readLine();
				sentCount++;
				//if(sentCount>10)
				//	break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Open IE: Extract relations from one sentence.
	 * @param document the pipeline annotated document
	 * @param parsedS: the parsed sentence Tree
	 * @throws IOException 
	 */
	public void extractRE(HashSet pairSet, String rawSent, Annotation document, Tree parsedS, PrintStream output) throws IOException{
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		Tree tree = parsedS;
		output.println("<sent>");
		 output.println(tree.toString());
		 output.println("<pair>");
		 
		 ArrayList tokenList = new ArrayList();
			ArrayList posList = new ArrayList();
			ArrayList lemmaList = new ArrayList();
		for(CoreMap sentence: sentences) {
			ExRECandidate2.getToken(sentence, tokenList, posList, lemmaList);
		}
		ArrayList mentionList = new ArrayList();
		ArrayList tokenList2 = new ArrayList();
		ArrayList NPs = new ArrayList();
		mentionList = this.extraction.extract(rawSent, tokenList2, NPs);
		
		//NPExStanford.extractOneSent(0, mentionList, tokenList2, posList2, sentence);
		tokenList2.clear();
		NPs.clear();
		ArrayList mentionOffsetList = this.getMentionOffsets(mentionList);
		//get the mentionOffsetList first, then get the filtered mentions.
		//otherwise some relation patterns extracted will contain some mention.
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
				//if(name2.equals(name1))
				//	continue;
				String type2 = mention2.getEntity().getType();
				int start2 = mention2.getStartToken();
				int end2 = mention2.getEndToken();
				if(type1.startsWith("PER")||type1.startsWith("ORG")||type2.startsWith("PER")||type2.startsWith("ORG")){

					//System.out.println("\t"+type2+"\t"+start2+"\t"+end2+"\t");
					if(start2-end<=this.maxEntryTokDist){//in a certain distance
						String pairS = mention.getEntity().getName()+":"+start+":"+end+";"+
								mention2.getEntity().getName()+":"+start2+":"+end2;
						
						pairSet.remove(pairS);
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
				}
			}
		}
		output.println();
		if(pairSet.size()>0){
			Iterator iter=pairSet.iterator();
			while(iter.hasNext()){
				String pair = (String)iter.next();
				System.out.println("not get:"+pair);
			}
		}
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
		//System.out.println(sent);
		//System.out.println(tokenList.size());
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
		buffer.setLength(buffer.length()-1);
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
