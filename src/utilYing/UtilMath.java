package utilYing;

public class UtilMath {
	
	public static double FScore(int pp, int pn, int np){
		double recall = (double)pp/(pp+pn);
		double precision = (double)pp/(pp+np);
		double F = 2*recall*precision/(recall+precision);
		System.out.println("recall:"+recall);
		System.out.println("precision:"+precision);
		System.out.println("F:"+F);
		return F;
	}
	
	public static void main(String[] args){
		UtilMath.FScore(8,17,3);
	}

}
