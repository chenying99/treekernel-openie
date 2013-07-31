package parseFilter.trainEx;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import relationEx.DPPair;
import relationEx.DPType;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
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
import edu.stanford.nlp.process.WhitespaceTokenizer;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import graph.LabeledEdge;

/**
 * Test the raw sentence by stanford parser, and get the graph.
 * How to get the shortest distance example:
 * java.util.List list =  DijkstraShortestPath.findPathBetween(graph,"0_Survivors", "3_wife");
 * list has the edges of the path.
 * So the path length is the list size.
 * @author ying
 *
 */
public class StanfordParser {
	LexicalizedParser lp = null;
	TokenizerFactory<CoreLabel> tokenizerFactory = 
		      PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
	 TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	public StanfordParser(){
		lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	}
	
	/**
	 * Constructor
	 * @param tokenized: whether the sentence is already tokenized by space.
	 */
	public StanfordParser(boolean tokenized){
		lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		
		if(tokenized){
			this.tokenizerFactory = WhitespaceTokenizer.newCoreLabelTokenizerFactory();
			
		}
	}
	
	/**
	 * Input text.
	 * All other parameters are empty, which will be filled in this function.
	 * @param text
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 */
	public void parser(String text, ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, DefaultEdge> graph ){
		//tokenize
		List<CoreLabel> rawWords2 = 
				tokenizerFactory.getTokenizer(new StringReader(text)).tokenize();
		graph.addVertex(-1+"_ROOT");
		for (int tokenId = 0; tokenId<rawWords2.size();tokenId++) {
			CoreLabel token = rawWords2.get(tokenId);
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			graph.addVertex(tokenId+"_"+word);
			System.out.println("node: "+tokenId+"_"+word);
		}
		//parse
		Tree parse = lp.apply(rawWords2);
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		List<TypedDependency> tdl =gs.typedDependenciesCCprocessed();
		System.out.println(tdl);
		for(int i=0;i<tdl.size();i++){
			TypedDependency tempDP = (TypedDependency)tdl.get(i);
			int end = tempDP.dep().index()-1;
			String endS = tempDP.dep().nodeString();		
			int start =  tempDP.gov().index()-1;
			String startS = tempDP.gov().nodeString();
			String relation = tempDP.reln().toString();
			System.out.println(start+"_"+startS+"\t"+end+"_"+endS);
			dpPairList.add(new DPPair(start,end, startS,endS));
			dpTypeList.add(new DPType(relation,1));
			graph.addEdge(start+"_"+startS, end+"_"+endS);
		}
		
	}
	
	/*public void parseTokened(String text,  ArrayList tokenList, ArrayList posList,  ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, DefaultEdge> graph){
		TokenizerFactory<CoreLabel> tokenizerFactory2 = WhitespaceTokenizer.newCoreLabelTokenizerFactory();
		List<CoreLabel> rawWords2 = tokenizerFactory2.getTokenizer(new StringReader(text)).tokenize();
		this.parse(rawWords2, tokenList, posList, dpPairList, dpTypeList, graph);
	}*/
	
	/**
	 * Input text.
	 * All other parameters are empty, which will be filled in this function.
	 * @param text
	 * @param tokenList
	 * @param posList
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 */
	public void parse(String text, ArrayList tokenList, ArrayList posList,  ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, DefaultEdge> graph){
		List<CoreLabel> rawWords2 = 
			      tokenizerFactory.getTokenizer(new StringReader(text)).tokenize();
		this.parse(rawWords2, tokenList, posList, dpPairList, dpTypeList, graph);

	}
	
	/**
	 * Input text.
	 * All other parameters are empty, which will be filled in this function.
	 * @param rawWords2
	 * @param tokenList
	 * @param posList
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 */
	public void parse(List<CoreLabel> rawWords2, ArrayList tokenList, ArrayList posList,  ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, DefaultEdge> graph){
		Tree parse = lp.apply(rawWords2);

		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		
		// nodes
		ArrayList<TaggedWord> taggedWordList = parse.taggedYield();
		graph.addVertex("-1_ROOT");
		for (int i=0;i<taggedWordList.size();i++)
		{
			TaggedWord taggedWord = (TaggedWord)taggedWordList.get(i);
			String lex = taggedWord.word();
			String pos = taggedWord.tag();
			tokenList.add(lex);
			posList.add(pos);
			graph.addVertex(i+"_"+lex);
			System.out.println(i+"_"+lex);
		}
		
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		//System.out.println(tdl);
		for(int i=0;i<tdl.size();i++){
			TypedDependency tempDP = (TypedDependency)tdl.get(i);
			int end = tempDP.dep().index()-1;
			String endS = tempDP.dep().nodeString();		
			int start =  tempDP.gov().index()-1;
			String startS = tempDP.gov().nodeString();
			String relation = tempDP.reln().toString();
			dpPairList.add(new DPPair(start,end, startS,endS));
			dpTypeList.add(new DPType(relation,1));
			System.out.println("\tedge:"+start+"_"+startS+"->"+end+"_"+endS);
			graph.addEdge(start+"_"+startS, end+"_"+endS);
		}

	}
	
	/**
	 * Input text.
	 * All other parameters are empty, which will be filled in this function.
	 * @param text
	 * @param tokenList
	 * @param posList
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 * @return
	 */
	public Tree parseLabeledEdge(String text, ArrayList tokenList, ArrayList posList,  ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, LabeledEdge> graph){
		List<CoreLabel> rawWords2 = 
			      tokenizerFactory.getTokenizer(new StringReader(text)).tokenize();
		return this.parseLabeledEdge(rawWords2, tokenList, posList, dpPairList, dpTypeList, graph);
	}
	
	/**
	 * Input text.
	 * All other parameters are empty, which will be filled in this function.
	 * @param rawWords2
	 * @param tokenList
	 * @param posList
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 * @return
	 */
	public Tree parseLabeledEdge(List<CoreLabel> rawWords2, ArrayList tokenList, ArrayList posList,  ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, LabeledEdge> graph){
		Tree parse = lp.apply(rawWords2);

		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		
		// nodes
		graph.addVertex("-1_ROOT");
		ArrayList<TaggedWord> taggedWordList = parse.taggedYield();
		for (int i=0;i<taggedWordList.size();i++)
		{
			TaggedWord taggedWord = (TaggedWord)taggedWordList.get(i);
			String lex = taggedWord.word();
			String pos = taggedWord.tag();
			tokenList.add(lex);
			posList.add(pos);
			graph.addVertex(i+"_"+lex);
		}
		
		
		List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		//System.out.println(tdl);
		for(int i=0;i<tdl.size();i++){
			TypedDependency tempDP = (TypedDependency)tdl.get(i);
			int end = tempDP.dep().index()-1;
			String endS = tempDP.dep().nodeString();		
			int start =  tempDP.gov().index()-1;
			String startS = tempDP.gov().nodeString();
			String relation = tempDP.reln().toString();
			dpPairList.add(new DPPair(start,end, startS,endS));
			dpTypeList.add(new DPType(relation,1));
			//System.out.println("\tedge:"+start+"_"+startS+"->"+end+"_"+endS);
			try{
				graph.addEdge(start+"_"+startS, end+"_"+endS, new LabeledEdge(start+"_"+startS, end+"_"+endS, relation));
			}catch(java.lang.IllegalArgumentException e){
				//e.printStackTrace();
				System.out.println("has loop when add "+startS+"->"+endS);
				continue;
			}
		}

		return parse;
	}

	
	public static void main(String[] args){
		//String text = "Survivors include his wife of 36 years , Nancy F. Yates of Placitas ; and two sons , Brian Yates of San Diego and Michael Yates of Albuquerque. ";
		//String text = "B is norminated as president of U.S.";
		//check the problem of two parents, result: no error
		String text = "I saw the man who loves you .";
		//check the problem of two parents, result: no error
		//String text = "Bills on ports and immigration were submitted.";
		//String text = "Bell makes and distributes products.";

		StanfordParser parser = new StanfordParser(true);
		ArrayList tokenList = new ArrayList();
		ArrayList posList = new ArrayList();
		ArrayList<DPPair> dpPairList = new ArrayList();
		ArrayList<DPType> dpTypeList= new ArrayList();
		
		UndirectedGraph<String, DefaultEdge> graph  = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		parser.parse(text, tokenList, posList, dpPairList, dpTypeList, graph);
		for(int i=0;i<tokenList.size();i++){
			System.out.println((String)tokenList.get(i)+"/"+(String)posList.get(i));
		}
		
		/*UndirectedGraph<String, LabeledEdge> graph  = new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);
		Tree t = parser.parseLabeledEdge(text, tokenList, posList, dpPairList, dpTypeList, graph);
		for(int i=0;i<tokenList.size();i++){
			System.out.println((String)tokenList.get(i)+"/"+(String)posList.get(i));
		}
		System.out.println(t.toString());
		//java.util.List list =  DijkstraShortestPath.findPathBetween(graph,"0_B", "6_U.S.");
		java.util.List list =  DijkstraShortestPath.findPathBetween(graph,"3_man", "6_you");
		//java.util.List list =  DijkstraShortestPath.findPathBetween(graph,"0_Bell", "4_products");
		
		//java.util.List list =  DijkstraShortestPath.findPathBetween(graph,"0_Bills", "4_immigration");
        System.out.println(list.size());
        for(int i=0;i<list.size();i++){
        	DefaultEdge edge = (DefaultEdge)list.get(i);
        	System.out.println(edge.toString());
        }*/
	}
}
