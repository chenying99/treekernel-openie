package stanford;

import java.util.ArrayList;

/**
 * transfer the stanford NE tag form to OpenNLP chunk form for reverb usage.
 * Here I only care about LOCATION, PERSON, ORGANIZATION.
 * @author ying
 *
 */
public class StanfNE2Chunk {

	public static ArrayList ne2Chunk(ArrayList neList){
		ArrayList chunkList = new ArrayList();
		String lastNe = "O";
		for(int i=0;i<neList.size();i++){
			String ne = (String)neList.get(i);
			if(ne.equals("PERSON") || ne.equals("ORGANIZATION") || ne.equals("LOCATION")){
				if(!lastNe.equals(ne))
					chunkList.add("B-NE");
				else
					chunkList.add("I-NE");
			}else
				chunkList.add("O");
			lastNe = ne;

		}
		return chunkList;
	}
}
