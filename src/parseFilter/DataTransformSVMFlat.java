package parseFilter;

import graph.DPPath2TBwithNE;
import graph.LabeledEdge;
import graph.relation.DPPath2TBwithR;
import graph.relation.FeatureExR;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import parseFilter.trainEx.Treebank2StanfLabeled;
import patternEX.lucene.PhrasePTokenizerWithOffset;
import relationEx.DPPair;
import relationEx.DPType;
import relationEx.DPathUtil;

/**
 * Extract SVM flat features with FeatureExR in graph.relation package.
 * @author ying
 *
 */
public class DataTransformSVMFlat {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		exeOne(args);
	}
	
	
	
	public static void exeOne(String[] args){
		if(args.length==0){
			String path = "/media/My Passport/Ying/2012treebankRelationTestData/";
			String file = path+"new1000sent_wsj3_dist20_dev100_judged_withRpt.txt";
			//the string feature file
			//add 
			String outF = path+"svm_tk/final/treebank/tb_dev100_judged_withRpt_flat.txt.feature3";
			//feature \t featureid file, this one is only for training
			String FMapF = path+"svm_tk/final/treebank/tb_judged_withRpt_flat.txt.featureMap3";
			//svm vector file
			String outSVM = path+"svm_tk/final/treebank/tb_dev100_judged_withRpt_flat.txt3";
			//svm vector with the pair file
			String outSVMMap = path+"svm_tk/final/treebank/tb_dev100_judged_withRpt_flat.txt.pairMap3";

			try {
				//for training data, you need extractF, getFeatureMap, and transform. 
				//for test data, you don't need getFeatureMap.
				extractF(file,outF);
				//getFeatureMap(outF, FMapF);
				transform(outF, FMapF, outSVM, outSVMMap);
				//deleteDup(outSVM, outSVM+".dlDup");//for training data only
				//checkContradict(outSVM+".dlDup");//just for analyze
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(args.length==7){
			String path = args[1];
			String file = path+ args[2];
			String outF = path+ args[3];
			String FMapF = path+ args[4];
			String outSVM = path+ args[5];
			String outSVMMap = path+ args[6];
			try {
				if(args[0].equals("train")){

					extractF(file,outF);
					getFeatureMap(outF, FMapF);
					transform(outF, FMapF, outSVM, outSVMMap);
				}else if(args[0].equals("test")){
					extractF(file,outF);
					transform(outF, FMapF, outSVM, outSVMMap);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void deleteDup(String inF, String outF) throws IOException{
		HashSet setLine = new HashSet();
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		PrintStream output = new PrintStream(outF);
		String line = input.readLine();
		int sentCount = 0;
		while(line!=null){
			setLine.add(line);
			line = input.readLine();
		}
		input.close();
		Iterator iter = setLine.iterator();
		while(iter.hasNext()){
			line = (String)iter.next();
			output.println(line);
		}
		output.close();
	}
	
	/**
	 * Check if there are entries that have the same features but different judge.
	 * @param inF
	 * @throws IOException
	 */
	public static void checkContradict(String inF) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		String line = input.readLine();
		int sentCount = 0;
		HashMap map = new HashMap();
		while(line!=null){
			int index = line.indexOf(" ");
			String judge = line.substring(0,index);
			String features = line.substring(index+1);
			if(map.containsKey(features)){
				String oldJ = (String)map.get(features);
				if(oldJ!=judge){
					System.out.println(features+" "+oldJ+"->"+judge);
				}
			}else{
				map.put(features, judge);
			}
			line = input.readLine();
		}
		input.close();
	}
	
	/**
	 * Extract features from a file
	 * @param inF
	 * @param outF
	 * @throws IOException
	 */
	public static void extractF(String inF, String outF)throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		PrintStream output = new PrintStream(outF);
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
			for(int i=0;i<tokenList.size();i++){
				System.out.print((String)tokenList.get(i));
				System.out.print(" ");
			}
			System.out.println();
			
			/**************Change the tk form here************************************/
			//4.
			ArrayList posForPath = new ArrayList();//afraid that the function will chage pos.
			posForPath.addAll(posList);
			FeatureExR dpPath2TB = new FeatureExR(dpPairList,
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
				//System.out.println(start1+"\t"+end1);
				int head1 = DPathUtil.getHead(govListMap, tokenList, posList, start1, end1);
				
				int start2 = Integer.parseInt(chunks2[1]);
				int end2 = Integer.parseInt(chunks2[2]);
				int head2 = DPathUtil.getHead(govListMap, tokenList, posList, start2, end2);
				
				int[] relationOffsets = PhrasePTokenizerWithOffset.parseOffset(chunksPair[2]);
				int[] type = DPathUtil.getType(posList, relationOffsets);
				int[] pairOffset = new int[]{start1,end1,start2, end2};
				dpPath2TB.extractFeature(tokenList, posList, head1, head2, pairOffset, relationOffsets, type);
				HashMap features = dpPath2TB.getFeatures();
				int judge = Integer.parseInt(chunksPair[3]);
				if(judge==0 || judge==2){
					judge=-1;
				}
				output.println("#"+pair);
				output.println("judge\t"+judge);
				outFeatures(features,output);
				output.println();
				output.flush();
				pair = input.readLine();
			}
			line = input.readLine();//<treebankSent>
		}
		input.close();
	}
	
	public static void outFeatures(HashMap features, PrintStream output){
		Iterator iter =features.keySet().iterator();
		while(iter.hasNext()){
			String key = (String)iter.next();
			String v = (String)features.get(key);
			output.println(key+"\t"+v);
		}
	}
	
	public static void extractOne() throws IOException{
		String parseTree = "( (S (NP-SBJ-1 (NP (NP (NNP Georgia-Pacific) (NNP Corp.) (POS 's) ) (JJ unsolicited) (ADJP (QP ($ $) (CD 3.19) (CD billion) ) (-NONE- *U*) ) (NN bid) ) (PP (IN for) (NP (NNP Great) (NNP Northern) (NNP Nekoosa) (NNP Corp.) ))) (VP (VBD was) (VP (VBN hailed) (NP (-NONE- *-1) ) (PP (IN by) (NP-LGS (NNP Wall) (NNP Street) )) (PP (IN despite) (NP (NP (DT a) (JJ cool) (NN reception) ) (PP (IN by) (NP-LGS (DT the) (NN target) (NN company) )))))) (. .) ))";
		String pair = "Georgia-Pacific Corp.:0:1;Great Northern Nekoosa Corp.:9:12	<PER> bid for <PER>	-1,-1;7,8;-1,-1	1";
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
		for(int i=0;i<tokenList.size();i++){
			System.out.print((String)tokenList.get(i));
			System.out.print(" ");
		}
		System.out.println();
		
		/**************Change the tk form here************************************/
		//4.
		ArrayList posForPath = new ArrayList();//afraid that the function will chage pos.
		posForPath.addAll(posList);
		FeatureExR dpPath2TB = new FeatureExR(dpPairList,
				dpTypeList, tokenList,
				posForPath,  graph,
				null);
		dpPath2TB.getFuncWords("func_verb.txt");
		dpPath2TB.setNewPOS();
		
		//pair \t relation pattern \t offset \t judge
		
			System.out.println(pair);
			String[] chunksPair = pair.split("\t");
					
			String[] chunks = chunksPair[0].split(";");
			String[] chunks1 = chunks[0].split(":");
			String[] chunks2 = chunks[1].split(":");
			int start1 = Integer.parseInt(chunks1[1]);
			int end1 = Integer.parseInt(chunks1[2]);
			//System.out.println(start1+"\t"+end1);
			int head1 = DPathUtil.getHead(govListMap, tokenList, posList, start1, end1);
			
			int start2 = Integer.parseInt(chunks2[1]);
			int end2 = Integer.parseInt(chunks2[2]);
			int head2 = DPathUtil.getHead(govListMap, tokenList, posList, start2, end2);
			
			int[] relationOffsets = PhrasePTokenizerWithOffset.parseOffset(chunksPair[2]);
			int[] type = DPathUtil.getType(posList, relationOffsets);
			int[] pairOffset = new int[]{start1,end1,start2, end2};
			dpPath2TB.extractFeature(tokenList, posList, head1, head2, pairOffset, relationOffsets, type);
			HashMap features = dpPath2TB.getFeatures();
			int judge = Integer.parseInt(chunksPair[3]);
			if(judge==0 || judge==2){
				judge=-1;
			}
			
			outFeatures(features,System.out);
	}
	
	public static void getFeatureMap(String inF, String outF) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		PrintStream output = new PrintStream(outF);
		String line = input.readLine();
		int sentCount = 0;
		HashSet set = new HashSet();
		while(line!=null){
			if(line.length()>0){
				line = input.readLine();
				while(line.length()>0){
					String[] chunks = line.split("\t");
					set.add(chunks[0]);
					line = input.readLine();
				}
			}
			line = input.readLine();
		}
		input.close();
		
		Iterator iter = set.iterator();
		int id=1;
		while(iter.hasNext()){
			String f = (String)iter.next();
			output.println(f+"\t"+id);
			id++;
		}
	}
	
	/**
	 * Output features that are frequent enough. Assign id>=1 for every feature.
	 * @param inF
	 * @param outF
	 * @throws IOException
	 */
	public static void getFeatureMapFilt(String inF, String outF) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		PrintStream output = new PrintStream(outF);
		String line = input.readLine();
		int sentCount = 0;
		HashMap set = new HashMap();
		while(line!=null){
			if(line.length()>0){
				line = input.readLine();
				while(line.length()>0){
					String[] chunks = line.split("\t");
					if(set.containsKey(chunks[0])){
						int count = (Integer)set.get(chunks[0]);
						count++;
						set.put(chunks[0], count);
					}else{
						set.put(chunks[0], 1);
					}
					line = input.readLine();
				}
			}
			line = input.readLine();
		}
		input.close();
		
		Iterator iter = set.keySet().iterator();
		int id=1;
		while(iter.hasNext()){
			String f = (String)iter.next();
			int count = (Integer)set.get(f);
			if(count>1){
				output.println(f+"\t"+id);
				id++;
			}
		}
	}

	public static HashMap readInFeatureId(String file) throws IOException{
		HashMap featureId = new HashMap();
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line = input.readLine();
		while(line!=null){
			String[] chunks = line.split("\t");
			int value = Integer.parseInt(chunks[1]);
			//value++;
			featureId.put(chunks[0], String.valueOf(value));
			line = input.readLine();
		}
		input.close();
		return featureId;
	}
	/**
	 * Output the final file for SVM
	 * @param featureF
	 * @param featureId
	 * @param outF: feauture vector for SVM
	 * @param outMapF: with pair and feature vector
	 * @throws IOException
	 */
	public static void transform(String featureF, String featureId, String outF, String outMapF) throws IOException{
		HashMap featureIdMap = readInFeatureId(featureId);
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(featureF)));
		PrintStream output = new PrintStream(outF);
		PrintStream outputMap = new PrintStream(outMapF);
		String line = input.readLine();//the pair
		int sentCount = 0;
		HashSet set = new HashSet();
		while(line!=null){
			if(line.length()>0){
				String pair = line;
				line = input.readLine();//the judge
				ArrayList tempFVector = new ArrayList();
				String[] chunks = line.split("\t");
				StringBuffer fVector = new StringBuffer();
				fVector.append(chunks[1]);
				line = input.readLine();//the features
				while(line.length()>0){
					chunks = line.split("\t");
					if(chunks[0].indexOf("E12E2_null")<0&&chunks[0].indexOf("E1POS_")<0 && chunks[0].indexOf("E2POS_")<0){
						Object idO = featureIdMap.get(chunks[0]);
						if(idO!=null){
							String id = (String)idO;
							tempFVector.add(new Feature(Integer.parseInt(id), chunks[1]));
						}
					}
					line = input.readLine();
				}
				Collections.sort(tempFVector);
				for(int i=0;i<tempFVector.size();i++){
					Feature tempF = (Feature)tempFVector.get(i);
					fVector.append(" ").append(tempF.id).append(":").append(tempF.value);
				}
				output.println(fVector.toString());
				outputMap.println(pair);
				outputMap.println(fVector.toString());
			}
			line = input.readLine();//the pair
		}
		input.close();
	}
}
