package graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.UndirectedGraph;

import relationEx.DPPair;
import relationEx.DPType;

/**
 * after analyze the result of DPPath2TB, I found that one reason of errors might be that the
 *  head of the first node is not related to the relation judge.
 *  So here I will delete that node.
 * @author ying
 *
 */
public class DPPath2TBNoHead extends DPPath2TB{

	public DPPath2TBNoHead(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
		// TODO Auto-generated constructor stub
	}
	
	public DPPath2TBNoHead(String text, String stopFile) throws IOException{
		super(text, stopFile);
	}
	
	/**
	 * The difference with the super.createTree() is that no rootS is added as the root.
	 */
	public DefaultMutableTreeNode createTree(List edges, String rootS, int top, String topS){
		//	System.out.println("list:");
		//only syntactic relation, i.e. edge label, will have child, so the nodesMap only
		//needs to record string vertex-> DefaultMutableTreeNode node relation
		HashMap nodesMap = new HashMap();//<key_String_node, value_DefaultMutableTreeNode>
		String topPOS = (String)this.posList.get(top);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(topPOS);
		nodesMap.put(topS, root);

		String topToken = (String)this.lemmaList.get(top);
		if(this.stopwords.contains(topToken)){
			DefaultMutableTreeNode firstW = new DefaultMutableTreeNode(topToken);
			root.add(firstW);
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

	public static void main(String[] args){
		//String text = "( (S (S (PP (RB Instead) (IN of) (NP (CD 50\\/50) )) (NP-SBJ (PRP it) ) (VP (VBD became) (, ,) (PP-LOC (IN on) (NP (NN paper) ) (ADVP (RB only) )) (, ,) (NP-PRD (NP (NNS two-thirds) (NNP Mariotta) ) (, ,) (NP (NN one-third) (NNP Neuberger) )))) (, ,) (CC and) (S (NP-SBJ (PRP they) ) (VP (VBD were) (PP-PRD (PP (IN in) (NP (DT the) (NN program) )) (CC and) (PP (IN off) (PP (TO to) (NP (DT the) (NNS races) )))))) (. .) ))";
		//String text = "( (S (NP-SBJ (NP (NNS Worksheets) ) (PP-LOC (IN in) (NP (NP (NP (DT a) (JJ test-practice) (NN kit) ) (VP (VBN called) (S (NP-SBJ (-NONE- *) ) (NP-PRD-TTL (NNP Learning) (NNPS Materials) )))) (, ,) (VP (VBN sold) (NP (-NONE- *) ) (PP-DTV (TO to) (NP (NP (NNS schools) ) (ADVP-LOC (IN across) (NP (DT the) (NN country) )))) (PP (IN by) (NP-LGS (NNP Macmillan\\/McGraw-Hill) (NNP School) (NNP Publishing) (NNP Co.) ))) (, ,) ))) (VP (VBP contain) (NP (DT the) (JJ same) (NNS questions) )) (. .) )) ";
		String text = "( (S (NP-SBJ-10 (NP (NNP J.P.) (NNP Bolduc) ) (, ,) (NP (NP (NN vice) (NN chairman) ) (PP (IN of) (NP  (NP (NNP W.R.) (NNP Grace) (CC &) (NNP Co.) ) (, ,)  (SBAR(WHNP-10 (WDT which) )(S(NP-SBJ (-NONE- *T*-10) ) (VP (VBZ holds) (NP(NP (DT a) (ADJP (CD 83.4) (NN %) )  (NN interest) )(PP-LOC (IN in) (NP (DT this) (JJ energy-services) (NN company) )))))))))       (, ,) )     (VP (VBD was)        (VP (VBN elected) (S  (NP-SBJ (-NONE- *-10) ) (NP-PRD (DT a) (NN director) )))) (. .) ))";
		try {
			DPPath2TBNoHead dpPath2TB = new DPPath2TBNoHead(text, "stopwords.txt");
			//dpPath2TB.getTreePath(0, 6);
			String result = dpPath2TB.getTreePath(1, 6);
			System.out.println(result);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
