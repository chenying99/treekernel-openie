package patternEX.lucene;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.oro.text.regex.MalformedPatternException;

public class PhrasePAnalyzerWithOffsetwithDup extends PhrasePAnalyzerWithOffset{

	public PhrasePAnalyzerWithOffsetwithDup(int n, String stopFile, int btSize)
			throws IOException {
		super(n, stopFile, btSize);
		// TODO Auto-generated constructor stub
	}
	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream stream = null;
		try {
			stream = new PhrasePTokenizerWithOffsetwithDup(reader, this.stopWords,this.nGram, this.btSize);
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
		//String sent = "Several/JJ/several women/NNS/woman ,/,/, including/VBG/include<PER>,/,/,<PER>'s/POS/'s wife/NN/wife ,/,/, and/CC/and<OFFSET>4-5;7-7";
		//String sent = "<PER>has/VBZ/have notified/VBN/notify<PER>that/IN/that it/PRP/it would/MD/would ``/``/`` shortly/RB/shortly respond/VB/respond<OFFSET>0-0;3-3";
		String sent = "on/IN/on a/DT/a revised/VBN/revise tender/NN/tender offer/NN/offer by/IN/by<PER>to/TO/to buy/VB/buy<PER>and/CC/and has/VBZ/have asked/VBN/ask for/IN/for clarification/NN/clarification of/IN/of<OFFSET>16-19;22-22";
		String stopFile = "smallStopWords.txt";
		Analyzer contextAnalyzer = new PhrasePAnalyzerWithOffsetwithDup(4, stopFile, 5);
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
