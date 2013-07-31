package graph.relation;

import graph.LabeledEdge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.jgrapht.UndirectedGraph;

import relationEx.DPPair;
import relationEx.DPType;
import relationEx.DPathUtil;
import utilYing.UtilString;

/**
 * 
 * Comparing with FeatureExR, this one is for reverb. So it doesn't need features for R links.
 * It need features for multi-argument and incomplete or extra words in relations and entities.
 * @author ying
 *
 */
public class FeatureExReverb extends FeatureExR{
	HashMap govListMap = null;
	HashMap targetListMap = null;
	public FeatureExReverb(ArrayList<DPPair> dpPairList,
			ArrayList<DPType> dpTypeList, ArrayList tokenList,
			ArrayList posList, UndirectedGraph<String, LabeledEdge> graph,
			String stopFile) throws IOException {
		super(dpPairList, dpTypeList, tokenList, posList, graph, stopFile);
		this.govListMap = DPathUtil.getGovList(dpPairList);
		this.targetListMap = FeatureExReverb.getTargetList(dpPairList);
	}
	
	public FeatureExReverb(String text, String stopFile) throws IOException{
		super(text, stopFile);
		this.govListMap = DPathUtil.getGovList(dpPairList);
		this.targetListMap = FeatureExReverb.getTargetList(dpPairList);
	}
	
	public void extractFeature(ArrayList tokenList, ArrayList posList,int head1, int head2, int[] pairOffset, int[] relationOffsets, int[] ptType){
		features.clear();
		this.distER1 = -1;
		this.distER2 = -1;
		this.extractEF(head1,head2, pairOffset,relationOffsets[2], relationOffsets[3]);
		this.extractE2E(pairOffset);
		this.extractRF(posList, pairOffset, relationOffsets[2], relationOffsets[3]);
		this.extractER(head1, head2, relationOffsets);
	}
	
	/**
	 * Extract extra link for R.
	 * @param pairOffsets
	 * @param startR
	 * @param endR
	 */
	public void extractRF(ArrayList posList, int[] pairOffsets, int startR, int endR){
		int head = getHeadVerb(posList, startR, endR);
		if(head==-1){
			this.features.put("R has no head", "1");
			return;
		}
		
		for(int i=0;i<dpPairList.size();i++){
			DPPair pair = (DPPair)dpPairList.get(i);
			//System.out.println(pair.getStartIndex()+"_"+pair.getStartToken()+"->"+pair.getEndIndex()+"_"+pair.getEndToken());
			int gov = pair.getStartIndex();
			int target = pair.getEndIndex();
			if(gov==head && target>gov){
				if((target<startR || target>endR) && 
						(target<pairOffsets[0]||target>pairOffsets[1])&&
						(target<pairOffsets[2]|| target>pairOffsets[3])){//not a relation, nor NEs
					DPType type = (DPType)this.dpTypeList.get(i);
					String typeS = type.getType();
					if(typeS.indexOf("conj")<0){
						System.out.println("R extra link "+gov+"->"+target+": "+typeS);
						if(typeS.indexOf("_")>0){
							typeS = typeS.substring(0,typeS.indexOf("_"));
						}
						features.put("R extra link "+typeS, "1");
					}
				}
			}
		}
		String posLast = (String)this.posList.get(endR);
		String tokenLast = (String)this.tokenList.get(endR);
		if(posLast.equals("RP")|| posLast.equals("IN") || posLast.equals("TO")){
			posLast = tokenLast.toLowerCase();
		}
		if(UtilString.isPubc(posLast, tokenLast))
			posLast = "PUNC";
		features.put("R last POS "+posLast, "1");
	}
	
	/**
	 * Extract extra child of NE head
	 */
	public void extractEF(int head1, int head2, int[] pairOffsets,int startR, int endR){
		extractEFSub(1, head1, pairOffsets, startR, endR);
		extractEFSub(2,head2, pairOffsets, startR, endR);
	}
	
	public void extractEFSub(int Ei, int head, int[] pairOffsets, int startR, int endR){
		for(int i=0;i<dpPairList.size();i++){
			DPPair pair = (DPPair)dpPairList.get(i);
			//System.out.println(pair.getStartIndex()+"_"+pair.getStartToken()+"->"+pair.getEndIndex()+"_"+pair.getEndToken());
			int gov = pair.getStartIndex();
			int target = pair.getEndIndex();
			if(gov==head && target>gov){
				if((target<startR || target>endR) && 
						(target<pairOffsets[0]||target>pairOffsets[1])&&
						(target<pairOffsets[2]|| target>pairOffsets[3])){//not NEs
					DPType type = (DPType)this.dpTypeList.get(i);
					String typeS = type.getType();
					if(typeS.indexOf("conj")<0){
						System.out.println(gov+"->"+target+": "+typeS);
						if(typeS.indexOf("_")>0){
							typeS = typeS.substring(0,typeS.indexOf("_"));
						}
						features.put("E"+Ei+" extra link "+typeS, "1");
					}
				}
			}
		}
		String posLast = (String)this.posList.get(pairOffsets[2*(Ei-1)+1]);
		String tokenLast = (String)this.tokenList.get(pairOffsets[2*(Ei-1)+1]);
		if(UtilString.isPubc(posLast, tokenLast))
			posLast = "PUNC";
		features.put("E"+Ei+" last POS "+posLast, "1");
	}
	
	
	/**
	 * Get the head word of the verb phrase, which's the last word whose govner word is outside the chunk.
	 * If the head word is not in the chunk, return -1.
	 * @param graph
	 * @param tokenList
	 * @param start
	 * @param end
	 * @return
	 */
	public int getHeadVerb(ArrayList posList, int start, int end){
		if(start==end)
			return start;
		int head = -1;
		int headNum=0;
		for(int i=start;i<=end;i++){
			String pos = (String)posList.get(i);
			System.out.println("for head: "+i+"\t"+pos);
			if(pos.indexOf("NN")==0 || pos.indexOf("VB")==0 || pos.indexOf("CD")==0||pos.indexOf("JJ")==0){
				Object govListO = govListMap.get(i);
				if(govListO!=null){//it has a head
					System.out.println(i+"\t"+pos);
					ArrayList govList = (ArrayList)govListO;
					for(int j=0;j<govList.size();j++){
						int gov = (Integer)govList.get(j);
						System.out.println("\tgov:"+gov);
						if(gov>end || gov<start){
							head = i;
						}
					}
				}else{
					if(head==-1)
						head = i;
				}
			}
		}
		if(head==-1){//check IN
			for(int i=start;i<=end;i++){
				String pos = (String)posList.get(i);
				System.out.println("for head: "+i+"\t"+pos);
				if(pos.indexOf("IN")==0){
					Object govListO = govListMap.get(i);
					if(govListO!=null){//it has a head
						System.out.println(i+"\t"+pos);
						ArrayList govList = (ArrayList)govListO;
						for(int j=0;j<govList.size();j++){
							int gov = (Integer)govList.get(j);
							System.out.println("\tgov:"+gov);
							if(gov>end || gov<start){
								head = i;
							}
						}
					}else{
						if(head==-1)
							head = i;
					}
				}
			}
		}
		return head;
	}
	/**
	 * get the target wordlist for every gov word, i.e. child List
	 *<key_int_gov value_arrayList_parents<int>>
	 * @param dpPairList
	 * @return
	 */
	public static HashMap getTargetList(ArrayList<DPPair> dpPairList){
		HashMap map = new HashMap();
		for(int i=0;i<dpPairList.size();i++){
			DPPair pair = (DPPair)dpPairList.get(i);
			//System.out.println(pair.getStartIndex()+"_"+pair.getStartToken()+"->"+pair.getEndIndex()+"_"+pair.getEndToken());
			int gov = pair.getStartIndex();
			int target = pair.getEndIndex();
			ArrayList list = null;
			Object listO = map.get(gov);
			if(listO==null){
				list = new ArrayList();
			}else
				list = (ArrayList)listO;
			list.add(target);
			map.put(gov, list);
		}
		return map;
	}
	
}
