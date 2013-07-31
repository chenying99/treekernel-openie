package parseFilter.trainEx;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import relationEx.DPPair;
import relationEx.DPType;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreeReader;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import graph.LabeledEdge;

public class Treebank2StanfLabeled {
	/**
	 * Transfer constituent parser structure to Stanford typed collapsed dependency structure
	 * Return the number of tokens in the tree.
	 * @param text
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 * @return
	 * @throws IOException
	 */
	public static int transforGetToken(String text, ArrayList tokenListR, ArrayList posListR,
			ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, LabeledEdge> graph) throws IOException{
		TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		
		//TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		Tree tree = tr.readTree();
		
		List<Label> posList = tree.preTerminalYield();
		
		int tokenId = 0;//the index with NONE deleted, to match with TrainEx.tokenizeSent();
		graph.addVertex("-1_ROOT");
		int posId = 0;//the node index in tree
		ArrayList noneNode=new ArrayList();
		 for(Tree leaf : tree.getLeaves()) {
			   //access CoreAnnotations here
			 CoreLabel label = (CoreLabel) leaf.label();
			 String pos = posList.get(posId).value();
			// System.out.print(pos+"\t");
			 
			 if(!pos.equalsIgnoreCase("-NONE-")){
				 String word = label.get(TextAnnotation.class);
				// System.out.println(word+"_"+tokenId);
				 graph.addVertex(tokenId+"_"+word);
				 tokenListR.add(word);
				 posListR.add(pos);
				 tokenId++;
			 }else{
				// System.out.println(posId);
				 noneNode.add(posId);
			 }
			 posId++;
		 }
		EnglishGrammaticalStructure structure = new EnglishGrammaticalStructure(tree) ;
		List dependList = structure.typedDependenciesCollapsed(false);
		for(int i=0;i<dependList.size();i++){
			TypedDependency tempDP = (TypedDependency)dependList.get(i);
			int end = tempDP.dep().index()-1;
			String endS = tempDP.dep().nodeString();		
			int start =  tempDP.gov().index()-1;
			String startS = tempDP.gov().nodeString();
			end = Treebank2Stanf.getAnotherIndex(end, noneNode);
			start = Treebank2Stanf.getAnotherIndex(start, noneNode);
			String temp1 = start+"_"+startS;
			String temp2 = end+"_"+endS;
			if(temp1.equals(temp2))
				continue;
			String relation = tempDP.reln().toString();
			dpPairList.add(new DPPair(start,end, startS,endS));
			dpTypeList.add(new DPType(relation,1));
			//System.out.println(start+"_"+startS+"=>"+ end+"_"+endS);
			try{
				graph.addEdge(start+"_"+startS, end+"_"+endS, new LabeledEdge(start+"_"+startS, end+"_"+endS, relation));
			}catch( java.lang.IllegalArgumentException e){
				
				System.out.println("error: "+start+"_"+startS+"\t"+end+"_"+endS);
				e.printStackTrace();
				System.exit(0);
			}
			
			//System.out.println(tempDP);
		}
		return tokenId;
	}
	
	/**
	 * Transfer constituent parser structure to Stanford typed collapsed dependency structure
	 * Input the parsed sentence by StanfordCoreNLP pipeline.
	 * Return the number of tokens in the tree.
	 * @param text
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 * @return
	 * @throws IOException
	 */
	public static int transforGetToken(CoreMap sentence, ArrayList tokenListR, ArrayList posListR,ArrayList lemmaListR,
			ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, LabeledEdge> graph) throws IOException{
		int tokenId = 0;
		graph.addVertex("-1_ROOT");
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			tokenListR.add(word);
			// this is the POS tag of the token
			String pos = token.get(PartOfSpeechAnnotation.class);
			posListR.add(pos);
			String lemma = token.get(LemmaAnnotation.class);
			lemmaListR.add(lemma);
			graph.addVertex(tokenId+"_"+word);
			tokenId++;
		}
		Tree tree = sentence.get(TreeAnnotation.class);
		EnglishGrammaticalStructure structure = new EnglishGrammaticalStructure(tree) ;
		List dependList = structure.typedDependenciesCollapsed(false);
		for(int i=0;i<dependList.size();i++){
			TypedDependency tempDP = (TypedDependency)dependList.get(i);
			int end = tempDP.dep().index()-1;
			String endS = tempDP.dep().nodeString();		
			int start =  tempDP.gov().index()-1;
			String startS = tempDP.gov().nodeString();
			String temp1 = start+"_"+startS;
			String temp2 = end+"_"+endS;
			if(temp1.equals(temp2))
				continue;
			String relation = tempDP.reln().toString();
			dpPairList.add(new DPPair(start,end, startS,endS));
			dpTypeList.add(new DPType(relation,1));
			//System.out.println(start+"_"+startS+"=>"+ end+"_"+endS);
			try{
				graph.addEdge(start+"_"+startS, end+"_"+endS, new LabeledEdge(start+"_"+startS, end+"_"+endS, relation));
			}catch( java.lang.IllegalArgumentException e){
				
				System.out.println("error: "+start+"_"+startS+"\t"+end+"_"+endS);
				e.printStackTrace();
				System.exit(0);
			}
			
			//System.out.println(tempDP);
		}
		return tokenListR.size();
	}
	
	/**
	 * Transfer constituent parser structure to Stanford typed collapsed dependency structure
	 * Different structure class. the function is not used.
	 * Return the number of tokens in the tree.
	 * @param text
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 * @return
	 * @throws IOException
	 */
	public static int transforGetToken2(String text, ArrayList tokenListR, ArrayList posListR,
			ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, LabeledEdge> graph) throws IOException{
		TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		
		//TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		Tree tree = tr.readTree();
		
		List<Label> posList = tree.preTerminalYield();
		
		int tokenId = 0;//the index with NONE deleted, to match with TrainEx.tokenizeSent();
		graph.addVertex("-1_ROOT");
		int posId = 0;//the node index in tree
		ArrayList noneNode=new ArrayList();
		 for(Tree leaf : tree.getLeaves()) {
			   //access CoreAnnotations here
			 CoreLabel label = (CoreLabel) leaf.label();
			 String pos = posList.get(posId).value();
			// System.out.print(pos+"\t");
			 
			 if(!pos.equalsIgnoreCase("-NONE-")){
				 String word = label.get(TextAnnotation.class);
				// System.out.println(word+"_"+tokenId);
				 graph.addVertex(tokenId+"_"+word);
				 tokenListR.add(word);
				 posListR.add(pos);
				 tokenId++;
			 }else{
				// System.out.println(posId);
				 noneNode.add(posId);
			 }
			 posId++;
		 }
		 TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		    GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
		    List<TypedDependency> dependList = gs.typedDependenciesCCprocessed();
		for(int i=0;i<dependList.size();i++){
			TypedDependency tempDP = (TypedDependency)dependList.get(i);
			int end = tempDP.dep().index()-1;
			String endS = tempDP.dep().nodeString();		
			int start =  tempDP.gov().index()-1;
			String startS = tempDP.gov().nodeString();
			end = Treebank2Stanf.getAnotherIndex(end, noneNode);
			start = Treebank2Stanf.getAnotherIndex(start, noneNode);
			String temp1 = start+"_"+startS;
			String temp2 = end+"_"+endS;
			if(temp1.equals(temp2))
				continue;
			String relation = tempDP.reln().toString();
			dpPairList.add(new DPPair(start,end, startS,endS));
			dpTypeList.add(new DPType(relation,1));
			System.out.println(start+"_"+startS+"=>"+ end+"_"+endS);
			try{
				graph.addEdge(start+"_"+startS, end+"_"+endS, new LabeledEdge(start+"_"+startS, end+"_"+endS, relation));
			}catch( java.lang.IllegalArgumentException e){
				
				System.out.println("error: "+start+"_"+startS+"\t"+end+"_"+endS);
				e.printStackTrace();
				System.exit(0);
			}
			
			//System.out.println(tempDP);
		}
		return tokenId;
	}
	
	public static void main(String[] args){
		//String text = "( (S (S (PP (RB Instead) (IN of) (NP (CD 50\\/50) )) (NP-SBJ (PRP it) ) (VP (VBD became) (, ,) (PP-LOC (IN on) (NP (NN paper) ) (ADVP (RB only) )) (, ,) (NP-PRD (NP (NNS two-thirds) (NNP Mariotta) ) (, ,) (NP (NN one-third) (NNP Neuberger) )))) (, ,) (CC and) (S (NP-SBJ (PRP they) ) (VP (VBD were) (PP-PRD (PP (IN in) (NP (DT the) (NN program) )) (CC and) (PP (IN off) (PP (TO to) (NP (DT the) (NNS races) )))))) (. .) ))";
		//String text = "( (S (NP-SBJ (NP (NNS Worksheets) ) (PP-LOC (IN in) (NP (NP (NP (DT a) (JJ test-practice) (NN kit) ) (VP (VBN called) (S (NP-SBJ (-NONE- *) ) (NP-PRD-TTL (NNP Learning) (NNPS Materials) )))) (, ,) (VP (VBN sold) (NP (-NONE- *) ) (PP-DTV (TO to) (NP (NP (NNS schools) ) (ADVP-LOC (IN across) (NP (DT the) (NN country) )))) (PP (IN by) (NP-LGS (NNP Macmillan\\/McGraw-Hill) (NNP School) (NNP Publishing) (NNP Co.) ))) (, ,) ))) (VP (VBP contain) (NP (DT the) (JJ same) (NNS questions) )) (. .) )) ";
	//String text = "( (S (NP-SBJ-10 (NP (NNP J.P.) (NNP Bolduc) ) (, ,) (NP (NP (NN vice) (NN chairman) ) (PP (IN of) (NP  (NP (NNP W.R.) (NNP Grace) (CC &) (NNP Co.) ) (, ,)  (SBAR(WHNP-10 (WDT which) )(S(NP-SBJ (-NONE- *T*-10) ) (VP (VBZ holds) (NP(NP (DT a) (ADJP (CD 83.4) (NN %) )  (NN interest) )(PP-LOC (IN in) (NP (DT this) (JJ energy-services) (NN company) )))))))))       (, ,) )     (VP (VBD was)        (VP (VBN elected) (S  (NP-SBJ (-NONE- *-10) ) (NP-PRD (DT a) (NN director) )))) (. .) ))";
	String text = "( (S (S-PRP (NP-SBJ (-NONE- *-2) ) (VP (TO To) (VP (VB wrestle) (PP-CLR (IN with) (NP (DT a) (NN demon) )) (PP-LOC (IN in) (NP (NP (DT a) (NN house) ) (VP (VBN owned) (NP (-NONE- *) ) (PP (IN by) (NP-LGS (DT a) (NAC-LOC (NNP Litchfield) (, ,) (NNP Conn.) (, ,) ) (NN woman) )))))))) (, ,) (NP-SBJ-2 (DT the) (NNS Warrens) ) (ADVP-TMP (RB recently) ) (VP (VBD called) (PRT (RP in) ) (NP (NP (DT an) (NN exorcist) ) (, ,) (NP (DT the) (NNP Rev.) (NNP Robert) (NNP McKenna) ) (, ,) (NP (NP (DT a) (JJ dissident) (NN clergyman) ) (SBAR (WHNP-1 (WP who) ) (S (NP-SBJ (-NONE- *T*-1) ) (VP (VBZ hews) (PP-CLR (TO to) (NP (NP (DT the) (NNP Catholic) (NNP Church) (POS 's) ) (JJ old) (NNP Latin) (NN liturgy) )))))))) (. .) ))";	
	ArrayList<DPPair> dpPairList = new ArrayList();
		ArrayList<DPType> dpTypeList= new ArrayList();
		ArrayList tokenListR = new ArrayList();
		ArrayList posListR = new ArrayList();
		UndirectedGraph<String, LabeledEdge> graph  = new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);
		try {
			int tokensize = Treebank2StanfLabeled.transforGetToken(text, tokenListR, posListR, dpPairList, dpTypeList, graph);
			System.out.println("tokensize:"+tokensize);
			for(int i=0;i<tokenListR.size();i++){
				String word = (String)tokenListR.get(i);
				System.out.print(word);
				System.out.print(" ");
			}
			System.out.println();
			//java.util.List list =  DijkstraShortestPath.findPathBetween(graph,"1_Bolduc", "6_W.R.");
			java.util.List list =  DijkstraShortestPath.findPathBetween(graph,"11_Litchfield", "28_McKenna");
			int distTemp = Integer.MAX_VALUE;
			if(list!=null){
				distTemp = list.size();
			}
			//although the graph is undirected, but the output edge is always put the parents in front as initiate 
			//when first added into the graph.
			System.out.println(distTemp);//(4_chairman : 3_vice)
			for(int i=0;i<list.size();i++){
				LabeledEdge edge = (LabeledEdge) list.get(i);
				System.out.println(edge.getLabel());
				System.out.println(edge);//first one is the gov word, second is the dep word
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
