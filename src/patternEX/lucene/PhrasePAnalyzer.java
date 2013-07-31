package patternEX.lucene;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.oro.text.regex.MalformedPatternException;

public class PhrasePAnalyzer  extends Analyzer {
	
	
	HashSet stopWords;
	int nGram = 0;
	int btSize = 0;
	public PhrasePAnalyzer(int n, String stopFile, int btSize) throws IOException{
		this.nGram = n;
		
		this.stopWords = new HashSet();
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(stopFile)));
		String line = input.readLine();
		while(line!=null){
			this.stopWords.add(line);
			line = input.readLine();
		}
		input.close();
		String punc = "-'`.!?;/\\\"[]<>{}&()";//remain , : ()
		for(int i=0;i<punc.length();i++)
			this.stopWords.add(punc.substring(i,i+1));
		this.btSize = btSize;
	}
	
	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream stream = null;
		try {
			stream = new PhrasePTokenizer(reader, this.stopWords,this.nGram, this.btSize);
		} catch (MalformedPatternException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stream;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String before = "percent/NN/percent of/IN/of respondents/NNS/respondent to/TO/to an/DT/a Asahi/NNP/Asahi newspaper/NN/newspaper telephone/NN/telephone survey/NN/survey conducted/VBN/conduct";
		String after = "'s/POS/'s large/JJ/large government/NN/government ././. ";
		String between = "said/VBD/say they/PRP/they supported/VBD/support";
		String sent = before +"<ORG>"+between+"<PER>"+after;
		String stopFile = "smallStopWords.txt";
		Analyzer contextAnalyzer = new PhrasePAnalyzer(3, stopFile, 5);
		StringReader reader = new StringReader(sent); 
		TokenStream tokens = contextAnalyzer.tokenStream("context",reader);
		TermAttribute termAtt = (TermAttribute) tokens.addAttribute(TermAttribute.class);
		
		tokens.reset();
		// print all tokens until stream is exhausted
		while (tokens.incrementToken()) {
			System.out.println("token:"+termAtt.term());
		}
		tokens.end();
		tokens.close();

	}

}
