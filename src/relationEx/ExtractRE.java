package relationEx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.NIOFSDirectory;

import patternEX.Gram;



/**
 * Extract Relation using the manually mapped list.
 * It's very similar to ExtractManualWithWords.java in IE/Tac/.
 * The difference is that this time we use patterns as relations, instead of unigram word.
 * @author ying
 *
 */
public class ExtractRE {
	
	public int minPatternFreq = 2;
	
	/**
	 * Get relation word clusters
	 * Return ArrayList, 
	 * The elements are lists<String> of words belong to this cluster
	 */
	ArrayList clusterList=null;
	//Labels of each cluster
	ArrayList labels = null;
	
	public ExtractRE(int min){
		this.minPatternFreq = min;
	}
	
	
	/**
	 * Get relation word clusters
	 * Return ArrayList, 
	 * The elements are lists<String> of words belong to this cluster
	 */
	public void getCluster(String file) throws IOException{
		//The elements are lists<String> of words belong to this cluster
		this.clusterList = new ArrayList();
		this.labels = new ArrayList();
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line = input.readLine();
		int lineCount=0;
		HashSet current = null;
		while(line!=null){
			if(line.length()>1){
				if(line.indexOf("L:")==0){
					this.labels.add(line.substring(2));
					current = new HashSet();
					this.clusterList.add(current);
				}else{
					current.add(line.trim());
				}
			}
			
			line = input.readLine();
			lineCount++;
		}
		input.close();
		
		/*for(int i=0;i<this.labels.size();i++){
			String label = (String)this.labels.get(i);
			System.out.print(label);
			ArrayList currentTemp = (ArrayList)this.clusterList.get(i);
			for(int j=0;j<currentTemp.size();j++){
				String tag = (String)currentTemp.get(j);
				System.out.print("\t"+tag);
			}
			System.out.println();
		}*/
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//the parameter is the min frequency.
		ExtractRE extractor = new ExtractRE(1);
		
		String luceneIndexPath = null;
		String file = null;
		String docLabelF = null;
		
		if(args.length==0){
			String path ="/media/My Passport/Ying/TAC2011/pattern/";;
		
			luceneIndexPath = path+"patternIndex/restrict2/per2per1/";
			file = path+"restrict2/manualLabels/p2p.txt";
			docLabelF = path+"restrict2/relationResult/p2p_freq1.txt";
			//luceneIndexPath = path+"patternIndex/restrict/per2org1/";
			//file = path+"restrict/manualLabels/p2o.txt";
			//docLabelF = path+"restrict/relationResult/p2o_freq1.txt";
			
		}else if(args.length==3){
			luceneIndexPath = args[0];
			file = args[1];
			docLabelF = args[2];
		}
		try {
			/**
			 * Get relation word clusters
			 * Return ArrayList, 
			 * The elements are lists<String> of words belong to this cluster
			 * And ArrayList label.
			 */
			extractor.getCluster(file);
			extractor.extractRE(luceneIndexPath, docLabelF);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void extractRE(String luceneIndexPath, String outputF) throws Exception{
		IndexReader reader = IndexReader.open(NIOFSDirectory.open(new File(luceneIndexPath)), true);
		//docLabels contain the label lists for every document, i.e. named entity pairs.
		ArrayList docLabels = new ArrayList();
		int maxDoc = reader.maxDoc();
		for(int i=0;i<maxDoc;i++){
			docLabels.add(new HashSet());
		}
		
		//int maxRelateDoc = maxDoc/20;//filter out relations that are too frequent
		Searcher searcher = new IndexSearcher(reader);
		for(int i=0;i<clusterList.size();i++){
			String label = (String)labels.get(i);
			System.out.println("label:"+label);
			if(label.equals("title"))
				continue;
			HashSet<String> wordList = (HashSet)clusterList.get(i);
			//Get documents which can be labeled by this cluster
			BooleanQuery bq = new  BooleanQuery();
			for(String pattern: wordList){
				Term term = new Term("content", pattern);
				bq.add(new TermQuery(term),BooleanClause.Occur.SHOULD);
			}
			int hitsNum=maxDoc/20;
			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsNum, false);
			//search
			searcher.search(bq, collector);
			int numTotalHits = collector.getTotalHits();
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			int end = Math.min(numTotalHits, hitsNum);
			for (int j = 0; j < end; j++) {//for every doc, check if this label can be its relation
				int id = hits[j].doc;
				Document pairTemp = reader.document(id);
				String entity1= pairTemp.get("entry1");
				String entity2 = pairTemp.get("entry2");
				if(entity1.equals(entity2))
					continue;
				//start filtering
				int freq = 0;
				//for every word in the label, get the frequency of the term in the document
				ArrayList freqList = new ArrayList();
				for(String pattern: wordList){
					Term term = new Term("content", pattern);
					TermDocs termDoc = reader.termDocs(term);
					if(termDoc.skipTo(id)){
						int freqTemp = termDoc.freq();
						freqList.add(new Gram(pattern, freqTemp));
					}
				}
				Collections.sort(freqList);
				for(int listI=0;listI<freqList.size();listI++){
					if(listI==0){
						freq += ((Gram)freqList.get(listI)).getFreq();
					}else{
						if(!isSub(freqList, listI)){
							freq += ((Gram)freqList.get(listI)).getFreq();
						}
					}
				}
				//end filtering
				if(freq>=minPatternFreq){
					//find a document evidence for the relation label.
					//String docEvidence = findDocEvidence(bq, reader.document(id), reader, reader3,  searcher3 ) ;
					HashSet docLabel = (HashSet)docLabels.get(id);
					docLabel.add(label);
				}
			}
		}
		outputResult( outputF,  docLabels,  reader);
	}
	
	public static void outputResult(String outputF, ArrayList docLabels, IndexReader reader) throws CorruptIndexException, IOException{
		//output the result
		PrintStream output = new PrintStream(outputF);
		for(int i=0;i<docLabels.size();i++){
			HashSet<String> docLabel = (HashSet)docLabels.get(i);
			if(docLabel.size()>0){
				Document doc = reader.document(i);
				//String pair = doc.get("pair");
				String pair = doc.get("id");
				output.print(pair+"\t");
				for(String label:docLabel){
					output.print(label+"\t");
				}
				output.println();
			}
		}
	}
	
	/**
	 * Because the list is ranked by frequency from the most frequent one.
	 * So sub string will be in front of longer string
	 * @param list
	 * @param index
	 * @return
	 */
	public boolean isSub(ArrayList list, int index){
		String pattern = ((Gram)list.get(index)).getGram();
		for(int i=0;i<list.size();i++){
			String patternTemp = ((Gram)list.get(i)).getGram();
			if(isSub(pattern, patternTemp))
				return true;
		}
		return false;
	}
	
	public boolean isSub(String string, String subString){
		String[] chunks = string.split(" ");
		HashSet set = new HashSet(Arrays.asList(chunks));
		String[] chunks2= subString.split(" ");
		HashSet subSet = new HashSet(Arrays.asList(chunks2));
		return set.containsAll(subSet);
		
	}
}
