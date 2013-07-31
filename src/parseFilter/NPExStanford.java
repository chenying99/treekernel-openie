package parseFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import stanford.Entity;
import stanford.Mention;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class NPExStanford {
	
	StanfordCoreNLP pipeline =null;
	
	public NPExStanford(){
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma, ner");
		pipeline = new StanfordCoreNLP(props);
		
	}
	public NPExStanford(StanfordCoreNLP apipeline){
		this.pipeline = apipeline;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String text = "Rudolph Agnew , 55 years old and former chairman of Consolidated Gold Fields PLC , was named a nonexecutive director of this British industrial conglomerate . He likes meat ."; // Add your text here!
		NPExStanford extractor = new NPExStanford();
		ArrayList Nps = new ArrayList();
		ArrayList tokenList = new ArrayList();
		extractor.extract(text,tokenList, Nps);
	}
	
	/**
	 * Input plain sentence text.
	 * return tokenList, Nps, and mention arrayList.
	 * element of Nps is NPOffset, which has start index, end index of entity.
	 * element of mention arrayList is Mention, has offset, Entity etc. all the informations.
	 * @param text
	 */
	public ArrayList extract(String text, ArrayList tokenList, ArrayList Nps){
			// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int sentStart = 0;
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		for(CoreMap sentence: sentences) {
			ArrayList posList = new ArrayList();
			this.extractOneSent(sentStart, mentions, tokenList, posList, sentence);
			sentStart = tokenList.size();
		}
		for(int i=0;i<mentions.size();i++){
			Mention mention = mentions.get(i);
			System.out.println(mention.getChunk()+"\t"+mention.getType()+"\t"+mention.getStartToken()+"\t"+mention.getEndToken());
			Nps.add(new NPOffset(mention.getStartToken(),mention.getEndToken()));
		}
		return mentions;
	}
	
	/**
	 * Input annotated sentence text: document
	 * return tokenList, and mention arrayList.
	 * element of mention arrayList is Mention, has offset, Entity etc. all the informations.
	 * @param text
	 */
	public static ArrayList extract(Annotation document, ArrayList tokenList){
		
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int sentStart = 0;
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		for(CoreMap sentence: sentences) {
			ArrayList posList = new ArrayList();
			NPExStanford.extractOneSent(sentStart, mentions, tokenList, posList, sentence);
			sentStart = tokenList.size();
		}
		return mentions;
	}
	
	/**
	 * mentions and tokenList are for the whole text.
	 * posList is for the current sentence.
	 * @param sentStart
	 * @param mentions
	 * @param tokenList
	 * @param posList
	 * @param sentence
	 */
	public static void extractOneSent(int sentStart,ArrayList mentions, ArrayList tokenList, ArrayList posList,CoreMap sentence){
		// traversing the words in the current sentence
		// a CoreLabel is a CoreMap with additional token-specific methods
		ArrayList neList = new ArrayList();
		for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			tokenList.add(word);
			String pos = token.get(PartOfSpeechAnnotation.class);
			posList.add(pos);
			// this is the NER label of the token
			String ne = token.get(NamedEntityTagAnnotation.class);      
			neList.add(ne);
			//System.out.println(word+"/"+pos+"/"+ne);
		}


		int position=0;
		String name = "";

		String lastNe = "O";
		int startEntity = 0;
		String type = "";

		for(int i=0;i<neList.size();i++){
			String ne = (String)neList.get(i);
			String word = (String)tokenList.get(i+sentStart);
			// keep track of mentions
			if(lastNe.equals("O")){
				if(!ne.equals("O")){
					startEntity = position;
					name = word;
					type = ne;
				}
			}else{
				if(ne.equals("O")){
					int endEntity = position-1;
					Entity entity = createEntity(name, lastNe);
					Mention mention = new Mention(entity, lastNe, startEntity+sentStart, endEntity+sentStart,tokenList);
					mentions.add(mention);
				}else{
					if(ne.equals(lastNe)){
						name += " " + word;
					}
				}

				if(!ne.equals(lastNe) && !ne.equals("O")){
					int endEntity = position-1;
					Entity entity = createEntity(name,  lastNe);
					Mention mention = new Mention(entity,lastNe, startEntity+sentStart, endEntity+sentStart,tokenList);
					mentions.add(mention);
					startEntity=position;
					name = word;
				}

			}

			//System.out.println(word + "\t" + lemma + "\t" + pos + "\t" + ne);
			lastNe = ne;
			position++;

		}

		// verify mention ending at the last token
		if(!lastNe.equals("O") && !lastNe.equals(".")){
			int endEntity = position-1;
			Entity entity = createEntity(name,  lastNe);
			Mention mention = new Mention(entity, lastNe,startEntity+sentStart, endEntity+sentStart, tokenList);
			mentions.add(mention);
		}
	}

	/**
	 * Input plain sentence text. get the entities, includes pronouns. 
	 * return tokenList, Nps, and mention arrayList.
	 * element of Nps is NPOffset, which has start index, end index of entity.
	 * element of mention arrayList is Mention, has offset, Entity etc. all the informations.
	 * @param text
	 */
	public ArrayList extractWithProp(String text, ArrayList tokenList, ArrayList Nps){
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int sentStart = 0;
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		for(CoreMap sentence: sentences) {
			//System.out.println(sentence);
			ArrayList posList = new ArrayList();
			
			this.extractOneSentWithProp(sentStart, mentions, tokenList, posList, sentence);
			
			sentStart =tokenList.size();
		}
		for(int i=0;i<mentions.size();i++){
			Mention mention = mentions.get(i);
			//System.out.println(mention.getChunk()+"\t"+mention.getType()+"\t"+mention.getStartToken()+"\t"+mention.getEndToken());
			Nps.add(new NPOffset(mention.getStartToken(),mention.getEndToken()));
		}
		return mentions;
	}
	
	/**
	 * Input: annotated sentence text: document. get the entities, includes pronouns. 
	 * return tokenList, Nps, and mention arrayList.
	 * element of mention arrayList is Mention, has offset, Entity etc. all the informations.
	 * @param text
	 */
	public static ArrayList extractWithProp(Annotation document, ArrayList tokenList){
		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int sentStart = 0;
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		for(CoreMap sentence: sentences) {
			//System.out.println(sentence);
			ArrayList posList = new ArrayList();
			
			NPExStanford.extractOneSentWithProp(sentStart, mentions, tokenList, posList, sentence);
			
			sentStart =tokenList.size();
		}
		return mentions;
	}
	
	public static void extractOneSentWithProp(int sentStart,ArrayList mentions, ArrayList tokenList, ArrayList posList, CoreMap sentence){
		ArrayList tempMentions = new ArrayList();
		NPExStanford.extractOneSent(sentStart, tempMentions, tokenList, posList, sentence);
		for(int i=0;i<posList.size();i++){
			String pos = (String)posList.get(i);
			String word = (String)tokenList.get(i+sentStart);
			//System.out.println(word+":"+pos);
			if(pos.equals("PRP") || pos.equals("PRP$")){
				Entity entity = createEntity(word.toLowerCase(), "PERSON");
				Mention mention = new Mention(entity, "PERSON", i+sentStart, i+sentStart,tokenList);
				int index=0;
				for(;index<tempMentions.size();index++){
					Mention temp = (Mention)tempMentions.get(index);
					if(temp.getStartToken()>i+sentStart){
						break;
					}
				}
				if(index<tempMentions.size())
					tempMentions.add(index,mention);
				else
					tempMentions.add(mention);
			}
		}
		mentions.addAll(tempMentions);
	}
	
	public static Entity createEntity(String name, String stanfordType){
		
		String myType=Entity.NONE;
		
		if(stanfordType.equals("PERSON"))
			myType = Entity.PERSON;
		
		if(stanfordType.equals("ORGANIZATION"))
			myType = Entity.ORGANIZATION;
		
		if(stanfordType.equals("LOCATION"))
			myType = Entity.LOCATION;
		
		if(stanfordType.equals("DATE"))
			myType = Entity.DATE;
		
		if(stanfordType.equals("GPE"))
			myType = Entity.GPE;
		
		if(stanfordType.equals("MISC"))
			myType = Entity.MISC;

		if(stanfordType.equals("MONEY"))
			myType = Entity.MONEY;
		
		if(stanfordType.equals("PERCENT"))
			myType = Entity.PERCENT;
		
		if(stanfordType.equals("TIME"))
			myType = Entity.TIME;
		
		
		String id = name.toUpperCase() + "#" + myType;
		
		return new Entity(id, name, myType);
	}

}
