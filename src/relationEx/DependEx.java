package relationEx;

import java.io.PrintStream;
import java.util.HashMap;

/**
 * Dependency extraction for a candidate relation instance.
 * @author ying
 *
 */
public class DependEx {
	/**
	 * Used when there is output.
	 * @param dpPairSet
	 * @param pairOffset
	 * @param relationOffsets
	 * @param ptType
	 * @param outputTest
	 * @return
	 */
	public static boolean dpFilter(HashMap dpPairSet, int[] pairOffset, int[] relationOffsets, int[] ptType, PrintStream outputTest){
		boolean clsLabel = false;
		//dependency between R if there are two parts
		for(int i=0;i<3;i++){
			if(ptType[i]!=-1){
				if(ptType[i+1]!=-1){
					DPType R1R2 = DependEx.getDpType(dpPairSet, relationOffsets[2*i], relationOffsets[2*i+1], relationOffsets[2*i+2], relationOffsets[2*i+3]);
					if(R1R2==null)
						return false;
				}
			}
		}
		
		//dependency between E1E2
		DPType E1E2 = DependEx.getDpType(dpPairSet, pairOffset[0], pairOffset[1], pairOffset[2], pairOffset[3]);
		if(E1E2!=null)
			outputTest.println("E1E2 type: "+ E1E2.type+"\tdirection: "+E1E2.dir);
		else
			outputTest.println("E1E2 type: null");
		
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
			if(E1R[i]!=null)
				outputTest.println("E1R["+i+"] type: "+ E1R[i].type+"\tdirection: "+E1R[i].dir);
			else
				outputTest.println("E1R["+i+"] type: null");
			if(E2R[i]!=null)
				outputTest.println("E2R["+i+"] type: "+ E2R[i].type+"\tdirection: "+E2R[i].dir);
			else
				outputTest.println("E2R["+i+"] type: null");
		}
		
		//1. check if it is the premodifier type, so the pattern type should be between type
		if(ptType[1]!=-1 && ptType[0]==-1 && ptType[2]==-1){
			if(DependEx.isPremodifier(E1E2, E1R, E2R)){
				outputTest.println("isPremodifier");
				return true;
			}
		}
		
		//2. check if it is possessive type
		if(DependEx.isPossessive(E1E2, E1R, E2R)){
			outputTest.println("isPossessive");
			return true;
		}
		
		//3. check if it is preposition or verbal
		if(DependEx.isVerbal(E1E2, E1R, E2R)){
			outputTest.println("isVerbal or preposition");
			return true;
		}
		return clsLabel;
	}
	
	public static boolean dpFilter(HashMap dpPairSet, int[] pairOffset, int[] relationOffsets, int[] ptType){
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
			return true;
		}
		return clsLabel;
	}
	
	/**
	 * check if it is the premodifier type
	 * @param E1E2
	 * @param E1R
	 * @param E2R
	 * @return
	 */
	public static boolean isPremodifier(DPType E1E2, DPType[] E1R, DPType[] E2R){
		if(E1R[1]==null &&
				E1E2!=null && E1E2.type.equals("nn")&&
				E2R[1]!=null && E2R[1].type.equals("nn"))
			return true;
		return false;
	}
	
	/**
	 * check if it is possessive type
	 * @param E1E2
	 * @param E1R
	 * @param E2R
	 * @return
	 */
	public static boolean isPossessive(DPType E1E2, DPType[] E1R, DPType[] E2R){
		if(E1E2!=null && E1E2.type.equals("poss")){
			for(int i=0;i<E2R.length;i++){
				if(E2R[i]!=null && E2R[i].type.equals("nn"))
					return true;
			}
		}
		return false;
	}
	
	public static boolean isVerbal(DPType E1E2, DPType[] E1R, DPType[] E2R){
		if(E1E2==null || E1E2.type.indexOf("conj")<0){
			boolean hasRE1 = false;
			boolean hasRE2 = false;
			for(int i=0;i<3;i++){
				if(E1R[i]!=null && E1R[i].type.indexOf("conj")<0)
					hasRE1 = true;
				if(E2R[i]!=null && E2R[i].type.indexOf("conj")<0)
					hasRE2 = true;
			}
			if(hasRE1 && hasRE2)
				return true;
		}
		return false;
	}
	
	
	/**
	 * Get the first dependency type between two chunks. 
	 * Return null if no dependency found.
	 * The type's direction is 1 if from the first chunk to the second
	 *  (It means en1 is the gov (company), en2 is the target (this)); -1 otherwise.
	 * 
	 * @param dpPairSet
	 * @param start1
	 * @param end1
	 * @param start2
	 * @param end2
	 * @return
	 */
	public static DPType getDpType(HashMap dpPairSet, int start1, int end1, int start2, int end2){
		for(int i=start1;i<=end1;i++){
			for(int j=start2;j<=end2;j++){
				DPPair pairTemp = new DPPair(i,j);
				Object typeO = dpPairSet.get(pairTemp);
				if(typeO!=null){
					//System.out.println("dep between "+i+"\t"+j);
					return new DPType((String)typeO,1);
				}else{
					//System.out.println("no dep between "+i+"\t"+j);
					pairTemp = new DPPair(j,i);
					typeO = dpPairSet.get(pairTemp);
					if(typeO!=null){
						//System.out.println("dep between "+j+"\t"+i);
						return new DPType((String)typeO,-1);
					}else{
						//System.out.println("no dep between "+j+"\t"+i);
					}
				}
			}
		}
		return null;
	}
}
