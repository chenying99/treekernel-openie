package graph;

import graph.relation.DPPath2TBwithRordered;
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
 * I want to order the children of a node according to the sentence order.
 * @author ying
 *
 */
public class DPPath2TBwithNEordered extends DPPath2TBwithNE{
	
	public DPPath2TBwithNEordered(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
		// TODO Auto-generated constructor stub
	}
	
	public DPPath2TBwithNEordered(String text, String stopFile) throws IOException{
		super(text, stopFile);
	}
	
	/**
	 * The difference with the DPPath2TBwithNE.createTree() is that children are ordered
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
			NodewithOrder firstW = new NodewithOrder(this.NE, top);
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
				NodewithOrder wNode = new NodewithOrder(this.NE, endIndex);
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

	public static void main(String[] args){
		//String text = "( (S (S (PP (RB Instead) (IN of) (NP (CD 50\\/50) )) (NP-SBJ (PRP it) ) (VP (VBD became) (, ,) (PP-LOC (IN on) (NP (NN paper) ) (ADVP (RB only) )) (, ,) (NP-PRD (NP (NNS two-thirds) (NNP Mariotta) ) (, ,) (NP (NN one-third) (NNP Neuberger) )))) (, ,) (CC and) (S (NP-SBJ (PRP they) ) (VP (VBD were) (PP-PRD (PP (IN in) (NP (DT the) (NN program) )) (CC and) (PP (IN off) (PP (TO to) (NP (DT the) (NNS races) )))))) (. .) ))";
		//String text = "( (S (NP-SBJ (NP (NNS Worksheets) ) (PP-LOC (IN in) (NP (NP (NP (DT a) (JJ test-practice) (NN kit) ) (VP (VBN called) (S (NP-SBJ (-NONE- *) ) (NP-PRD-TTL (NNP Learning) (NNPS Materials) )))) (, ,) (VP (VBN sold) (NP (-NONE- *) ) (PP-DTV (TO to) (NP (NP (NNS schools) ) (ADVP-LOC (IN across) (NP (DT the) (NN country) )))) (PP (IN by) (NP-LGS (NNP Macmillan\\/McGraw-Hill) (NNP School) (NNP Publishing) (NNP Co.) ))) (, ,) ))) (VP (VBP contain) (NP (DT the) (JJ same) (NNS questions) )) (. .) )) ";
		//String text = "( (S (NP-SBJ-10 (NP (NNP J.P.) (NNP Bolduc) ) (, ,) (NP (NP (NN vice) (NN chairman) ) (PP (IN of) (NP  (NP (NNP W.R.) (NNP Grace) (CC &) (NNP Co.) ) (, ,)  (SBAR(WHNP-10 (WDT which) )(S(NP-SBJ (-NONE- *T*-10) ) (VP (VBZ holds) (NP(NP (DT a) (ADJP (CD 83.4) (NN %) )  (NN interest) )(PP-LOC (IN in) (NP (DT this) (JJ energy-services) (NN company) )))))))))       (, ,) )     (VP (VBD was)        (VP (VBN elected) (S  (NP-SBJ (-NONE- *-10) ) (NP-PRD (DT a) (NN director) )))) (. .) ))";
		String text = "(ROOT (S (NP (NP (NNP T.) (NNP Marshall) (NNP Hahn) (NNP Jr.)) (, ,) (NP (NP (NNP Georgia-Pacific) (POS 's)) (NX (NX (NN chairman)) (CC and) (NX (JJ chief) (NN executive)))) (, ,)) (VP (VBD said) (PP (IN in) (NP (DT an) (NN interview))) (SBAR (IN that) (S (NP (NP (DT all) (NNS terms)) (PP (IN of) (NP (DT the) (NN offer)))) (VP (VBP are) (ADJP (JJ negotiable)))))) (. .)))";
		//StanfordParser parser = new StanfordParser();
		//String text = "The two-year note 's yield was unchanged at 5.95 percent .";
		try {
			DPPath2TBwithNEordered dpPath2TB = new DPPath2TBwithNEordered(text, "stopwords.txt");
			/*ArrayList<DPPair> dpPairList = new ArrayList();
			ArrayList<DPType> dpTypeList= new ArrayList();
			ArrayList tokenList = new ArrayList();
			ArrayList posList = new ArrayList();
			UndirectedGraph<String, LabeledEdge> graph  = new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);
			parser.parseLabeledEdge(text, tokenList, posList, dpPairList, dpTypeList, graph);
			DPPath2TBwithNE dpPath2TB = new DPPath2TBwithNE(dpPairList,
					dpTypeList, tokenList,
					posList,  graph,
					"stopwords.txt");
			HashMap govListMap = DpLinkTranEx.getGovList(dpPairList);*/
			//int head1 = DpLinkTranEx.getHead(govListMap, tokenList, posList,26, 31);
			//System.out.println(head1);
			
			//String result = dpPath2TB.getTreePath(1, 7);
			String result = dpPath2TB.getTreePath(3, 5);
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
