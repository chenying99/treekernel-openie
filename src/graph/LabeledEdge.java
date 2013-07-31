package graph;
import org.jgrapht.graph.*;

public class LabeledEdge<V> extends DefaultEdge{
	private V v1;
	private V v2;
	
	private String label;
	
	public LabeledEdge(V v1, V v2, String label){
		this.v1 = v1;
		this.v2 = v2;
		this.label = label;
	}
	
	public V getV1(){
		return this.v1;
	}
	
	public V getV2(){
		return v2;
	}
	
	public String toString(){
		return v1+"->"+v2+"\t"+label;
	}
	
	public String getLabel(){
		return label;
	}
}
