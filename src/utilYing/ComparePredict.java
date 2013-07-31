package utilYing;

import java.io.*;
/**
 * Just check if the two SVM prediction output has the same results.
 * @author ying
 *
 */
public class ComparePredict {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "/home/ying/study/tools/SVM-Light-1.5-to-be-released/treebankMy/experiment/final/treebank/";
		String file1 = path+"svm_predictions_TandV";
		String fileOrdered = path+"svm_predictions_TandV_5";
		try {
			check(file1, fileOrdered);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void check(String file1, String fileOrderd) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file1)));
		BufferedReader inputOrd = new BufferedReader(new InputStreamReader(new FileInputStream(fileOrderd)));
		String line = input.readLine();
		String lineOrd = inputOrd.readLine();
		while(line!=null){
			int judge1 = 1;
			if(line.startsWith("-")){
				judge1 = -1;
			}
			
			int judgeOrd = 1;
			if(lineOrd.startsWith("-")){
				judgeOrd = -1;
			}
			if(judge1!=judgeOrd){
				System.out.println("line:"+line);
				System.out.println("lineOrd:"+lineOrd);
				System.out.println();
			}
			line = input.readLine();
			lineOrd = inputOrd.readLine();
		}
		input.close();
	}

}
