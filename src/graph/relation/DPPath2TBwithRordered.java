package graph.relation;

import graph.DPPath2TB;
import graph.LabeledEdge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.UndirectedGraph;

import relationEx.DPPair;
import relationEx.DPType;

/**
 * Want to add more information into the tree kernel besides the function in DPPath2TBwithR.
 * I want to sort the child nodes, so that they will be in the order as in the sentence.
 * The pos of the parents will also be sorted with the child of the parents, so we know the position 
 * of parent nodes with child nodes.
 * @author ying
 *
 */
public class DPPath2TBwithRordered extends DPPath2TBwithR{

	public DPPath2TBwithRordered(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
	}
	
	public DPPath2TBwithRordered(String text, String stopFile) throws IOException{
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
		NodewithOrder root = new NodewithOrder(topPOS, top);
		nodesMap.put(topS, root);

		String topToken = (String)this.lemmaList.get(top);
		if(topS.equals(HToken2)||topS.equals(HToken1)){
			NodewithOrder firstW = new NodewithOrder(this.NE, top);
			root.add(firstW);
		}else if(rTokenSet.contains(topS)){
			NodewithOrder firstW = new NodewithOrder(this.R, top);
			root.add(firstW);
		}else if(this.stopwords.contains(topToken)){
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
			NodewithOrder posNode = new NodewithOrder(pos, endIndex);
			childNode.add(posNode);
			String word = (String)this.lemmaList.get(endIndex);
			if(endS.equals(HToken2)||endS.equals(HToken1)){
				NodewithOrder wNode = new NodewithOrder(this.NE, endIndex);
				posNode.add(wNode);
			}else if(rTokenSet.contains(endS)){
				NodewithOrder wNode = new NodewithOrder(this.R, endIndex);
				posNode.add(wNode);
			}else if(this.stopwords.contains(word)){
				NodewithOrder wNode = new NodewithOrder(word, endIndex);
				posNode.add(wNode);
			}
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
	
	public static void outputTree(NodewithOrder parent){
		System.out.print("(");
		System.out.print(parent.toString()+"_"+parent.getSentIndex());
		System.out.print(" ");
		int childSize=parent.getChildCount();
		if(childSize>0){
			for(int i=0;i<childSize;i++){
				NodewithOrder child = (NodewithOrder)parent.getChildAt(i);
				DPPath2TBwithRordered.outputTree(child);
			}
		}
		System.out.print(")");
	}
	
	public static void testTBSent() throws IOException{
		String text = "(ROOT (S (PP (IN In) (NP (NN contrast))) (, ,) (NP (NP (NNS amphibians)) (, ,) (NP (NNS lizards)) (, ,) (NP (NNS snakes)) (, ,) (NP (NNS turtles)) (, ,) (NP (JJ many) (NNS fishes)) (, ,) (CC and) (NP (JJS most) (NNS invertebrates))) (VP (VBP are) (ADVP (RB mainly)) (ADJP (JJ ectothermic)) (, ,) (S (VP (VBG meaning) (SBAR (IN that) (S (NP (PRP they)) (VP (VB gain) (NP (NP (JJS most)) (PP (IN of) (NP (PRP$ their) (NN heat)))) (PP (IN from) (NP (JJ external) (NNS sources))))))))) (. .)))";
		DPPath2TBwithRordered dpPath2TB = new DPPath2TBwithRordered(text, "stopwords.txt");
		
		String result = dpPath2TB.getTreePath(1, 7,3,5);
		if(result!=null){
			System.out.println(result);
		}else{
			System.out.println("resutl==null");
		}
		
	}

	public static void main(String[] args){
		
		try {
			testTBSent();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
