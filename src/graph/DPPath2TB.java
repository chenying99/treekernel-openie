package graph;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleGraph;

import edu.stanford.nlp.process.Morphology;

import parseFilter.trainEx.Treebank2StanfLabeled;

import relationEx.DPPair;
import relationEx.DPType;

/**
 * Transform the dependency path information to treebank bracket form.
 * For svmlight-tk 's ptk usage.
 * Tree structure reference:
 * Structured Lexical Similarity via Convolution Kernels on Dependency Trees, Croce et.al. 2011 EMNLP
 * @author ying
 *
 */
public class DPPath2TB {
	protected Morphology morph = new Morphology();
	protected ArrayList<DPPair> dpPairList = null;
	protected ArrayList<DPType> dpTypeList= null;
	protected ArrayList tokenList = null;
	protected ArrayList posList = null;
	protected ArrayList lemmaList = null;
	protected UndirectedGraph<String, LabeledEdge> graph  = null;
	//can be added as the POS's child node
	protected static HashSet stopwords = new HashSet();
	//these words are used to replace POS, not just add as POS's child node
	protected static HashSet funcWords = new HashSet();
	protected HashMap govListMap = null; 
	public DPPath2TB(String text, String stopFile) throws IOException{
		this.dpPairList = new ArrayList();
		this.dpTypeList = new ArrayList();
		this.tokenList = new ArrayList();
		this.posList = new ArrayList();
		this.graph = new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);
		
		int tokensize = Treebank2StanfLabeled.transforGetToken(text, tokenList, posList, dpPairList, dpTypeList, graph);
		this.getMorph();
		this.getStopWords(stopFile);
		govListMap = DPPath2TB.getGovList(dpPairList);
	}
	
	/**
	 * Get the lemma for every token in the sentences by stanford tool
	 */
	protected void getMorph(){
		this.lemmaList = new ArrayList();
		for(int i=0;i<this.tokenList.size();i++){
			String tempLemma = this.morph.lemma((String)this.tokenList.get(i), (String)this.posList.get(i));
			this.lemmaList.add(tempLemma);
		}
	}
	
	/**
	 * Read in stop words from a file
	 * @param stopFile
	 * @throws IOException
	 */
	public void getFuncWords(String funcFile) throws IOException{
		if(this.funcWords.size()==0 && funcFile!=null){
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(funcFile)));
			String line = input.readLine();
			while(line!=null){
				this.funcWords.add(line);
				line = input.readLine();
			}
			input.close();
		}
	}
	
	/**
	 * Read in stop words from a file
	 * @param stopFile
	 * @throws IOException
	 */
	public void getStopWords(String stopFile) throws IOException{
		if(this.stopwords.size()==0 && stopFile!=null){
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(stopFile)));
			String line = input.readLine();
			while(line!=null){
				this.stopwords.add(line);
				line = input.readLine();
			}
			input.close();
		}
	}
	
	/**
	 * get the gov wordlist for every dep word. 
	 *<key_String_target value_arrayList_parents<String>>
	 * @param dpPairList
	 * @return
	 */
	public static HashMap getGovList(ArrayList<DPPair> dpPairList){
		HashMap map = new HashMap();
		for(int i=0;i<dpPairList.size();i++){
			DPPair pair = (DPPair)dpPairList.get(i);
			//System.out.println(pair.getStartIndex()+"_"+pair.getStartToken()+"->"+pair.getEndIndex()+"_"+pair.getEndToken());
			String gov = pair.getStartIndex()+"_"+pair.getStartToken();
			String target = pair.getEndIndex()+"_"+pair.getEndToken();
			ArrayList list = null;
			Object listO = map.get(target);
			if(listO==null){
				list = new ArrayList();
			}else
				list = (ArrayList)listO;
			list.add(gov);
			map.put(target, list);
		}
		return map;
	}
	
	public DPPath2TB(ArrayList<DPPair> dpPairList, ArrayList<DPType>dpTypeList, ArrayList tokenList, ArrayList posList,
			UndirectedGraph<String, LabeledEdge> graph, String stopFile) throws IOException{
		this.dpPairList = dpPairList;
		this.dpTypeList = dpTypeList;
		this.tokenList = tokenList;
		this.posList = posList;
		this.graph = graph;
		this.getMorph();
		this.getStopWords(stopFile);
		govListMap = DPPath2TB.getGovList(dpPairList);
	}
	
	/**
	 * I will try to manipulate the pos to change it to the stop words if its lemma is in the stopwords.
	 */
	public void setNewPOS(){
		for(int i=0;i<this.tokenList.size();i++){
			String posTemp = (String)this.posList.get(i);
			if(posTemp.indexOf("NN")==0){
				this.posList.set(i, "NN");
			}else if(posTemp.indexOf("VB")==0){
				this.posList.set(i, "VB");
			}
			String lemmaTemp = (String)this.lemmaList.get(i);
			if(this.funcWords.contains(lemmaTemp)){
				this.posList.set(i, lemmaTemp);
			}
		}
	}
	
	/**
	 * Get the shortest path's Grammatical Relation Centered Tree (GRCT).
	 * @param head1
	 * @param head2
	 * @return
	 */
	public String getTreePath(int head1, int head2){
		String result = null;
		String HToken1 = head1+"_"+(String)this.tokenList.get(head1);
		String HToken2 = head2+"_"+(String)this.tokenList.get(head2);
		//System.out.println("head:"+HToken1+"/t"+HToken2);
		java.util.List list =  DijkstraShortestPath.findPathBetween(graph,HToken1, HToken2);
		if(list!=null){
			//sort the edges in the list.
			this.sortEdges(list);
			//get the parent of the first node
			LabeledEdge edge = (LabeledEdge) list.get(0);
			String node1 = (String)edge.getV1();
			int indexTemp = node1.indexOf("_");
			int indexNode1 = Integer.parseInt(node1.substring(0,indexTemp));
			int parent = -1;
			String parentToken = "ROOT";
			for(int i=0;i<dpPairList.size();i++){
				DPPair pair = (DPPair)this.dpPairList.get(i);
				if(pair.getEndIndex()==indexNode1){
					parent = pair.getStartIndex();
					parentToken= pair.getStartToken();
					break;
				}
			}
			
			edge = this.graph.getEdge(parent+"_"+parentToken, node1);
			
			String relation = edge.getLabel();
			DefaultMutableTreeNode root = this.createTree(list, relation, indexNode1, node1);
			if(root ==null)
				return null;
			result = this.getTree(root);
			//System.out.println(result);
		}
		return result;
	}
	
	/**
	 * Sorted the list, make the parent nodes at the beginning. 
	 * //if the edge parent is not in the childNode set, add at the beginning;
		//else, add at the end. Because it has no loop, we don't need to check if the edge child is in the parent node set.
		
	 * @param list
	 */
	public void sortEdges(List list){
		LinkedList sortList = new LinkedList();
		HashSet childNodes = new HashSet();//the current child node set
		//System.out.println("origin list:");
		for(int i=0;i<list.size();i++){
			LabeledEdge edge = (LabeledEdge) list.get(i);
			//System.out.println(edge);
			String startS = (String)edge.getV1();
			String endS = (String)edge.getV2();
			if(!childNodes.contains(startS)){
				sortList.addFirst(edge);
			}else{
				sortList.addLast(edge);
			}
			childNodes.add(endS);
		}
		list.clear();
		list.addAll(sortList);
	}
	
	public DefaultMutableTreeNode createTree(List edges, String rootS, int top, String topS){
	//	System.out.println("list:");
		//only syntactic relation, i.e. edge label, will have child, so the nodesMap only
		//needs to record string vertex-> DefaultMutableTreeNode node relation
		HashMap nodesMap = new HashMap();//<key_String_node, value_DefaultMutableTreeNode>
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootS);
		String topPOS = (String)this.posList.get(top);
		DefaultMutableTreeNode first = new DefaultMutableTreeNode(topPOS);
		nodesMap.put(topS, root);
		root.add(first);
		
		String topToken = (String)this.lemmaList.get(top);
		if(this.stopwords.contains(topToken)){
			DefaultMutableTreeNode firstW = new DefaultMutableTreeNode(topToken);
			first.add(firstW);
		}
		//because the child of the first edge is still not included
		for(int i=0;i<edges.size();i++){
			//first one is the gov word, second is the dep word
			LabeledEdge edge = (LabeledEdge) edges.get(i);
			String startS = (String)edge.getV1();
			String endS = (String)edge.getV2();
			String relation = edge.getLabel();
			Object parentNodeO = nodesMap.get(startS);
			if(parentNodeO==null)
				return null;
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)parentNodeO;
			DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(relation);
			parentNode.add(childNode);
			nodesMap.put(endS, childNode);
			int endIndex = Integer.parseInt(endS.substring(0,endS.indexOf("_")));
			//then add pos and word
			String pos = (String)this.posList.get(endIndex);
			DefaultMutableTreeNode posNode = new DefaultMutableTreeNode(pos);
			childNode.add(posNode);
			String word = (String)this.lemmaList.get(endIndex);
			if(this.stopwords.contains(word)){
				DefaultMutableTreeNode wNode = new DefaultMutableTreeNode(word);
				posNode.add(wNode);
			}
		}
		//System.out.println();
		return root;
		
	}
	
	public String getTree(DefaultMutableTreeNode parent){
		StringBuffer buffer = new StringBuffer();
		buffer.append("(");
		buffer.append(parent.toString());
		buffer.append(" ");
		int childSize=parent.getChildCount();
		if(childSize>0){
			for(int i=0;i<childSize;i++){
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getChildAt(i);
				buffer.append(this.getTree(child));
			}
		}
		buffer.append(")");
		return buffer.toString();
	}
	
	
	public static void outputTree(DefaultMutableTreeNode parent){
		System.out.print("(");
		System.out.print(parent.toString());
		System.out.print(" ");
		int childSize=parent.getChildCount();
		if(childSize>0){
			for(int i=0;i<childSize;i++){
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getChildAt(i);
				DPPath2TB.outputTree(child);
			}
		}
		System.out.print(")");
	}
	
	public static void main(String[] args){
		//String text = "( (S (S (PP (RB Instead) (IN of) (NP (CD 50\\/50) )) (NP-SBJ (PRP it) ) (VP (VBD became) (, ,) (PP-LOC (IN on) (NP (NN paper) ) (ADVP (RB only) )) (, ,) (NP-PRD (NP (NNS two-thirds) (NNP Mariotta) ) (, ,) (NP (NN one-third) (NNP Neuberger) )))) (, ,) (CC and) (S (NP-SBJ (PRP they) ) (VP (VBD were) (PP-PRD (PP (IN in) (NP (DT the) (NN program) )) (CC and) (PP (IN off) (PP (TO to) (NP (DT the) (NNS races) )))))) (. .) ))";
		//String text = "( (S (NP-SBJ (NP (NNS Worksheets) ) (PP-LOC (IN in) (NP (NP (NP (DT a) (JJ test-practice) (NN kit) ) (VP (VBN called) (S (NP-SBJ (-NONE- *) ) (NP-PRD-TTL (NNP Learning) (NNPS Materials) )))) (, ,) (VP (VBN sold) (NP (-NONE- *) ) (PP-DTV (TO to) (NP (NP (NNS schools) ) (ADVP-LOC (IN across) (NP (DT the) (NN country) )))) (PP (IN by) (NP-LGS (NNP Macmillan\\/McGraw-Hill) (NNP School) (NNP Publishing) (NNP Co.) ))) (, ,) ))) (VP (VBP contain) (NP (DT the) (JJ same) (NNS questions) )) (. .) )) ";
		String text = "( (S (NP-SBJ-10 (NP (NNP J.P.) (NNP Bolduc) ) (, ,) (NP (NP (NN vice) (NN chairman) ) (PP (IN of) (NP  (NP (NNP W.R.) (NNP Grace) (CC &) (NNP Co.) ) (, ,)  (SBAR(WHNP-10 (WDT which) )(S(NP-SBJ (-NONE- *T*-10) ) (VP (VBZ holds) (NP(NP (DT a) (ADJP (CD 83.4) (NN %) )  (NN interest) )(PP-LOC (IN in) (NP (DT this) (JJ energy-services) (NN company) )))))))))       (, ,) )     (VP (VBD was)        (VP (VBN elected) (S  (NP-SBJ (-NONE- *-10) ) (NP-PRD (DT a) (NN director) )))) (. .) ))";
		try {
			DPPath2TB dpPath2TB = new DPPath2TB(text, "stopwords.txt");
			//dpPath2TB.getTreePath(0, 6);
			System.out.println(dpPath2TB.getTreePath(1, 7));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
