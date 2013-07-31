package parseFilter;

import edu.stanford.nlp.trees.Tree;
import graph.LabeledEdge;
import graph.lexicon.DP2TBwithLexWR;
import graph.relation.DPPath2TBwithR;
import graph.relation.DPPath2TBwithRordered;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import relationEx.DPathUtil;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import parseFilter.trainEx.StanfordParser;
import parseFilter.trainEx.Treebank2Stanf;
import parseFilter.trainEx.Treebank2StanfLabeled;
import relationEx.DPPair;
import relationEx.DPType;

/**
 * Transform the ollie test data into the tree kernel form.
 * The input file has sentences and triple with offset, from ollie/GetOffset.java
 * This transform is with R.
 * @author ying
 *
 */
public class DataTfOllieTest {
	StanfordParser parser = new StanfordParser(true);
	public DataTfOllieTest(){

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//testOneSent();
		testFilewithParse();
	}
	
	public static void testFilewithParse(){
		String path = "/media/My Passport/Ying/naacl2013Data_lexical/OLLIE/";
		
		String fileTest = path+"ollie-offset_man.txt";
		String fileParse = path+"ollie-offset.txt.parse";
		String outF = path+"ollie_lex.txt";
		
		DataTfOllieTest dataTf = new DataTfOllieTest();
		try {
			dataTf.tagFilewithParse(fileTest, fileParse, outF);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * transform the path with a sentence parsed
	 * @param sentFile
	 * @param outF
	 * @throws Exception
	 */
	public void tagFilewithParse(String sentFile, String parseF, String outF) throws Exception{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(sentFile)));
		BufferedReader inputParse = new BufferedReader(new InputStreamReader(new FileInputStream(parseF)));
		
		PrintStream output = new PrintStream(outF);
		PrintStream outputMap = new PrintStream(outF+".tripleMap");
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
			this.tagOneSentParsed(sentParsed, standJudge, r, e1, e2, rOffset, e1Offset, e2Offset, output, outputMap);
			sentCount++;
			//if(sentCount>50)
			//	break;
			line = input.readLine();
		}
		input.close();
	}
	
	public void temp0to1(String file) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line = input.readLine();
		PrintStream output = new PrintStream(file+".021");
		while(line!=null){
			if(line.charAt(0)=='0'){
				output.println("-1"+line.substring(1));
			}else
				output.println(line);
			line = input.readLine();
		}
		input.close();
		
	}
	
	
	public void tagOneSentParsed(String sent, String standJudge, String r, String e1, String e2, String rOffset, String e1Offset, String e2Offset, PrintStream output, PrintStream outputMap) throws IOException{
		ArrayList<DPPair> dpPairList = new ArrayList();
		ArrayList<DPType> dpTypeList= new ArrayList();
		ArrayList tokenList = new ArrayList();
		ArrayList posList = new ArrayList();
		UndirectedGraph<String, LabeledEdge> graph  = new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);
		Treebank2StanfLabeled.transforGetToken(sent, tokenList, posList, dpPairList, dpTypeList, graph);
		ArrayList dependList = new ArrayList();
		ArrayList noneList = new ArrayList();
		Tree tree = Treebank2Stanf.getTree(sent, tokenList, posList, dependList, noneList);
		
		/**************Change the tk form here************************************/
		//4.
		/*DPPath2TBwithR dpPath2TB = new DPPath2TBwithR(dpPairList,
				dpTypeList, tokenList,
				posList,  graph,
				null);
		dpPath2TB.getFuncWords("func_verb.txt");
		dpPath2TB.setNewPOS();*/
		
		//8.
		/*DPPath2TBwithRordered dpPath2TB = new DPPath2TBwithRordered(dpPairList,
				dpTypeList, tokenList,
				posList,  graph,
				null);
		dpPath2TB.getFuncWords("func_verb.txt");
		dpPath2TB.setNewPOS();*/
		
		DP2TBwithLexWR dpPath2TB = new DP2TBwithLexWR(dpPairList,
				dpTypeList, tokenList,
				posList,  graph,
				null);
		dpPath2TB.getFuncWords("func_verb.txt");
		dpPath2TB.setNewPOS();

		HashMap govListMap = DPathUtil.getGovList(dpPairList);
		ArrayList offsetR = this.getOffset(rOffset);
		ArrayList offsetE1 = this.getOffset(e1Offset);
		ArrayList offsetE2 = this.getOffset(e2Offset);
		System.out.println("now for head "+e1);
		int head1 = DPathUtil.getHeadOllie(govListMap, tokenList, posList, offsetE1);
		System.out.println("now for head "+e2);
		int head2 = DPathUtil.getHeadOllie(govListMap, tokenList, posList, offsetE2);
		if(head1==-1 || head2==-1){
			output.println(standJudge+"\t|BT| (null) |ET|");
			outputMap.println(e1+"\t"+r+"\t"+e2);
			outputMap.println(standJudge+"\t|BT| (null) |ET|");
		}else{
			String result = dpPath2TB.getTreePath(head1, head2, offsetR);
			//YingParse parse = new YingParse(tree, dependList);
			//BufferedImage image = makeImage.displayImage(parse, noneList);
			//ImageIO.write(image, "png", new File(r+"_e1_"+e2+".png"));
			//this.makeImage.getFrame().dispose();
			if(result!=null){
				output.println(standJudge+"\t|BT| "+result+" |ET|");
				output.flush();
				outputMap.println(e1+"\t"+r+"\t"+e2);
				outputMap.println(standJudge+"\t|BT| "+result+" |ET|");
			}else{
				output.println(standJudge+"\t|BT| (null) |ET|");
				outputMap.println(e1+"\t"+r+"\t"+e2);
				outputMap.println(standJudge+"\t|BT| (null) |ET|");
			}
		}
	}
	
	public static void testOneSent(){
		DataTfOllieTest test = new DataTfOllieTest();
		String sent = "On the one hand , we cannot fully explain a higher level of order by breaking it down into its parts .";
		String standJudge = "1";
		
		String r = "maintain";
		String rOffset = "10;";
		String e1 = "it";
		String e1Offset = "5;";
		String e2 = "itself";
		String e2Offset = "11;";
		try {
			//test.tagOneSent(sent, standJudge, r, e1, e2, rOffset, e1Offset, e2Offset, System.out, System.out, System.out);
			//System.out.println("-------------------------------------");
			String parsedSent ="(ROOT (S (S (VP (TO To) (VP (VB build) (NP (NP (DT the) (JJ complex) (NNS molecules)) (SBAR (S (NP (PRP it)) (VP (VBZ needs) (S (VP (TO to) (VP (VP (VB grow)) (, ,) (VP (VB maintain) (NP (PRP itself))) (, ,) (CC and) (VP (VB reproduce)))))))))))) (, ,) (NP (DT an) (NN animal)) (VP (MD must) (VP (VB obtain) (NP (NP (CD two) (NNS types)) (PP (IN of) (NP (JJ organic) (NNS precursors)))) (PP (IN from) (NP (PRP$ its) (NN food))))) (. .)))";
			test.tagOneSentParsed(parsedSent, standJudge, parsedSent, e1, e2, rOffset, e1Offset, e2Offset, System.out, System.out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * The original offset string is offsets with ";" as segmentor
	 * Return offset arrayList
	 * @param offsetS
	 * @return
	 */
	public static ArrayList getOffset(String offsetS){
		ArrayList list = new ArrayList();
		String[] chunks = offsetS.split(";");
		for(int i=0;i<chunks.length;i++){
			if(chunks[i].trim().length()>0){
				list.add(new Integer(chunks[i]));
			}
		}
		return list;
	}
	
	/**
	 * Want to add flat vector, but you cannot transform the whole data, so 
	 * I need to segment the parsed data and offsets into 5 parts....
	 */
	public void crossVForFlat(){
		
	}

}
