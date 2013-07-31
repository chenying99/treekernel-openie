package graph;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

public class TestGraphP {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		testGraphLabel();
	}
	
	public static void testGraphLabel(){
		UndirectedGraph<String, LabeledEdge> g =
	            new SimpleGraph<String, LabeledEdge>(LabeledEdge.class);

	        String v1 = "v1";
	        String v2 = "v2";
	        String v3 = "v3";
	        String v4 = "v4";

	        // add the vertices
	        g.addVertex(v1);
	        g.addVertex(v2);
	        g.addVertex(v3);
	        g.addVertex(v4);

	        // add edges to create a circuit
	        g.addEdge(v1, v2, new LabeledEdge(v1, v2, "nn"));
	        g.addEdge(v2, v3, new LabeledEdge(v2,v3, "conj"));
	        g.addEdge(v4, v1, new LabeledEdge(v4,v1, "vb"));

	        java.util.List list = DijkstraShortestPath.findPathBetween(g, "v1", "v4");
	        for(int i=0;i<list.size();i++){
	        	DefaultEdge edge = (DefaultEdge)list.get(i);
	        	System.out.println(edge.toString());
	        }
	        System.out.println("contains v1<=> v3?" +g.containsEdge("v1", "v3"));
	        System.out.println("contains v2<=> v1?" +g.containsEdge("v2", "v1"));
	}
}
