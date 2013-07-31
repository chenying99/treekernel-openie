package parseFilter;

import graph.DPPath2TBwithNE;
import graph.LabeledEdge;
import graph.lexicon.DP2TBwithLexWR;
import graph.relation.DPPath2TBwithR;
import graph.relation.DPPath2TBwithRordered;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import parseFilter.trainEx.Treebank2StanfLabeled;
import patternEX.lucene.PhrasePTokenizerWithOffset;
import relationEx.DPPair;
import relationEx.DPType;
import relationEx.DPathUtil;
import relationEx.DependExwithR;

/**
 * Transform the path between NEs and R to SVM PTK.
 * Input File is created by application/ExRECandidate.java
 * @author ying
 *
 */
public class DataTransformSVMwithR {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path=null;
		String file = null;
		String outF = null;
		String outFMap =null;
		if(args.length==0){
			/*path = "example/";
			file = path+"triple_judge.txt";
			outF = path+"triple_svm.txt";
			outFMap = path+"triple_svm.map";*/
			path =  "/media/My Passport/Ying/naacl2013Data/treebankRaw/";
			file = path+"test100_triple_judge.txt";
			outF = path+"test100_triple_svm.txt";
			outFMap = path+"test100_triple_svm.map";
		}else if(args.length==3){
			path = args[0];
			file = path+args[1];
			outF = path+args[2];
			outFMap = outF+".map";
		}
		
		try {
			transform(file,outF,outFMap);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void transform(String inF, String outF, String outFMap) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		
		PrintWriter output = new PrintWriter(outF);
		PrintStream outputTest = new PrintStream(outFMap);
		
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
			//output the tokenList
			/*for(int i=0;i<tokenList.size();i++){
				System.out.print((String)tokenList.get(i));
				System.out.print(" ");
			}
			System.out.println();*/
			
			/**************Change the tk form here************************************/
			//4.
			/*ArrayList posForPath = new ArrayList();//afraid that the function will chage pos.
			posForPath.addAll(posList);
			DPPath2TBwithR dpPath2TB = new DPPath2TBwithR(dpPairList,
					dpTypeList, tokenList,
					posForPath,  graph,
					null);
			dpPath2TB.getFuncWords("func_verb.txt");
			dpPath2TB.setNewPOS();*/
			
			//5.
			ArrayList posForPath = new ArrayList();//afraid that the function will chage pos.
			posForPath.addAll(posList);
			DPPath2TBwithRordered dpPath2TB = new DPPath2TBwithRordered(dpPairList,
					dpTypeList, tokenList,
					posForPath,  graph,
					null);
			dpPath2TB.getFuncWords("func_verb.txt");
			dpPath2TB.setNewPOS();
			
			
			//pair \t relation pattern \t offset \t judge
			line = input.readLine();//<<pair>>
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
				System.out.println(start2+"\t"+end2);
				System.out.println(chunksPair[2]);
				int[] relationOffsets = PhrasePTokenizerWithOffset.parseOffset(chunksPair[2]);
				int[] type = DPathUtil.getType(posList, relationOffsets);
				
				String result =  dpPath2TB.getTreePath(head1, head2,relationOffsets);
				int judge = Integer.parseInt(chunksPair[3]);
				if(judge==0 || judge==2){
					judge=-1;
				}
				outputTest.println(pair);
				if(result!=null){
					output.println(judge+"\t|BT| "+result+" |ET|");
					outputTest.println(judge+"\t|BT| "+result+" |ET|");
				}else{
					output.println(judge+"\t|BT| (null) |ET|");
					outputTest.println(judge+"\t|BT| (null) |ET|");
				}
				output.flush();
				pair = input.readLine();
			}
			line = input.readLine();//<treebankSent>
		}
		input.close();
	}

}
