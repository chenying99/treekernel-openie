package ollie;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import utilYing.UtilMath;

/**
 * Use 5 cross-validation to compare ollie and my svm result
 * @author ying
 *
 */
public class CrossValide {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "/media/My Passport/Ying/ollie/annotations/";
		String ollieF = path+"ollie-scored.txt.filt";
		
		String path2 = "/media/My Passport/Ying/naacl2013Data_lexical/OLLIE/";
		String standF = path2+"ollie_lex.txt";//the tree
		String svmPredict = path2+"svm_predictions_all";
		try {
			//crossV_SVM(svmPredict,standF);
			separateFile(standF);
			
			//String ollieF1 = path+"ollie-offset.txt";
			//String ollieParseF = path+"ollie-offset.txt.parse";
			//separateOffsetF(ollieF1, ollieParseF);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Add ollie tagged data into the training SVM might help.
	 * @param svmF
	 * @throws IOException
	 */
	public static void separateFile(String svmF)throws IOException{
		for(int i=0;i<5;i++){
			PrintStream outputTrain = new PrintStream(svmF+".train_"+i);
			PrintStream outputTest  = new PrintStream(svmF+".test_"+i);
			BufferedReader inputSVM = new BufferedReader(new InputStreamReader(new FileInputStream(svmF)));
			String line = inputSVM.readLine();//the result
			int lineCount=0;
			while(line!=null){
				if(lineCount%5==i){
					outputTest.println(line);
				}else{
					outputTrain.println(line);
				}
				line = inputSVM.readLine();
				lineCount++;
			}
			inputSVM.close();
		}
	}
	
	/**
	 * For flat, I need to separate the ollie-offset.txt, ollie-offset.txt.parse
	 * @throws IOException 
	 */
	public static void separateOffsetF(String sentFile, String parseF) throws IOException{
		for(int i=1;i<5;i++){
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(sentFile)));
			BufferedReader inputParse = new BufferedReader(new InputStreamReader(new FileInputStream(parseF)));

			PrintStream outputSentTrain = new PrintStream(sentFile+".train_"+i);
			PrintStream outputSentTest = new PrintStream(sentFile+".test_"+i);
			PrintStream outputParseTrain = new PrintStream(parseF+".train_"+i);
			PrintStream outputParseTest = new PrintStream(parseF+".test_"+i);
			String line = input.readLine();//the sentence.
			int sentCount=0;
			while(line!=null){
				String sent = line;
				String r = input.readLine();
				String rOffset = input.readLine();
				if(rOffset.equals("<no offset>")){
					line = input.readLine();
					continue;
				}

				String e1 = input.readLine();
				String e1Offset = input.readLine();
				if(e1Offset.equals("<no offset>")){
					line = input.readLine();
					continue;
				}

				String e2 = input.readLine();
				String e2Offset = input.readLine();
				if(e2Offset.equals("<no offset>")){
					line = input.readLine();
					continue;
				}
				String sentParsed = inputParse.readLine();
				String standJudge = input.readLine();
				if(standJudge.equals("0")){
					standJudge="-1";
				}
				if(sentCount%5==i){
					outputSentTest.println(sent);
					outputSentTest.println(r);
					outputSentTest.println(rOffset);
					outputSentTest.println(e1);
					outputSentTest.println(e1Offset);
					outputSentTest.println(e2);
					outputSentTest.println(e2Offset);
					outputSentTest.println(standJudge);
					outputParseTest.println(sentParsed);
				}else{
					outputSentTrain.println(sent);
					outputSentTrain.println(r);
					outputSentTrain.println(rOffset);
					outputSentTrain.println(e1);
					outputSentTrain.println(e1Offset);
					outputSentTrain.println(e2);
					outputSentTrain.println(e2Offset);
					outputSentTrain.println(standJudge);
					outputParseTrain.println(sentParsed);
				}
				sentCount++;
				//if(sentCount>50)
				//	break;
				line = input.readLine();
			}
			input.close();
			inputParse.close();
			outputSentTrain.close();
			outputSentTest.close();
			outputParseTrain.close();
			outputParseTest.close();
		}
	}
	
	/**
	 * Input the svm result and standard judge Map.
	 * @param reverbF
	 * @throws IOException 
	 */
	public static void crossV_SVM(String svmF, String standF) throws IOException{
		for(int i=0;i<5;i++){
			int pp=0;
			int pn=0;
			int np=0;
			int nn=0;
			BufferedReader inputSVM = new BufferedReader(new InputStreamReader(new FileInputStream(svmF)));
			BufferedReader inputStand = new BufferedReader(new InputStreamReader(new FileInputStream(standF)));
			
			String line = inputSVM.readLine();//the result
			String lineStand = inputStand.readLine();
			int lineCount=0;
			while(line!=null){
				if(lineCount%5==i){
					String[] chunks = lineStand.split("\t");
					
					int standJudge =Integer.parseInt(chunks[0]);
						int svmJudge = 1;
						if(line.charAt(0)=='-'){
							svmJudge=-1;
						}

						if(standJudge==1 && svmJudge==1){
							pp++;
						}else if(standJudge==1 && svmJudge!=1){
							pn++;
						}else if(standJudge!=1 && svmJudge==1){
							np++;
						}else if(standJudge!=1 && svmJudge!=1){
							nn++;
						}
				}
				line = inputSVM.readLine();
				lineStand = inputStand.readLine();
				lineCount++;
			}
			inputSVM.close();
			inputStand.close();
			System.out.println("pp="+pp+"\tpn="+pn+"\tnp="+np);
			double f = UtilMath.FScore(pp, pn, np);
		}
	}
}
