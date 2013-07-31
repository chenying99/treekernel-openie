package graph;

import graph.relation.NodewithOrder;
import graph.relation.SortTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.UndirectedGraph;

import relationEx.DPPair;
import relationEx.DPType;

/**
 * DPPath2TB for traditional task. 
 * My plan for traditional task: 1. use my binary open IE to get possitive NEs.
 * 2. use more specific relation path, tagging NE-types, tagging words, for relation classification.
 * So this class will extand DPPath2TBwithNEordered, add words and NE-types.
 * @author ying
 *
 */
public class DPPath2TBTrad extends DPPath2TBwithNE{
	String e1Type = null;
	String e2Type = null;
	
	public DPPath2TBTrad(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
		// TODO Auto-generated constructor stub
	}
	
	public DPPath2TBTrad(String text, String stopFile) throws IOException{
		super(text, stopFile);
	}
	
	public String getTreePath(int head1, int head2, String e1Type, String e2Type){
		this.e1Type = e1Type;
		this.e2Type = e2Type;
		return this.getTreePath(head1, head2);
	}
	
	/**
	 * The difference with DPPath2TBwithNEordered is add words and NE-types.
	 */
	public DefaultMutableTreeNode createTree(String HToken1, String HToken2, List edges, String rootS, int top, String topS){
		//	System.out.println("list:");
		//only syntactic relation, i.e. edge label, will have child, so the nodesMap only
		//needs to record string vertex-> DefaultMutableTreeNode node relation
		HashMap nodesMap = new HashMap();//<key_String_node, value_DefaultMutableTreeNode>
		String topPOS = (String)this.posList.get(top);
		NodewithOrder root = new NodewithOrder(topPOS, top);
		nodesMap.put(topS, root);

		String topToken = (String)this.lemmaList.get(top);
		if(topS.equals(HToken2)||topS.equals(HToken1)){
			NodewithOrder firstW =null;
			if(topS.equals(HToken2))
				firstW = new NodewithOrder(this.e2Type, top);
			else
				firstW =  new NodewithOrder(this.e1Type, top);
			root.add(firstW);
		}else{
			NodewithOrder firstW = new NodewithOrder(topToken, top);
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
			NodewithOrder parentNode = (NodewithOrder)parentNodeO;
			
			int endIndex = Integer.parseInt(endS.substring(0,endS.indexOf("_")));
			NodewithOrder childNode = new NodewithOrder(relation, endIndex);
			parentNode.add(childNode);
			nodesMap.put(endS, childNode);
			//then add pos and word
			String pos = (String)this.posList.get(endIndex);
			NodewithOrder posNode = new NodewithOrder(pos, endIndex);
			childNode.add(posNode);
			String word = (String)this.lemmaList.get(endIndex);
			
			if(endS.equals(HToken2)||endS.equals(HToken1)){
				
				NodewithOrder wNode =null;
				if(endS.equals(HToken2))
					wNode = new NodewithOrder(this.e2Type, top);
				else
					wNode =  new NodewithOrder(this.e1Type, top);
				posNode.add(wNode);
			}else{
				NodewithOrder wNode = new NodewithOrder(word, endIndex);
				posNode.add(wNode);
			}
		}
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
		String text = "(ROOT (S (NP (NP (NNP T.) (NNP Marshall) (NNP Hahn) (NNP Jr.)) (, ,) (NP (NP (NNP Georgia-Pacific) (POS 's)) (NX (NX (NN chairman)) (CC and) (NX (JJ chief) (NN executive)))) (, ,)) (VP (VBD said) (PP (IN in) (NP (DT an) (NN interview))) (SBAR (IN that) (S (NP (NP (DT all) (NNS terms)) (PP (IN of) (NP (DT the) (NN offer)))) (VP (VBP are) (ADJP (JJ negotiable)))))) (. .)))";
		try {
			DPPath2TBTrad dpPath2TB = new DPPath2TBTrad(text, null);
			String result = dpPath2TB.getTreePath(3, 5,"PER","ORG");
			if(result!=null){
				System.out.println(result);
			}else{
				System.out.println("resutl==null");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
