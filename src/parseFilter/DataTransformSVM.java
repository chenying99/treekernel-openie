package parseFilter;

import graph.DPPath2TB;
import graph.DPPath2TBNoHead;
import graph.DPPath2TBwithNE;
import graph.LabeledEdge;
import graph.lexicon.DP2TBwithLex;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import parseFilter.trainEx.Treebank2Stanf;
import parseFilter.trainEx.Treebank2StanfLabeled;
import relationEx.DPPair;
import relationEx.DPType;
import relationEx.DPathUtil;

/**
 * Transform the tagged training sentences 
 * 1 with parent-r without function word
2 No parent-r without function word
3  No parent-r with all stopwords
4 No parent-r with generalized POS and small function verb set
5. ordered 4
6. with Lexicon
 * @author ying
 *
 */
public class DataTransformSVM {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "/media/My Passport/Ying/2012treebankRelationTestData/";
		String file = path+"new1000sent_wsj3_dist20_dev100_judged.txt.Stanf";
		String outF = path+"svm_tk/final/lexicon/dev_lex.txt.Stanf";
		//String file = path+"new1000sent_wsj3_dist20_dev100_judged.txt";
		//String outF = path+"svm_tk/final/lexicon/dev.txt";
		//String outF2 = path+"new1000sent_wsj3_dist20_test100_svm_2.txt";
		try {
			transform(file,outF);
			//change2toNeg(outF, outF2); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * At first I kept the judge is 2, I need to change it to -1. Now I fix the transform.
	 * @throws IOException 
	 */
	public static void change2toNeg(String inF, String outF) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		
		PrintWriter output = new PrintWriter(outF);
		String line = input.readLine();
		int sentCount = 0;
		while(line!=null){
			sentCount++;
			String[] chunks = line.split("\t");
			if(chunks[0].equals("2")){
				output.println("-1\t"+chunks[1]);
			}else{
				output.println(line);
			}
			line = input.readLine();
		}
		input.close();
		output.close();
	}
	
	public static void transform(String inF, String outF) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		
		PrintWriter output = new PrintWriter(outF);
		//PrintWriter output2 = new PrintWriter(outF+".sentMap");
		
		String line = input.readLine();
		int sentCount = 0;
		while(line!=null){
			sentCount++;
			//if(sentCount>10)
			//	break;
			String parseTree=input.readLine(); // the parse tree
			ArrayList<DPPair> dpPairList = new ArrayList();
			ArrayList<DPType> dpTypeList= new ArrayList();
			ArrayList tokenList = new ArrayList();
			ArrayList posList = new ArrayList();
			UndirectedGraph<String, LabeledEdge> graph  = new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);
			int graphSize = 0;
			try {
				graphSize = Treebank2StanfLabeled.transforGetToken(parseTree, tokenList, posList, dpPairList, dpTypeList, graph);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			HashMap govListMap = DPathUtil.getGovList(dpPairList);
			
			/**************Change the tk form here************************************/
			//DPPath2TB dpPath2TB = new DPPath2TB(parseTree, "stopwords.txt");
			//1. 
			//DPPath2TBwithNEwithHead dpPath2TB = new DPPath2TBwithNEwithHead(parseTree, null);
			//dpPath2TB.getFuncWords("func_verb.txt");
			//dpPath2TB.setNewPOS();
			//2.
			//DPPath2TBwithNE dpPath2TB = new DPPath2TBwithNE(parseTree, null);
			//3
			//DPPath2TBwithNE dpPath2TB = new DPPath2TBwithNE(parseTree, null);
			//4
			//DPPath2TBwithNE dpPath2TB = new DPPath2TBwithNE(parseTree, null);
			//dpPath2TB.getFuncWords("func_verb.txt");
			//dpPath2TB.setNewPOS();
			//5
			//DPPath2TBwithNEordered dpPath2TB = new DPPath2TBwithNEordered(parseTree, null);
			//dpPath2TB.getFuncWords("func_verb.txt");
			//dpPath2TB.setNewPOS();
			//6. lexicon
			DP2TBwithLex dpPath2TB = new DP2TBwithLex(parseTree, null);
			dpPath2TB.getFuncWords("func_verb.txt");
			dpPath2TB.setNewPOS();
			
			line = input.readLine();//<dep:short/long>
			line = input.readLine();//dp information
			while(!line.equals("<pair>")){
				line = input.readLine();
			}
			String pair = input.readLine();
			while(pair.length()>1){
				System.out.println(pair);
				String[] chunksPair = pair.split("\t");
						
				String[] chunks = chunksPair[0].split(";");
				String[] chunks1 = chunks[0].split(":");
				String[] chunks2 = chunks[1].split(":");
				int start1 = Integer.parseInt(chunks1[1]);
				int end1 = Integer.parseInt(chunks1[2]);
				System.out.println(start1+"\t"+end1);
				int head1 = DPathUtil.getHead(govListMap, tokenList, posList, start1, end1);
				
				int start2 = Integer.parseInt(chunks2[1]);
				int end2 = Integer.parseInt(chunks2[2]);
				int head2 = DPathUtil.getHead(govListMap, tokenList, posList, start2, end2);
				
				String result = dpPath2TB.getTreePath(head1, head2);
				int judge= Integer.parseInt(chunksPair[1]);
				if(judge==0 || judge==2){
					judge=-1;
				}
				output.println(judge+"\t|BT| "+result+" |ET|");
				output.flush();
				//output2.println(pair);
				//output2.println(judge+"\t|BT| "+result+" |ET|");
				//output2.println();
				//output2.flush();
				pair = input.readLine();
			}
			line = input.readLine();//<treebankSent>
		}
		input.close();
		output.close();
		//output2.close();
	}
	

}
