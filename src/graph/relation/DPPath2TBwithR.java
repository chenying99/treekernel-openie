package graph.relation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleGraph;

import parseFilter.trainEx.StanfordParser;

import relationEx.DPPair;
import relationEx.DPType;
import relationEx.DPathUtil;
import graph.DPPath2TBwithNE;
import graph.LabeledEdge;

/**
 * Extract the dependency path which has relation words included
 * @author ying
 *
 */
public class DPPath2TBwithR extends DPPath2TBwithNE{
	public static final String R = "R";
	public DPPath2TBwithR(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
		// TODO Auto-generated constructor stub
	}
	
	public DPPath2TBwithR(String text, String stopFile) throws IOException{
		super(text, stopFile);
	}
	
	/**
	 * Get the shortest path's Grammatical Relation Centered Tree (GRCT).
	 * @param head1
	 * @param head2
	 * @return
	 */
	public String getTreePath(int head1, int head2, int rStart1, int rEnd1){
		HashSet rTokenSet = this.getRelationToken(rStart1, rEnd1);
		return this.getTreePath(head1, head2, rTokenSet);
	}
	
	/**
	 * Get the shortest path's Grammatical Relation Centered Tree (GRCT).
	 * Here the rOffsets has three consecutive segments. For my regular expression relation words.
	 * @param head1
	 * @param head2
	 * @return
	 */
	public String getTreePath(int head1, int head2, int[] rOffsets){
		HashSet rTokenSet = new HashSet();
		for(int i=0;i<3;i++){
			if(rOffsets[i*2]!=-1){
				HashSet rTokenSet2 = this.getRelationToken(rOffsets[i*2], rOffsets[i*2+1]);
				rTokenSet.addAll(rTokenSet2);
			}
		}
		
		return this.getTreePath(head1, head2, rTokenSet);
	}
	
	/**
	 * Get the shortest path's Grammatical Relation Centered Tree (GRCT).
	 * Here the rOffsets can be non Consecutive.
	 * @param head1
	 * @param head2
	 * @return
	 */
	public String getTreePath(int head1, int head2, ArrayList rOffsets){
		
		HashSet rTokenSet = this.getGraphNodeForm(rOffsets);
		return this.getTreePath(head1, head2, rTokenSet);
	}
	
	public  HashSet getGraphNodeForm(ArrayList rOffsets){
		HashSet rTokenSet = new HashSet();
		for(int index=0;index<rOffsets.size();index++){
			int i=(Integer)rOffsets.get(index);
			rTokenSet.add(i+"_"+(String)this.tokenList.get(i));
			System.out.println(i+"_"+(String)this.tokenList.get(i));
		}
		return rTokenSet;
	}
	
	
	/**
	 * Get the shortest path's Grammatical Relation Centered Tree (GRCT).
	 * Here we will add the relation tokens into the path. 
	 * 1. only all the words in the shortest path between two NEs are added.
	 * 2. 
	 * if no relation word is added in 1, get the set of path from relation words to E1,
	 * get the set of path from relation words to E2, 
	 * get the shortest path from these two sets, add it to the tree
	 * @param head1
	 * @param head2
	 * @return
	 */
	public String getTreePath(int head1, int head2, HashSet rTokenSet){
		String result = null;
		String HToken1 = head1+"_"+(String)this.tokenList.get(head1);
		String HToken2 = head2+"_"+(String)this.tokenList.get(head2);
		//System.out.println("two entities' head:"+HToken1+"/"+HToken2);
		java.util.List list =  DijkstraShortestPath.findPathBetween(graph,HToken1, HToken2);
		if(list!=null){
			//outEdgeList(list);
			//get the path from NEs to Relation
			boolean hasLink = this.addR2EdgeList(list, rTokenSet, HToken1, HToken2);
			if(!hasLink){
				return null;
			}
			//outEdgeList(list);
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
					if(parent!=indexNode1){
						parentToken= pair.getStartToken();
						break;
					}
				}
			}
			//get the dep relation between the root node and its parent node
			//System.out.println("no edge from: "+parent+"_"+parentToken+"->"+ node1);
			edge = this.graph.getEdge(parent+"_"+parentToken, node1);
			
			String relationDP = edge.getLabel();
			
			DefaultMutableTreeNode root = this.createTree(HToken1, HToken2,list, relationDP, indexNode1, node1, rTokenSet);
			if(root ==null)
				return null;
			result = this.getTree(root);
			//System.out.println(result);
		}
		return result;
	}
	
	public HashSet getRelationToken(int start1, int end1){
		//System.out.println("rStart:"+start1+"\trEnd:"+end1);
		HashSet tokenSet = new HashSet();
		//System.out.println("relation node set: ");
		for(int i=start1;i<=end1;i++){
			tokenSet.add(i+"_"+(String)this.tokenList.get(i));
			//System.out.println(i+"_"+(String)this.tokenList.get(i));
		}
		return tokenSet;
		
	}
	
	/**
	 * Here we will add the relation tokens into the path. 
	 * 1. only all the words in the shortest path between two NEs are added.
	 * 2. 
	 * if no relation word is added in 1, get the set of path from relation words to E1,
	 * get the set of path from relation words to E2, 
	 * get the shortest path from these two sets, add it to the tree
	 * @param list
	 * @param rTokenSet
	 */
	public boolean addR2EdgeList(List list, HashSet rTokenSet, String HToken1, String HToken2){
		//1. 
		if(this.hasNodesIntersec(list, rTokenSet)){
			return true;
		}
		
		//2. 
		//2.1 find links to E2 first, as usually this is the case.
		List shortestList = null;
		shortestList = DPPath2TBwithR.findSP(graph, HToken2, rTokenSet);
		//2.2 use the links to E1 only if the shortest path from E2 has E1, or there is
		//no link from E2 to relation at all.
		if(shortestList==null || this.hasNode(shortestList, HToken1)){
			shortestList = DPPath2TBwithR.findSP(graph, HToken1, rTokenSet);
		}
		
		//3. add the shortestList to the list
		if(shortestList!=null){
			for(int i=0;i<shortestList.size();i++){
				if(!list.contains(shortestList.get(i)))
					list.add(shortestList.get(i));
			}
			return true;
		}else{
			System.out.println("no path from NEs to relation...");
			return false;
		}
	}
	
	/**
	 * Find shortest path from one node to a set of other nodes.
	 */
	public static List findSP( UndirectedGraph<String, LabeledEdge> graph, String HToken2, HashSet rTokenSet){
		List shortestList = null;
		Iterator iter = rTokenSet.iterator();
		while(iter.hasNext()){
			String rToken = (String)iter.next();
			java.util.List listTemp =  DijkstraShortestPath.findPathBetween(graph,HToken2, rToken);
			if(listTemp!=null && listTemp.size()>0){
				if(shortestList==null){
					shortestList = listTemp;
				}else{
					if(listTemp.size()<shortestList.size()){
						shortestList = listTemp;
					}
				}
			}
		}
		return shortestList;
	}
	
	/**
	 * Check if a list has the nodes in the set
	 * @param list
	 * @return
	 */
	public boolean hasNodesIntersec(List list, HashSet rTokenSet){
		for(int i=0;i<list.size();i++){
			LabeledEdge edge = (LabeledEdge) list.get(i);
			String startS = (String)edge.getV1();
			String endS = (String)edge.getV2();
			if(rTokenSet.contains(startS)|| rTokenSet.contains(endS))
				return true;
		}
		return false;
	}
	
	/**
	 * Check if a list has the node
	 * @param list
	 * @return
	 */
	public boolean hasNode(List list, String node){
		for(int i=0;i<list.size();i++){
			LabeledEdge edge = (LabeledEdge) list.get(i);
			String startS = (String)edge.getV1();
			String endS = (String)edge.getV2();
			if(startS.equals(node)|| endS.equals(node))
				return true;
		}
		return false;
	}
	
	/**
	 * The difference with the DPPath2TBwithNE's createTree is that the R is added.
	 */
	public DefaultMutableTreeNode createTree(String HToken1, String HToken2, List edges, String rootS, int top, String topS, HashSet rTokenSet){
		//	System.out.println("list:");
		//only syntactic relation, i.e. edge label, will have child, so the nodesMap only
		//needs to record string vertex-> DefaultMutableTreeNode node relation
		HashMap nodesMap = new HashMap();//<key_String_node, value_DefaultMutableTreeNode>
		String topPOS = (String)this.posList.get(top);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(topPOS);
		nodesMap.put(topS, root);

		String topToken = (String)this.lemmaList.get(top);
		if(topS.equals(HToken2)||topS.equals(HToken1)){
			DefaultMutableTreeNode firstW = new DefaultMutableTreeNode(this.NE);
			root.add(firstW);
		}else if(rTokenSet.contains(topS)){
			DefaultMutableTreeNode firstW = new DefaultMutableTreeNode(this.R);
			root.add(firstW);
		}else if(this.stopwords.contains(topToken)){
			DefaultMutableTreeNode firstW = new DefaultMutableTreeNode(topToken);
			root.add(firstW);
		}
		/*for(int i=0;i<edges.size();i++){
			//first one is the gov word, second is the dep word
			LabeledEdge edge = (LabeledEdge) edges.get(i);
			System.out.println(edge);
		}*/
		
		//because the child of the first edge is still not included
		for(int i=0;i<edges.size();i++){
			//first one is the gov word, second is the dep word
			LabeledEdge edge = (LabeledEdge) edges.get(i);
			//System.out.println(edge);
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
			if(endS.equals(HToken2)||endS.equals(HToken1)){
				DefaultMutableTreeNode wNode = new DefaultMutableTreeNode(this.NE);
				posNode.add(wNode);
			}else if(rTokenSet.contains(endS)){
				DefaultMutableTreeNode wNode = new DefaultMutableTreeNode(this.R);
				posNode.add(wNode);
			}else if(this.stopwords.contains(word)){
				DefaultMutableTreeNode wNode = new DefaultMutableTreeNode(word);
				posNode.add(wNode);
			}
		}
		//System.out.println();
		return root;

	}
	
	public static void outEdgeList(List list){
		for(int i=0;i<list.size();i++){
			LabeledEdge edge = (LabeledEdge) list.get(i);
			System.out.println(edge);
		}
	}
	
	public static void testTBSent() throws IOException{
		String text = "( (S (NP-SBJ-10 (NP (NNP J.P.) (NNP Bolduc) ) (, ,) (NP (NP (NN vice) (NN chairman) ) (PP (IN of) (NP  (NP (NNP W.R.) (NNP Grace) (CC &) (NNP Co.) ) (, ,)  (SBAR(WHNP-10 (WDT which) )(S(NP-SBJ (-NONE- *T*-10) ) (VP (VBZ holds) (NP(NP (DT a) (ADJP (CD 83.4) (NN %) )  (NN interest) )(PP-LOC (IN in) (NP (DT this) (JJ energy-services) (NN company) )))))))))       (, ,) )     (VP (VBD was)        (VP (VBN elected) (S  (NP-SBJ (-NONE- *-10) ) (NP-PRD (DT a) (NN director) )))) (. .) ))";
		DPPath2TBwithR dpPath2TB = new DPPath2TBwithR(text, "stopwords.txt");
		
		String result = dpPath2TB.getTreePath(1, 7,3,5);
		if(result!=null){
			System.out.println(result);
		}else{
			System.out.println("resutl==null");
		}
	}
	
	public static void testRawSent() throws IOException{
		StanfordParser parser = new StanfordParser();
		//String text = "This extension point allows tools to handle the presentation aspects of a debug model .";
		String text = "UStates president Obama says so.";
		ArrayList<DPPair> dpPairList = new ArrayList();
		ArrayList<DPType> dpTypeList= new ArrayList();
		ArrayList tokenList = new ArrayList();
		ArrayList posList = new ArrayList();
		UndirectedGraph<String, LabeledEdge> graph  = new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);
		parser.parseLabeledEdge(text, tokenList, posList, dpPairList, dpTypeList, graph);
		DPPath2TBwithR dpPath2TB = new DPPath2TBwithR(dpPairList,
				dpTypeList, tokenList,
				posList,  graph,
				"stopwords.txt");
		HashMap govListMap = DPathUtil.getGovList(dpPairList);
		int head1 = DPathUtil.getHeadReverb(govListMap, tokenList, posList,0, 0);
		int head2 = DPathUtil.getHeadReverb(govListMap, tokenList, posList,2, 2);
		//System.out.println(head1);
		String result = dpPath2TB.getTreePath(head1, head2,1,1);
		if(result!=null){
			System.out.println(result);
		}else{
			System.out.println("resutl==null");
		}
	}
	
	public static void main(String[] args){
		
		try {
			testRawSent();
			//testTBSent();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
