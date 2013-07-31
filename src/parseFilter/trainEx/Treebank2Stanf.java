package parseFilter.trainEx;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.LeftHeadFinder;
import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeNormalizer;
import edu.stanford.nlp.trees.TreeReader;
import edu.stanford.nlp.trees.TypedDependency;

import relationEx.DPPair;
import relationEx.DPType;

/**
 * Transform the form of treebank to stanford parser.
 * @author ying
 *
 */
public class Treebank2Stanf {
	
	/**
	 * Note: just for AnalyzeJudge use.
	 * @param text
	 * @param tokenListR
	 * @param posListR
	 * @param dependList
	 * @return
	 * @throws IOException
	 */
	public static Tree getTree(String text, ArrayList tokenListR, ArrayList posListR, ArrayList dependList, ArrayList noneNode) throws IOException{
		TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		
		//TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		Tree tree = tr.readTree();
		
		List<Label> posList = tree.preTerminalYield();
		
		int tokenId = 0;//the index with NONE deleted, to match with TrainEx.tokenizeSent();
		int posId = 0;//the node index in tree
		 for(Tree leaf : tree.getLeaves()) {
			   //access CoreAnnotations here
			 CoreLabel label = (CoreLabel) leaf.label();
			 String pos = posList.get(posId).value();
			// System.out.print(pos+"\t");
			 
			 if(!pos.equalsIgnoreCase("-NONE-")){
				 String word = label.get(TextAnnotation.class);
				 tokenListR.add(word);
				 posListR.add(pos);
				// System.out.println(word+"_"+tokenId);
				 tokenId++;
			 }else{
				// System.out.println(posId);
				 noneNode.add(posId);
			 }
			 posId++;
		 }
		 EnglishGrammaticalStructure structure = new EnglishGrammaticalStructure(tree) ;
		 List dependListRaw = structure.typedDependenciesCollapsed(false);
		 dependList.addAll(dependListRaw);
		 return tree;
	}
	
	/**
	 * Extract tokens from a treebank sentence string
	 * @param text
	 * @throws IOException
	 */
	public static void getToken(String text, ArrayList tokenListR, ArrayList posListR) throws IOException{
		TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		
		//TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		Tree tree = tr.readTree();
		
		List<Label> posList = tree.preTerminalYield();
		
		int tokenId = 0;//the index with NONE deleted, to match with TrainEx.tokenizeSent();
		int posId = 0;//the node index in tree
		ArrayList noneNode=new ArrayList();
		 for(Tree leaf : tree.getLeaves()) {
			   //access CoreAnnotations here
			 CoreLabel label = (CoreLabel) leaf.label();
			 String pos = posList.get(posId).value();
			// System.out.print(pos+"\t");
			 
			 if(!pos.equalsIgnoreCase("-NONE-")){
				 String word = label.get(TextAnnotation.class);
				 tokenListR.add(word);
				 posListR.add(pos);
				// System.out.println(word+"_"+tokenId);
				 tokenId++;
			 }else{
				// System.out.println(posId);
				 noneNode.add(posId);
			 }
			 posId++;
		 }
	}
	
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
	public static int transfor(String text, ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, DefaultEdge> graph) throws IOException{
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
			//System.out.println(start+"_"+startS+"\t"+ end+"_"+endS);
			try{
				graph.addEdge(start+"_"+startS, end+"_"+endS);
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
	 * Return the number of tokens in the tree.
	 * @param text
	 * @param dpPairList
	 * @param dpTypeList
	 * @param graph
	 * @return
	 * @throws IOException
	 */
	public static int transforGetToken(String text, ArrayList tokenListR, ArrayList posListR,
			ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList,UndirectedGraph<String, DefaultEdge> graph) throws IOException{
		TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		//TreeReader tr = new PennTreeReader(new StringReader(text), new LabeledScoredTreeFactory());
		Tree tree = tr.readTree();
		System.out.println("tree:"+tree);
		List<Label> posList = tree.preTerminalYield();
		
		int tokenId = 0;//the index with NONE deleted, to match with TrainEx.tokenizeSent();
		graph.addVertex("-1_ROOT");
		int posId = 0;//the node index in tree
		ArrayList noneNode=new ArrayList();
		 for(Tree leaf : tree.getLeaves()) {
			   //access CoreAnnotations here
			 CoreLabel label = (CoreLabel) leaf.label();
			 String pos = posList.get(posId).value();
			System.out.print(pos+"\t");
			 
			 if(!pos.equalsIgnoreCase("-NONE-")){
				 String word = label.get(TextAnnotation.class);
				System.out.println(word+"_"+tokenId);
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
		// EnglishGrammaticalStructure structure = new EnglishGrammaticalStructure(tree);
		EnglishGrammaticalStructure structure = new EnglishGrammaticalStructure(tree,new PennTreebankLanguagePack().punctuationWordRejectFilter(), new LeftHeadFinder()) ;
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
			//System.out.println(start+"_"+startS+"\t"+ end+"_"+endS);
			try{
				graph.addEdge(start+"_"+startS, end+"_"+endS);
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
	 * get the actual index of tokens, ignore the -NONE- in tree structure.
	 * @param index
	 * @param NONEList
	 * @return
	 */
	public static int getAnotherIndex(int index, ArrayList NONEList){
		int indexNew = index;
		for(int i=0;i<NONEList.size();i++){
			int noneTempId = (Integer)NONEList.get(i);
			if(index>noneTempId)
				indexNew--;
		}
		return indexNew;
	}
	
	public static void test(){
		//String text = "( (S (S (PP (RB Instead) (IN of) (NP (CD 50\\/50) )) (NP-SBJ (PRP it) ) (VP (VBD became) (, ,) (PP-LOC (IN on) (NP (NN paper) ) (ADVP (RB only) )) (, ,) (NP-PRD (NP (NNS two-thirds) (NNP Mariotta) ) (, ,) (NP (NN one-third) (NNP Neuberger) )))) (, ,) (CC and) (S (NP-SBJ (PRP they) ) (VP (VBD were) (PP-PRD (PP (IN in) (NP (DT the) (NN program) )) (CC and) (PP (IN off) (PP (TO to) (NP (DT the) (NNS races) )))))) (. .) ))";
				//String text = "( (S (NP-SBJ (NP (NNS Worksheets) ) (PP-LOC (IN in) (NP (NP (NP (DT a) (JJ test-practice) (NN kit) ) (VP (VBN called) (S (NP-SBJ (-NONE- *) ) (NP-PRD-TTL (NNP Learning) (NNPS Materials) )))) (, ,) (VP (VBN sold) (NP (-NONE- *) ) (PP-DTV (TO to) (NP (NP (NNS schools) ) (ADVP-LOC (IN across) (NP (DT the) (NN country) )))) (PP (IN by) (NP-LGS (NNP Macmillan\\/McGraw-Hill) (NNP School) (NNP Publishing) (NNP Co.) ))) (, ,) ))) (VP (VBP contain) (NP (DT the) (JJ same) (NNS questions) )) (. .) )) ";
			String text = "( (S (NP-SBJ-10 (NP (NNP J.P.) (NNP Bolduc) ) (, ,) (NP (NP (NN vice) (NN chairman) ) (PP (IN of) (NP  (NP (NNP W.R.) (NNP Grace) (CC &) (NNP Co.) ) (, ,)  (SBAR(WHNP-10 (WDT which) )(S(NP-SBJ (-NONE- *T*-10) ) (VP (VBZ holds) (NP(NP (DT a) (ADJP (CD 83.4) (NN %) )  (NN interest) )(PP-LOC (IN in) (NP (DT this) (JJ energy-services) (NN company) )))))))))       (, ,) )     (VP (VBD was)        (VP (VBN elected) (S  (NP-SBJ (-NONE- *-10) ) (NP-PRD (DT a) (NN director) )))) (. .) ))";
				ArrayList<DPPair> dpPairList = new ArrayList();
				ArrayList<DPType> dpTypeList= new ArrayList();
				UndirectedGraph<String, DefaultEdge> graph  = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
				try {
					int tokensize = Treebank2Stanf.transfor(text, dpPairList, dpTypeList, graph);
					System.out.println("tokensize:"+tokensize);
					java.util.List list =  DijkstraShortestPath.findPathBetween(graph,"1_Bolduc", "6_W.R.");
					int distTemp = Integer.MAX_VALUE;
					if(list!=null){
						distTemp = list.size();
					}
					//although the graph is undirected, but the output edge is always put the parents in front as initiate 
					//when first added into the graph.
					System.out.println(distTemp);//(4_chairman : 3_vice)
					for(int i=0;i<list.size();i++){
						DefaultEdge edge = (DefaultEdge) list.get(i);
						System.out.println(edge);//first one is the gov word, second is the dep word
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	public static void testNNEGenia(){
		String text = "((S (OTHER (DNA (NN IL-2) (NN gene)) (NN expression) )  (CC and)  (other (protein_molecule (NN NF-kappa) (NN B)) (NN activation))  (IN through) (protein_molecule (NN CD28))  (VBZ requires) (JJ reactive) (NN oxygen) (NN production) (IN by)   (protein_molecule (NN 5-lipoxygenase))  (. .)))";
		ArrayList<DPPair> dpPairList = new ArrayList();
		ArrayList<DPType> dpTypeList= new ArrayList();
		ArrayList tokenList = new ArrayList();
		ArrayList posList = new ArrayList();
		UndirectedGraph<String, DefaultEdge> graph  = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		try {
			int tokensize = Treebank2Stanf.transforGetToken(text,tokenList, posList, dpPairList, dpTypeList, graph);
			System.out.println("tokensize:"+tokensize);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		testNNEGenia();
	}
}
