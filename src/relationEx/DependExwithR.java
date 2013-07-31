package relationEx;

import java.util.ArrayList;
import java.util.HashMap;

import reverb.ContextPhMatch;

public class DependExwithR {
	public static boolean dpFilter(ArrayList tokenList, ArrayList posList, HashMap dpPairSet, int[] pairOffset, int[] relationOffsets, int[] ptType){
		//if a unigram preposition is at the beginning or after, then it is wrong.
		if(relationOffsets[0]!=-1 && relationOffsets[0]==relationOffsets[1]){
			String posTemp = (String)posList.get(relationOffsets[0]);
			if(posTemp.equals("IN") || posTemp.equals("RP") || posTemp.equals("TO")){
				System.out.println("uni pos in before");
				return false;
			}
		}
		if(relationOffsets[4]!=-1 && relationOffsets[4]==relationOffsets[5]){
			String posTemp = (String)posList.get(relationOffsets[4]);
			if(posTemp.equals("IN") || posTemp.equals("RP") || posTemp.equals("TO")){
				System.out.println("uni pos in after");
				return false;
			}
		}
		
		boolean clsLabel = false;
		//dependency between R if there are two parts
		for(int i=0;i<2;i++){
			if(ptType[i]!=-1){
				if(ptType[i+1]!=-1){
					DPType R1R2 = DependEx.getDpType(dpPairSet, relationOffsets[2*i], relationOffsets[2*i+1], relationOffsets[2*i+2], relationOffsets[2*i+3]);
					if(R1R2==null)
						return false;
				}
			}
		}
				
		DPType E1E2 = DependEx.getDpType(dpPairSet, pairOffset[0], pairOffset[1], pairOffset[2], pairOffset[3]);
			
		DPType[] E1R = new DPType[3];
		DPType[] E2R = new DPType[3];
		for(int i=0;i<3;i++){
			if(ptType[i]!=-1){
				E1R[i] = DependEx.getDpType(dpPairSet, pairOffset[0], pairOffset[1], relationOffsets[2*i], relationOffsets[2*i+1]);
				E2R[i] = DependEx.getDpType(dpPairSet, pairOffset[2], pairOffset[3], relationOffsets[2*i], relationOffsets[2*i+1]);
				if(E1R[i]!=null)
					System.out.println("E1R "+i+" "+E1R[i].type);
				else
					System.out.println("E1R "+i+" null");
				if(E2R[i]!=null)
					System.out.println("E2R "+i+" "+E2R[i].type);
				else
					System.out.println("E2R "+i+" null");
			}else{
				E1R[i] = null;
				E2R[i] = null;
			}
		}
		
		//1. check if it is the premodifier type, so the pattern type should be between type
		if(ptType[1]!=-1 && ptType[0]==-1 && ptType[2]==-1){
			if(DependEx.isPremodifier(E1E2, E1R, E2R)){
				return true;
			}
		}
		
		//2. check if it is possessive type
		if(DependEx.isPossessive(E1E2, E1R, E2R)){
			return true;
		}
		
		//3. check if it is preposition or verbal
		if(DependEx.isVerbal(E1E2, E1R, E2R)){
			//check if the preposition is included
			if(!checkPrep(tokenList, posList, E1R, E2R, relationOffsets, ptType)){
				return false;
			}
			return true;
		}
		return clsLabel;
	}
	
	public static boolean dpForRSVM(ArrayList tokenList, ArrayList posList, HashMap dpPairSet, int[] pairOffset, int[] relationOffsets, int[] ptType){
		//if a unigram preposition is at the beginning or after, then it is wrong.
		if(relationOffsets[0]!=-1 && relationOffsets[0]==relationOffsets[1]){
			String posTemp = (String)posList.get(relationOffsets[0]);
			if(posTemp.equals("IN") || posTemp.equals("RP") || posTemp.equals("TO")){
				System.out.println("uni pos in before");
				return false;
			}
		}
		if(relationOffsets[4]!=-1 && relationOffsets[4]==relationOffsets[5]){
			String posTemp = (String)posList.get(relationOffsets[4]);
			if(posTemp.equals("IN") || posTemp.equals("RP") || posTemp.equals("TO")){
				System.out.println("uni pos in after");
				return false;
			}
		}
				
		boolean clsLabel = false;
		//dependency between R if there are two parts
		for(int i=0;i<2;i++){
			if(ptType[i]!=-1){
				if(ptType[i+1]!=-1){
					DPType R1R2 = DependEx.getDpType(dpPairSet, relationOffsets[2*i], relationOffsets[2*i+1], relationOffsets[2*i+2], relationOffsets[2*i+3]);
					if(R1R2==null)
						return false;
				}
			}
		}
		DPType E1E2 = DependEx.getDpType(dpPairSet, pairOffset[0], pairOffset[1], pairOffset[2], pairOffset[3]);
		
		DPType[] E1R = new DPType[3];
		DPType[] E2R = new DPType[3];
		for(int i=0;i<3;i++){
			if(ptType[i]!=-1){
				E1R[i] = DependEx.getDpType(dpPairSet, pairOffset[0], pairOffset[1], relationOffsets[2*i], relationOffsets[2*i+1]);
				E2R[i] = DependEx.getDpType(dpPairSet, pairOffset[2], pairOffset[3], relationOffsets[2*i], relationOffsets[2*i+1]);
			}else{
				E1R[i] = null;
				E2R[i] = null;
			}
		}
		//check if the preposition is included
		if(!checkPrep(tokenList, posList, E1R, E2R, relationOffsets, ptType)){
			return false;
		}
		System.out.println("Relation is true");
		return true;
	}
	
	public static boolean checkPrep(ArrayList tokenList,ArrayList posList, DPType[] E1R, DPType[] E2R, int[]relationOffsets, int[] rTypes){
	
		for(int i=0;i<3;i++){
			if(relationOffsets[2*i]!=-1 && rTypes[i]==ContextPhMatch.VP && E1R[i]!=null && E1R[i].type.indexOf("prep")>=0 && !E1R[i].type.equals("prep_by")){
				if(!checkPrepSub(tokenList, E1R[i].type,relationOffsets[2*i],relationOffsets[2*i+1])){
					System.out.println("E1R pos not match in relation seg "+i);
					return false;
				}
			}
			if(relationOffsets[2*i]!=-1 &&rTypes[i]==ContextPhMatch.VP && E2R[i]!=null && E2R[i].type.indexOf("prep")>=0 && !E2R[i].type.equals("prep_by")){
				if(!checkPrepSub(tokenList,  E2R[i].type,relationOffsets[2*i],relationOffsets[2*i+1])){
					System.out.println("E2R pos not match in relation seg "+i);
					return false;
				}
			}
			
			DPType tempER = null;
			if(i==0)
				tempER = E1R[i];
			else
				tempER = E2R[i];
			
		}
		System.out.println("check prep is true");
		return true;
	}
	
	/**
	 * If the dp relation has prep_*, and relation type is vb, then * need to be in the relation word
	 * @param tokenList
	 * @param type
	 * @param startR
	 * @param endR
	 * @return
	 */
	public static boolean checkPrepSub(ArrayList tokenList, String type, int startR, int endR){
		int index = type.lastIndexOf("_");
		String posShould = type.substring(index+1);
		String pos = (String)tokenList.get(endR);
		if(pos.equalsIgnoreCase(posShould)){
			return true;
		}else{
			return false;
		}
	}
	
	public static boolean checkPrepSubReverse(ArrayList tokenList,ArrayList posList, DPType tempER, int[]relationOffsets, int i, int[] rTypes){
		//if the last word is prep, then the dp relation has to have a prep
		if(relationOffsets[2*i]!=-1 && (rTypes[i]==ContextPhMatch.VP || rTypes[i]==ContextPhMatch.NP) ){
			String posLast = (String)posList.get(relationOffsets[2*i+1]);
			String tokenLast = (String)tokenList.get(relationOffsets[2*i+1]);
			System.out.println("posLast of "+i+": "+posLast);
			System.out.println("tokenLast of "+i+": "+tokenLast);
			if(posLast.equals("IN") && !tokenLast.equals("by")){
				boolean hasprep = false;

				if(tempER!=null && tempER.type.indexOf("prep")>=0){
					int index = tempER.type.indexOf("_");
					String posShould =tempER.type.substring(index+1);
					System.out.println("posShould of "+i+": "+posShould);
					if(tokenLast.equalsIgnoreCase(posShould)){
						hasprep=true;
					}
				}
				if(tempER!=null&&!hasprep)
					return false;
			}
		}
		return true;
	}
	
}
