package parseFilter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;


/**
 * combining two features Tree and vector for relation triple.
 * @author ying
 *
 */
public class DataTransformSVMTandV {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "";
		String file = "";
		String inSVMMap = "";
		String outF = "";
		
		if(args.length==0){
				
		 path = "/media/My Passport/Ying/naacl2013Data/treebankRaw/";
		//treebank
		//the tree kernel
		 file = path+"train_triple_svm.map";
		//the vector kernel
		 inSVMMap = path+"train_triple_judge_flat.txt.pairMap3";
		//combined
		 outF = path+"train_triple_judge_TandV_5.txt3";
		}else if(args.length==4){
			path = args[0];
			file = path+args[1];
			inSVMMap = path + args[2];
			outF = path + args[3];
		}
		//reverb
		//the tree kernel
		//String file = path+"svm_tk/reverb1000_withR_4.txt.tripleMap";
		//String file = path+"svm_tk/reverb500_withR_4.txt.dev.tripleMap";
		//the vector kernel
		//String inSVMMap = path+"svm_tk/flatReverb/1000.flat.pairMap";
		//String inSVMMap = path+"svm_tk/flatReverb/500.dev.flat.pairMap";
		//combined
		//String outF = path+"svm_tk/flatReverb/1000_T4andV.txt";
		//String outF = path+"svm_tk/flatReverb/500_dev_T4andV.txt";
				
		try {
			HashMap map = readVF(inSVMMap);
			transform(map, file,outF);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Read in the vector features for every pair
	 * @param mapF
	 * @return
	 * @throws IOException 
	 */
	public static HashMap readVF(String mapF) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(mapF)));
		String line = input.readLine();//pair
		HashMap map = new HashMap();
		while(line!=null){
			String vector = input.readLine();
			map.put(line.substring(1), vector);
			line = input.readLine();
		}
		input.close();
		return map;
	}
	
	public static void transform(HashMap vectorMap, String inF, String outF) throws IOException{
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inF)));
		
		PrintWriter output = new PrintWriter(outF);
		
		String line = input.readLine();//the pair
		int sentCount = 0;
		while(line!=null){
			sentCount++;
			String tree = input.readLine();
			String vector = (String)vectorMap.get(line);
			if(vector==null){
				System.out.println(line);
			}else{
				int index = vector.indexOf(" ");
				if(index!=-1){
					vector = vector.substring(index+1);
				}else{
					vector = "";//no vector features
				}
				output.println(tree+" "+vector);
			}
			line = input.readLine();//the pair
		}
		input.close();
		output.close();
	}

}
