package patternEX.lucene;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.oro.text.regex.MalformedPatternException;

/**
 * It is used for noise filter, feature extraction. When you find the pair, instance sentence, the offset of the pair,
 * you can use this to get exactly the pattern's offset, type.
 * So it should have the same pattern extraction with PhrasePAnalyzer.
 * @author ying
 *
 */
public class PhrasePAnalyzerWithOffset extends Analyzer {

	
	HashSet stopWords;
	int nGram = 0;
	int btSize = 0;//ignore side words when the between size >=btSize
	public PhrasePAnalyzerWithOffset(int n, String stopFile, int btSize) throws IOException{
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
			stream = new PhrasePTokenizerWithOffset(reader, this.stopWords,this.nGram, this.btSize);
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
		/*String before = "percent/NN/percent of/IN/of respondents/NNS/respondent to/TO/to an/DT/a Asahi/NNP/Asahi newspaper/NN/newspaper telephone/NN/telephone survey/NN/survey conducted/VBN/conduct";
		String after = "'s/POS/'s large/JJ/large government/NN/government ././. ";
		String between = "said/VBD/say they/PRP/they supported/VBD/support";
		String sent = before +"<ORG>"+between+"<PER>"+after+"<OFFSET>15-16;20-30";*/
		String sent = "Several/JJ/several women/NNS/woman ,/,/, including/VBG/include<PER>,/,/,<PER>'s/POS/'s wife/NN/wife ,/,/, and/CC/and<OFFSET>4-5;7-7";
		String stopFile = "smallStopWords.txt";
		Analyzer contextAnalyzer = new PhrasePAnalyzerWithOffset(4, stopFile, 5);
		StringReader reader = new StringReader(sent); 
		TokenStream tokens = contextAnalyzer.tokenStream("context",reader);
		TermAttribute termAtt = (TermAttribute) tokens.addAttribute(TermAttribute.class);
		PatternTypeAttribute typeAtt = (PatternTypeAttribute) tokens.addAttribute(PatternTypeAttribute.class);
		PatternOffsetAttribute offsetAtt = (PatternOffsetAttribute)tokens.addAttribute(PatternOffsetAttribute.class);
		tokens.reset();
		// print all tokens until stream is exhausted
		while (tokens.incrementToken()) {
			System.out.println("token:"+termAtt.term());
			System.out.println("type:"+typeAtt.getType());
			System.out.println("offset:"+offsetAtt.getOffset());
		}
		tokens.end();
		tokens.close();

	}

}
