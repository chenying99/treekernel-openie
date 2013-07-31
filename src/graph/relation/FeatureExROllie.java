package graph.relation;

import graph.LabeledEdge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jgrapht.UndirectedGraph;

import relationEx.DPPair;
import relationEx.DPType;
import relationEx.DPathUtil;
import relationEx.DependEx;

/**
 * * Comparing with FeatureExR, this one is for ollie. So it doesn't need features for R links.
 * 
 * @author ying
 *
 */
public class FeatureExROllie extends FeatureExR{
	
	public FeatureExROllie(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
	}
	
	public FeatureExROllie(String text, String stopFile) throws IOException{
		super(text, stopFile);
	}
	
	public void extractFeature(ArrayList tokenList, ArrayList posList,int head1, int head2, ArrayList offsetE1, ArrayList offsetE2, ArrayList rOffsets){
		features.clear();
		this.distER1 = -1;
		this.distER2 = -1;
		String HToken1 = head1+"_"+(String)this.tokenList.get(head1);
		String HToken2 = head2+"_"+(String)this.tokenList.get(head2);
		
		this.extractE2E(HToken1, HToken2, offsetE1,  offsetE2);
		this.extractRF(posList, rOffsets);
		this.extractER(HToken1, HToken2, rOffsets);
	}
	
	public void extractE2E(String HToken1, String HToken2, ArrayList offsetE1, ArrayList offsetE2){
		HashSet e1TokenSet =this.getGraphNodeForm(offsetE1);
		HashSet e2TokenSet = this.getGraphNodeForm(offsetE2);
		List tempList1 = DPPath2TBwithR.findSP(graph, HToken1, e2TokenSet);
		List tempList2 = DPPath2TBwithR.findSP(graph, HToken2, e1TokenSet);
		if(tempList1!=null && tempList1.size()==1){
			LabeledEdge edge = (LabeledEdge)tempList1.get(0);
			String edgeName = edge.getLabel();
			features.put("E12E2_"+edgeName,"1");
		}else if(tempList2!=null && tempList2.size()==1){
			LabeledEdge edge = (LabeledEdge)tempList2.get(0);
			String edgeName = edge.getLabel();
			features.put("E12E2_"+edgeName,"1");
		}else{
			features.put("E12E2_null","1");
		}
		
	}
	
	public void extractRF(ArrayList posList, ArrayList rOffsets){
		String posFirst = (String)posList.get((Integer)rOffsets.get(0));
		if(posFirst.equals("(IN")||posFirst.equals("RP")||posFirst.equals("TO")){
			features.put("RType_PREP", "1");
			return;
		}
		HashMap govListMap = DPathUtil.getGovList(dpPairList);
		int head1 = DPathUtil.getHeadOllie(govListMap, tokenList, posList, rOffsets);
		if(head1>=0){
			String posHead = (String)posList.get(head1);
			System.out.println("head:"+posHead+"_"+(String)this.tokenList.get(head1));
			if(posHead.startsWith("VB")){
				features.put("RType_VB", "1");
				return;
			}else if(posHead.startsWith("NN")){
				features.put("RType_NN", "1");
				return;
			}else{
				features.put("RType_"+posHead, "1");
				return;
			}
		}else{
			features.put("RType_Other", "1");
			return;
		}
	}
	
	public void extractER( String HToken1, String HToken2, ArrayList relationOffsets){
		
		this.extractERSub(1, relationOffsets, HToken1);
		this.extractERSub(2, relationOffsets, HToken2);
		
	}
	
	public void extractERSub(int Ei,ArrayList rOffsets, String HTokeni ){
		System.out.println("extractER:"+HTokeni);
		List shortestListE1R = null;
		HashSet E1Rset = null;
		HashSet rTokenSet =this.getGraphNodeForm(rOffsets);
		List tempList1 = DPPath2TBwithR.findSP(graph, HTokeni, rTokenSet);
		shortestListE1R = tempList1;
		
		E1Rset = rTokenSet;
		if(shortestListE1R==null){
			features.put("E"+Ei+"R_null", "1");
		}else{
			int dist = shortestListE1R.size();
			if(dist==1){
				//check if it is conj
				LabeledEdge edge = (LabeledEdge)shortestListE1R.get(0);
				String edgeName = edge.getLabel();
				if(edgeName.startsWith("conj")){
					features.put("E"+Ei+"R_null", "1");
					return;
				}
			}
			if(Ei==1)
				this.distER1 = dist;
			else
				this.distER2 = dist;
			
			DPPath2TBwithR.outEdgeList(shortestListE1R);
			if(dist>4)
				dist=5;
			features.put("E"+Ei+"R_"+dist, "1");
			if(shortestListE1R.size()==2){
				for(int i=0;i<shortestListE1R.size();i++){
					LabeledEdge edge = (LabeledEdge)shortestListE1R.get(i);
					String edgeName = edge.getLabel();
					if(edgeName.indexOf("conj")==0){
						String V1 = (String)edge.getV1();
						String V2 = (String)edge.getV2();
						if(V1.equals(HTokeni)|| V2.equals(HTokeni)){
							features.put("E"+Ei+"R"+"edge_conj_E"+Ei, "1");
						}else{
							features.put("E"+Ei+"R"+"edge_conj_R", "1");
						}
					}
					
					if(edgeName.indexOf("appos")==0){
						String V1 = (String)edge.getV1();
						String V2 = (String)edge.getV2();
						if(V1.equals(HTokeni)|| V2.equals(HTokeni)){
							features.put("E"+Ei+"R"+"Edge_appos", "1");
						}
					}
				}
			}
		}
	}
	
	/**
	 * The same as the one in DPPath2TBwithR.java
	 * @param rOffsets
	 * @return
	 */
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
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
