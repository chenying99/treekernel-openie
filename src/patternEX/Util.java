package patternEX;

import java.util.ArrayList;

public class Util {
	
	/**
	 * check if the pair types are as required.
	 * @param type1
	 * @param type2
	 * @param otherType1
	 * @param otherType2
	 * @return
	 */
	public static boolean isSuitType(String type1, String type2, String otherType1, String otherType2){
		if((type1.equals(otherType1) && type2.equals(otherType2))
				||(type1.equals(otherType2)&&type2.equals(otherType1))){
			return true;
		}else
			return false;
	}
	
	public static boolean isNumber(char c){
		if(c<='9' && c>='0')
			return true;
		return false;
	}
	
	/**
     * Validates if input String is a number
     */
    public static boolean isNumber(String in) {
        
        try {

            Double.parseDouble(in);
        
        } catch (NumberFormatException ex) {
            return false;
        }
        
        return true;
    }
    
    /**
     * check if string contains only alphabet
     * @param s
     * @return
     */
    public static boolean isAlphabet(String s){
    	for(int i=0;i<s.length();i++){
    		char c = s.charAt(i);
    		if(c>='a'&& c<='z')
    			continue;
    		else if(c>='A' && c<='Z')
    			continue;
    		else if(c=='-')
    			continue;
    		else 
    			return false;
    	}
    	return true;
    }
    
    public static void main(String[] args){
    	String word = "support";
    	System.out.println(isAlphabet(word));
    }
    
    /**
     * get a string from the arrayList;
     * @param list
     * @return
     */
    public static String array2String(ArrayList list){
    	if(list.size()==0)
    		return "";
    	StringBuffer buffer = new StringBuffer();
    	for(int i=0;i<list.size();i++){
    		buffer.append((String)list.get(i)).append(" ");
    	}
    	return buffer.toString().trim();
    }
   

}
