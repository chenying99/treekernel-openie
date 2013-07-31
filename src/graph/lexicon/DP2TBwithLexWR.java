package graph.lexicon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.UndirectedGraph;

import relationEx.DPPair;
import relationEx.DPType;
import graph.LabeledEdge;
import graph.relation.DPPath2TBwithR;
import graph.relation.NodewithOrder;
import graph.relation.SortTree;

/**
 * Create the trees which has lexicon for the triple task.
 * 1. the nodes are also ordered according to sentence order.
 * 2. lexicon is preserved.
 * 3. So R tag and NE tag are added to the POS
 * @author ying
 *
 */
public class DP2TBwithLexWR extends DPPath2TBwithR{
	
	public DP2TBwithLexWR(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
	}
	
	public DP2TBwithLexWR(String text, String stopFile) throws IOException{
		super(text, stopFile);
	}
	
	/**
	 * The difference with the DPPath2TBwithR.createTree() is that the child nodes of trees are sorted.
	 */
	public DefaultMutableTreeNode createTree(String HToken1, String HToken2, List edges, String rootS, int top, String topS, HashSet rTokenSet){
		//	System.out.println("list:");
		//only syntactic relation, i.e. edge label, will have child, so the nodesMap only
		//needs to record string vertex-> NodewithOrder node relation
		HashMap nodesMap = new HashMap();//<key_String_node, value_Node>
		String topPOS = (String)this.posList.get(top);
		if(topS.equals(HToken2)||topS.equals(HToken1)){
			topPOS = this.NE + "_"+topPOS;
		}else if(rTokenSet.contains(topS)){
			topPOS = this.R + "_" +topPOS;
		}
		NodewithOrder root = new NodewithOrder(topPOS, top);
		nodesMap.put(topS, root);

		String topToken = (String)this.lemmaList.get(top);
		NodewithOrder firstW = new NodewithOrder(topToken, top);
		root.add(firstW);
		
		
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
			//int parentIndex = parentNode.getSentIndex();
			int endIndex = Integer.parseInt(endS.substring(0,endS.indexOf("_")));
			//String direct = "l_";
			//if(endIndex>parentIndex)
			//	direct = "r_";
			//I don't use direct because if it is ordered, direct wouldn't matter.
			//NodewithOrder childNode = new NodewithOrder(direct+relation, endIndex);
			NodewithOrder childNode = new NodewithOrder(relation, endIndex);
			
			parentNode.add(childNode);
			nodesMap.put(endS, childNode);
			
			//then add pos and word
			String pos = (String)this.posList.get(endIndex);
			if(endS.equals(HToken2)||endS.equals(HToken1)){
				pos = this.NE +"_"+pos;
			}else if(rTokenSet.contains(endS)){
				pos = this.R +"_" +pos;
			}
			NodewithOrder posNode = new NodewithOrder(pos, endIndex);
			childNode.add(posNode);
			String word = (String)this.lemmaList.get(endIndex);
			NodewithOrder wNode = new NodewithOrder(word, endIndex);
			posNode.add(wNode);
		}
		//System.out.println();
		//System.out.println("before sort:");
		//DPPath2TBwithRordered.outputTree(root);
		//System.out.println();
		root = SortTree.sort(root);
		//System.out.println("after sort:");
		//DPPath2TBwithRordered.outputTree(root);
		//System.out.println();
		return root;

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
