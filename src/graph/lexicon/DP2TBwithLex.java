package graph.lexicon;

import graph.DPPath2TB;
import graph.LabeledEdge;
import graph.relation.NodewithOrder;
import graph.relation.SortTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;

import relationEx.DPPair;
import relationEx.DPType;

/**
 * Transform the dependency path information to treebank bracket form.
 * The difference with DPPath2TB.java is that I want to preserve the word here. 
 * NE tag will be add to the POS tags.
 * @author ying
 *
 */
public class DP2TBwithLex extends DPPath2TB{
	public static final String NE = "NE";
	public DP2TBwithLex(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
		// TODO Auto-generated constructor stub
	}
	
	public DP2TBwithLex(String text, String stopFile) throws IOException{
		super(text, stopFile);
	}
	
	/**
	 * Get the shortest path's Grammatical Relation Centered Tree (GRCT).
	 * It's copied from the DPPath2TBwithNE.java
	 * @param head1
	 * @param head2
	 * @return
	 */
	public String getTreePath(int head1, int head2){
		String result = null;
		//System.out.println("head1:"+head1+"\thead2:"+head2);
		String HToken1 = head1+"_"+(String)this.tokenList.get(head1);
		String HToken2 = head2+"_"+(String)this.tokenList.get(head2);
		//System.out.println("head:"+HToken1+"/"+HToken2);
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
			
			String relation = null;
			if(edge!=null)
				relation = edge.getLabel();
			DefaultMutableTreeNode root = this.createTree(HToken1, HToken2,list, relation, indexNode1, node1);
			if(root ==null)
				return null;
			result = this.getTree(root);
			//System.out.println(result);
		}
		return result;
	}
	
	/**
	 * 1. the nodes are ordered according to the sentence position.
	 * 2. NE is attached to the POS.
	 * 3. lexicon is still there.
	 */
	public DefaultMutableTreeNode createTree(String HToken1, String HToken2, List edges, String rootS, int top, String topS){
		//	System.out.println("list:");
		//only syntactic relation, i.e. edge label, will have child, so the nodesMap only
		//needs to record string vertex-> DefaultMutableTreeNode node relation
		HashMap nodesMap = new HashMap();//<key_String_node, value_DefaultMutableTreeNode>
		String topPOS = (String)this.posList.get(top);
		if(topS.equals(HToken2)||topS.equals(HToken1)){//attach the NE tag.
			topPOS = this.NE + "_"+topPOS;
		}
		NodewithOrder root = new NodewithOrder(topPOS, top);
		nodesMap.put(topS, root);

		String topToken = (String)this.lemmaList.get(top);
		//if(this.stopwords.contains(topToken)){ //add all the lexicon
			NodewithOrder firstW = new NodewithOrder(topToken, top);
			root.add(firstW);
		//}
		
		
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
			NodewithOrder parentNode = (NodewithOrder)parentNodeO;
			
			int endIndex = Integer.parseInt(endS.substring(0,endS.indexOf("_")));
			NodewithOrder childNode = new NodewithOrder(relation, endIndex);
			parentNode.add(childNode);
			nodesMap.put(endS, childNode);
			//then add pos and word
			String pos = (String)this.posList.get(endIndex);
			if(endS.equals(HToken2)||endS.equals(HToken1)){
				pos = this.NE+"_"+pos;
			}
			NodewithOrder posNode = new NodewithOrder(pos, endIndex);
			childNode.add(posNode);
			String word = (String)this.lemmaList.get(endIndex);
			//if(this.stopwords.contains(word)){
				NodewithOrder wNode = new NodewithOrder(word, endIndex);
				posNode.add(wNode);
			//}
		}
		root = SortTree.sort(root);
		return root;

	}

	public static void main(String[] args){
		//String text = "( (S (S (PP (RB Instead) (IN of) (NP (CD 50\\/50) )) (NP-SBJ (PRP it) ) (VP (VBD became) (, ,) (PP-LOC (IN on) (NP (NN paper) ) (ADVP (RB only) )) (, ,) (NP-PRD (NP (NNS two-thirds) (NNP Mariotta) ) (, ,) (NP (NN one-third) (NNP Neuberger) )))) (, ,) (CC and) (S (NP-SBJ (PRP they) ) (VP (VBD were) (PP-PRD (PP (IN in) (NP (DT the) (NN program) )) (CC and) (PP (IN off) (PP (TO to) (NP (DT the) (NNS races) )))))) (. .) ))";
		//String text = "( (S (NP-SBJ (NP (NNS Worksheets) ) (PP-LOC (IN in) (NP (NP (NP (DT a) (JJ test-practice) (NN kit) ) (VP (VBN called) (S (NP-SBJ (-NONE- *) ) (NP-PRD-TTL (NNP Learning) (NNPS Materials) )))) (, ,) (VP (VBN sold) (NP (-NONE- *) ) (PP-DTV (TO to) (NP (NP (NNS schools) ) (ADVP-LOC (IN across) (NP (DT the) (NN country) )))) (PP (IN by) (NP-LGS (NNP Macmillan\\/McGraw-Hill) (NNP School) (NNP Publishing) (NNP Co.) ))) (, ,) ))) (VP (VBP contain) (NP (DT the) (JJ same) (NNS questions) )) (. .) )) ";
		String text = "( (S (NP-SBJ-10 (NP (NNP J.P.) (NNP Bolduc) ) (, ,) (NP (NP (NN vice) (NN chairman) ) (PP (IN of) (NP  (NP (NNP W.R.) (NNP Grace) (CC &) (NNP Co.) ) (, ,)  (SBAR(WHNP-10 (WDT which) )(S(NP-SBJ (-NONE- *T*-10) ) (VP (VBZ holds) (NP(NP (DT a) (ADJP (CD 83.4) (NN %) )  (NN interest) )(PP-LOC (IN in) (NP (DT this) (JJ energy-services) (NN company) )))))))))       (, ,) )     (VP (VBD was)        (VP (VBN elected) (S  (NP-SBJ (-NONE- *-10) ) (NP-PRD (DT a) (NN director) )))) (. .) ))";
		try {
			DP2TBwithLex dpPath2TB = new DP2TBwithLex(text, "stopwords.txt");
			//dpPath2TB.getTreePath(0, 6);
			System.out.println(dpPath2TB.getTreePath(1, 7));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
