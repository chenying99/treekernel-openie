package utilYing;

public class UtilString {
	public static final String punc = ".,!;?\"()[]{}`':";
	
	public static boolean isPubc(String pos, String token){
		//System.out.println(pos+" "+token);
		if(pos.length()==1 && punc.indexOf(pos)>=0){
			return true;
		}
		if(token.equals("``")||token.equals("''")){
			return true;
		}
		if(token.equals("-lrb-")||token.equals("-rrb-")){
			return true;
		}
		return false;
	}
	
	public static boolean isCapital(String s){
		char c = s.charAt(0);
		if(c<='Z'&& c>='A'){
			return true;
		}
		return false;
	}
	
	public static void main(String[] args){
		String s = "``";
		System.out.println(isPubc(s,s));
	}
}
