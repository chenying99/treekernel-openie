package graph.relation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;

import relationEx.DPPair;
import relationEx.DPType;
import relationEx.DPathUtil;
import relationEx.DependEx;
import graph.DPPath2TB;
import graph.LabeledEdge;

/**
 * Extract flat features. 
 * @author ying
 *
 */
public class FeatureExR extends DPPath2TB{
	
	HashMap features = new HashMap();
	HashMap dpPairSet  = new HashMap();
	int distER1 = -1;
	int distER2 = -1;
	public FeatureExR(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
		dpPairSet = DPathUtil.getDPMap(dpPairList, dpTypeList);
	}
	
	public FeatureExR(String text, String stopFile) throws IOException{
		super(text, stopFile);
		dpPairSet = DPathUtil.getDPMap(dpPairList, dpTypeList);
	}
	
	public HashMap getFeatures(){
		return features;
	}
	
	public int getDistER1(){
		return this.distER1;
	}
	
	public int getDistER2(){
		return this.distER2;
	}
	
	public void extractFeature(ArrayList tokenList, ArrayList posList,int head1, int head2, int[] pairOffset, int[] relationOffsets, int[] ptType){
		features.clear();
		this.distER1 = -1;
		this.distER2 = -1;
		this.extractE2E(pairOffset);
		this.extractRF(tokenList, posList, relationOffsets, ptType);
		this.extractER(head1, head2, relationOffsets);
	}
	
	public void extractRF(ArrayList tokenList,ArrayList posList, int[] relationOffsets, int[] ptTypes){
		StringBuffer typeBigram = new StringBuffer();
		for(int i=0;i<3;i++){
			if(i==1 && ptTypes[i]==-1 && relationOffsets[i*2]!=-1 && relationOffsets[i*2]==relationOffsets[i*2+1]){
				String posTemp = (String)posList.get(relationOffsets[i*2]);
				if(posTemp.equals("POS")){
					features.put("R1_POS", "1");
				}
			}
			if(ptTypes[i]!=-1){
				typeBigram.append("RType").append(i).append(" ").append(ptTypes[i]).append(" ");
			}
		}
		if(typeBigram.toString().trim().length()>0)
			features.put(typeBigram.toString().trim(), "1");
		else
			features.put("RType_null", "1");
		
		//dependency between R if there are two parts
		for(int i=0;i<2;i++){
			if(ptTypes[i]!=-1){
				if(ptTypes[i+1]!=-1){
					DPType R1R2 = DependEx.getDpType(dpPairSet, relationOffsets[2*i], relationOffsets[2*i+1], relationOffsets[2*i+2], relationOffsets[2*i+3]);
					if(R1R2==null){
						features.put("RLink_null", "1");
					}
				}
			}
		}
	}
	
	public void extractE2E(int[] pairOffset){
		DPType E1E2 = DependEx.getDpType(dpPairSet, pairOffset[0], pairOffset[1], pairOffset[2], pairOffset[3]);
		if(E1E2!=null){
			features.put("E12E2_"+E1E2.getType(),"1");
		}else{
			features.put("E12E2_null","1");
		}
		//String posLast1 = (String)this.posList.get(pairOffset[1]);
		//String posLast2 = (String)this.posList.get(pairOffset[2]);
		//features.put("E1POS_"+posLast1, "1");
		//features.put("E2POS_"+posLast2, "1");
	}
	
	public void extractER( int head1, int head2, int[] relationOffsets){
		String HToken1 = head1+"_"+(String)this.tokenList.get(head1);
		String HToken2 = head2+"_"+(String)this.tokenList.get(head2);
		
		this.extractERSub(1, relationOffsets, HToken1);
		this.extractERSub(2, relationOffsets, HToken2);
		
	}
	
	public void extractERSub(int Ei,int[] relationOffsets, String HTokeni ){
		List shortestListE1R = null;
		int E1R = -1;
		HashSet E1Rset = null;
		for(int i=0;i<3;i++){
			if(relationOffsets[i*2]!=-1){
				HashSet rTokenSet = getRelationToken(relationOffsets[i*2],relationOffsets[i*2+1]);
				List tempList1 = DPPath2TBwithR.findSP(graph, HTokeni, rTokenSet);
				if(shortestListE1R==null || (tempList1!=null && shortestListE1R.size()>tempList1.size())){
					shortestListE1R = tempList1;
					E1R = i;
					E1Rset = rTokenSet;
				}
			}
		}
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
			if(shortestListE1R.size()==1){
				String posLast = (String)posList.get(relationOffsets[2*E1R+1]);
				String tokenLast = (String)tokenList.get(relationOffsets[2*E1R+1]);
				if(!posLast.equals("IN") && !posLast.equals("TO") && !posLast.equals("RP")){
					tokenLast = "NONIN";
				}
				LabeledEdge edge = (LabeledEdge)shortestListE1R.get(0);
				String edgeName = edge.getLabel();
				if(edgeName.indexOf("prep")>=0 || posLast.indexOf("RP")>=0|| posLast.indexOf("IN")>=0|| posLast.indexOf("TO")>=0){
					if(Ei==1 && E1R==0)
						features.put("E"+Ei+"R"+E1R+"edge_"+edgeName+" RPOS_"+tokenLast, "1");
					if(Ei==2 && E1R!=0)
						features.put("E"+Ei+"R"+E1R+"edge_"+edgeName+" RPOS_"+tokenLast, "1");
					//now add the match explicitly
					boolean checkPrep=false;
					if((Ei==1 && E1R==0)||(Ei==2 && E1R!=0)){
						if(!checkPrepSub(tokenLast, edgeName)){
							features.put("E"+Ei+"R"+E1R+"edge_prep_not_match", "1");
						}
					}
				}
				
			}else if(shortestListE1R.size()==2){
				for(int i=0;i<shortestListE1R.size();i++){
					LabeledEdge edge = (LabeledEdge)shortestListE1R.get(i);
					String edgeName = edge.getLabel();
					if(edgeName.indexOf("conj")==0){
						String V1 = (String)edge.getV1();
						String V2 = (String)edge.getV2();
						if(V1.equals(HTokeni)|| V2.equals(HTokeni)){
							features.put("E"+Ei+"R"+E1R+"edge_conj_E"+Ei, "1");
						}else{
							features.put("E"+Ei+"R"+E1R+"edge_conj_R", "1");
						}
					}
					
					if(edgeName.indexOf("appos")==0){
						String V1 = (String)edge.getV1();
						String V2 = (String)edge.getV2();
						if(V1.equals(HTokeni)|| V2.equals(HTokeni)){
							features.put("E"+Ei+"R"+E1R+"Edge_appos", "1");
						}
					}
				}
			}
		}
	}
	
	public HashSet getRelationToken( int start1, int end1){
		HashSet tokenSet = new HashSet();
		//System.out.println("relation node set: ");
		for(int i=start1;i<=end1;i++){
			tokenSet.add(i+"_"+(String)this.tokenList.get(i));
			//System.out.println(i+"_"+(String)this.tokenList.get(i));
		}
		return tokenSet;
		
	}
	
	/**
	 * If the dp relation has prep_*, and relation type is vb, then * need to be in the relation word
	 * @param tokenList
	 * @param type
	 * @param startR
	 * @param endR
	 * @return
	 */
	public static boolean checkPrepSub(String pos, String type){
		int index = type.lastIndexOf("_");
		String posShould = type.substring(index+1);//in fact, it is prep, such as in, at
		if(pos.equalsIgnoreCase(posShould)){
			return true;
		}else{
			return false;
		}
	}

}
