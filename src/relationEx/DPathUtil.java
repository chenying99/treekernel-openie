package relationEx;

import java.util.ArrayList;
import java.util.HashMap;

import reverb.ContextPhMatch;


/**
 * Has functions such as get gov wordlist for every dep word.
 * Get heads for a phrase from the dp information.
 * @author ying
 *
 */
public class DPathUtil {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Get the head word of the chunk, which's the last word whose govner word is outside the chunk.
	 * If the head word is not in the chunk, return -1.
	 * @param graph
	 * @param tokenList
	 * @param start
	 * @param end
	 * @return
	 */
	public static int getHead(HashMap govListMap,ArrayList tokenList , ArrayList posList, int start, int end){
		if(start==end)
			return start;
		//System.out.println("start:"+start+"\tend:"+end);
		int head = -1;
		for(int i=start;i<=end;i++){
			String pos = (String)posList.get(i);
			//System.out.println(i+"\t"+pos);
			if(pos.indexOf("NN")==0 || pos.indexOf("VB")==0 || pos.indexOf("PRP")==0){
				Object govListO = govListMap.get(i);
				if(govListO!=null){//it has a head
					//System.out.println(i+"\t"+pos);
					ArrayList govList = (ArrayList)govListO;
					for(int j=0;j<govList.size();j++){
						int gov = (Integer)govList.get(j);
						//System.out.println("\tgov:"+gov);
						if(gov>end || gov<start){
							head = i;
						}
					}
				}else{
					head = i;
				}
			}
		}
		if(head==-1){
			if(((String)posList.get(end)).equals("POS")){
				head= end;
			}
		}
		return head;
	}
	
	/**
	 * Get the head word of the chunk, which's the last word whose govner word is outside the chunk.
	 * The headword might not be verb or noun. it might be cd
	 * If the head word is not in the chunk, return -1.
	 * @param graph
	 * @param tokenList
	 * @param start
	 * @param end
	 * @return
	 */
	public static int getHeadReverb(HashMap govListMap,ArrayList tokenList , ArrayList posList, int start, int end){
		if(start==end)
			return start;
		int head = -1;
		for(int i=start;i<=end;i++){
			String pos = (String)posList.get(i);
			System.out.println(i+"\t"+pos+"/"+tokenList.get(i));
			if(pos.indexOf("DT")==0 || pos.indexOf("NN")==0 || pos.indexOf("VB")==0 || pos.indexOf("CD")==0||pos.indexOf("JJ")==0 || pos.indexOf("PRP")==0){
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
					head = i;
				}
			}
		}
		if(head==-1){
			for(int i=start;i<=end;i++){
				String pos = (String)posList.get(i);
				//System.out.println(i+"\t"+pos);
					Object govListO = govListMap.get(i);
					if(govListO!=null){//it has a head
						//System.out.println(i+"\t"+pos);
						ArrayList govList = (ArrayList)govListO;
						for(int j=0;j<govList.size();j++){
							int gov = (Integer)govList.get(j);
							//System.out.println("\tgov:"+gov);
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
		return head;
	}
	
	
	/**
	 * Get the head word of the chunk, which's the last word whose govner word is outside the chunk.
	 * The headword might not be verb or noun. it might be cd
	 * If the head word is not in the chunk, return -1.
	 * The difference with getHead and getHeadReverb is that here the offsets are not continuous.
	 * @param graph
	 * @param tokenList
	 * @param start
	 * @param end
	 * @return
	 */
	public static int getHeadOllie(HashMap govListMap,ArrayList tokenList , ArrayList posList, ArrayList offsets){
		if(offsets.size()==1)
			return (Integer)offsets.get(0);
		int head = -1;
		for(int index=0;index<offsets.size();index++){
			int i=(Integer)offsets.get(index);
			String pos = (String)posList.get(i);
			//System.out.println(i+"\t"+pos+"/"+tokenList.get(i));
			if(pos.indexOf("DT")==0 || pos.indexOf("NN")==0 || pos.indexOf("VB")==0 || pos.indexOf("CD")==0||pos.indexOf("JJ")==0){
				Object govListO = govListMap.get(i);
				if(govListO!=null){//it has a head
					//System.out.println(i+"\t"+pos+"<-");
					ArrayList govList = (ArrayList)govListO;
					for(int j=0;j<govList.size();j++){
						int gov = (Integer)govList.get(j);
						//System.out.println("\tgov:"+gov);
						if(!offsets.contains(gov)){
							head = i;
						}
					}
				}else{
					head = i;
				}
			}
		}
		if(head==-1){
			for(int index=0;index<offsets.size();index++){
				int i=(Integer)offsets.get(index);
				String pos = (String)posList.get(i);
				String token = (String)tokenList.get(i);
						
				//System.out.println(i+"\t"+pos);
				//it can not be conjunctions or punctuations
				if(pos.indexOf("CC")<0 && !utilYing.UtilString.isPubc(pos, token)){
					
					Object govListO = govListMap.get(i);
					if(govListO!=null){//it has a head
						//System.out.println(i+"\t"+pos);
						ArrayList govList = (ArrayList)govListO;
						for(int j=0;j<govList.size();j++){
							int gov = (Integer)govList.get(j);
							//System.out.println("\tgov:"+gov);
							if(!offsets.contains(gov)){
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
		//if(head!=-1)
		//	System.out.println("head:"+head+"_"+tokenList.get(head));
		return head;
	}
	
	/**
	 * get the gov wordlist for every dep word. 
	 *<key_int_target value_arrayList_parents<int>>
	 * @param dpPairList
	 * @return
	 */
	public static HashMap getGovList(ArrayList<DPPair> dpPairList){
		HashMap map = new HashMap();
		for(int i=0;i<dpPairList.size();i++){
			DPPair pair = (DPPair)dpPairList.get(i);
			//System.out.println(pair.getStartIndex()+"_"+pair.getStartToken()+"->"+pair.getEndIndex()+"_"+pair.getEndToken());
			int gov = pair.getStartIndex();
			int target = pair.getEndIndex();
			ArrayList list = null;
			Object listO = map.get(target);
			if(listO==null){
				list = new ArrayList();
			}else
				list = (ArrayList)listO;
			list.add(gov);
			map.put(target, list);
		}
		return map;
	}
	public static HashMap getDPMap(ArrayList<DPPair> dpPairList, ArrayList<DPType> dpTypeList){
		HashMap map = new HashMap();
		for(int i=0;i<dpPairList.size();i++){
			DPPair pair = dpPairList.get(i);
			DPType type = dpTypeList.get(i);
			map.put(pair, type.type);
		}
		return map;
	}
	
	/**
	 * Get the type for every relation segment, is it noun, verb or only preposition.
	 * @param posList
	 * @param relationOffsets
	 * @return
	 */
	public static int[] getType(ArrayList posList, int [] relationOffsets){
		int[] type = new int[3];
		for(int i=0;i<type.length;i++){
			if(relationOffsets[2*i]!=-1){
				StringBuffer temp = new StringBuffer();
				for(int k=relationOffsets[2*i];k<=relationOffsets[2*i+1];k++){
					temp.append((String)posList.get(k)).append(" ");
				}
				if(i==0){
					type[i]= ContextPhMatch.sdPhMatch(temp.toString().trim());
				}else{
					type[i] = ContextPhMatch.btPhMatch(temp.toString().trim());
				}
			}else
				type[i]=-1;
		}
		return type;
	}

}
