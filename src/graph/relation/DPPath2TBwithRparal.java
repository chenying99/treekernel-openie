package graph.relation;

import graph.LabeledEdge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import parseFilter.trainEx.StanfordParser;
import relationEx.DPPair;
import relationEx.DPType;
import relationEx.DPathUtil;

/**
 * Get hint from "Tree Kernel Engineering for Proposition Re-ranking", Moschitt 2006.
 * Put R in parallel with POS, make it POS-R
 * better than DPPath2TBwithRtop but worse than withR.
 * @author ying
 *
 */
public class DPPath2TBwithRparal extends DPPath2TBwithR{
	public DPPath2TBwithRparal(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
	}
	
	public DPPath2TBwithRparal(String text, String stopFile) throws IOException{
		super(text, stopFile);
	}
	
	
	/**
	 * The difference with the DPPath2TBNoHead.createTree() is that NE is added as word into the tree.
	 */
	public DefaultMutableTreeNode createTree(String HToken1, String HToken2, List edges, String rootS, int top, String topS, HashSet rTokenSet){
		//	System.out.println("list:");
		//only syntactic relation, i.e. edge label, will have child, so the nodesMap only
		//needs to record string vertex-> DefaultMutableTreeNode node relation
		HashMap nodesMap = new HashMap();//<key_String_node, value_DefaultMutableTreeNode>
		
		DefaultMutableTreeNode root=null;
		String topToken = (String)this.lemmaList.get(top);
		if(topS.equals(HToken2)||topS.equals(HToken1)){
			String topPOS = (String)this.posList.get(top);
			 root = new DefaultMutableTreeNode(topPOS);
			nodesMap.put(topS, root);
			DefaultMutableTreeNode firstW = new DefaultMutableTreeNode(this.NE);
			root.add(firstW);
		}else if(rTokenSet.contains(topS)){
			String topPOS = (String)this.posList.get(top);
			 root = new DefaultMutableTreeNode(this.R+"-"+topPOS);
			nodesMap.put(topS, root);
		}else if(this.stopwords.contains(topToken)){
			String topPOS = (String)this.posList.get(top);
			 root = new DefaultMutableTreeNode(topPOS);
			nodesMap.put(topS, root);
			DefaultMutableTreeNode firstW = new DefaultMutableTreeNode(topToken);
			root.add(firstW);
		}else{
			String topPOS = (String)this.posList.get(top);
			 root = new DefaultMutableTreeNode(topPOS);
			nodesMap.put(topS, root);
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
			
			String word = (String)this.lemmaList.get(endIndex);
			if(endS.equals(HToken2)||endS.equals(HToken1)){
				DefaultMutableTreeNode posNode = new DefaultMutableTreeNode(pos);
				childNode.add(posNode);
				DefaultMutableTreeNode wNode = new DefaultMutableTreeNode(this.NE);
				posNode.add(wNode);
			}else if(rTokenSet.contains(endS)){
				DefaultMutableTreeNode posNode = new DefaultMutableTreeNode(this.R+"-"+pos);
				childNode.add(posNode);
			}else if(this.stopwords.contains(word)){
				DefaultMutableTreeNode posNode = new DefaultMutableTreeNode(pos);
				childNode.add(posNode);
				DefaultMutableTreeNode wNode = new DefaultMutableTreeNode(word);
				posNode.add(wNode);
			}
		}
		//System.out.println();
		return root;

	}
	
	public static void testTBSent() throws IOException{
		String text = "( (S (NP-SBJ-10 (NP (NNP J.P.) (NNP Bolduc) ) (, ,) (NP (NP (NN vice) (NN chairman) ) (PP (IN of) (NP  (NP (NNP W.R.) (NNP Grace) (CC &) (NNP Co.) ) (, ,)  (SBAR(WHNP-10 (WDT which) )(S(NP-SBJ (-NONE- *T*-10) ) (VP (VBZ holds) (NP(NP (DT a) (ADJP (CD 83.4) (NN %) )  (NN interest) )(PP-LOC (IN in) (NP (DT this) (JJ energy-services) (NN company) )))))))))       (, ,) )     (VP (VBD was)        (VP (VBN elected) (S  (NP-SBJ (-NONE- *-10) ) (NP-PRD (DT a) (NN director) )))) (. .) ))";
		DPPath2TBwithRparal dpPath2TB = new DPPath2TBwithRparal(text, "stopwords.txt");
		
		String result = dpPath2TB.getTreePath(1, 7,3,5);
		if(result!=null){
			System.out.println(result);
		}else{
			System.out.println("resutl==null");
		}
	}
	
	public static void testRawSent() throws IOException{
		StanfordParser parser = new StanfordParser();
		String text = "This extension point allows tools to handle the presentation aspects of a debug model .";
		ArrayList<DPPair> dpPairList = new ArrayList();
		ArrayList<DPType> dpTypeList= new ArrayList();
		ArrayList tokenList = new ArrayList();
		ArrayList posList = new ArrayList();
		UndirectedGraph<String, LabeledEdge> graph  = new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);
		parser.parseLabeledEdge(text, tokenList, posList, dpPairList, dpTypeList, graph);
		DPPath2TBwithRparal dpPath2TB = new DPPath2TBwithRparal(dpPairList,
				dpTypeList, tokenList,
				posList,  graph,
				"stopwords.txt");
		HashMap govListMap = DPathUtil.getGovList(dpPairList);
		int head1 = DPathUtil.getHeadReverb(govListMap, tokenList, posList,0, 2);
		int head2 = DPathUtil.getHeadReverb(govListMap, tokenList, posList,4, 4);
		//System.out.println(head1);
		String result = dpPath2TB.getTreePath(head1, head2,3,3);
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
