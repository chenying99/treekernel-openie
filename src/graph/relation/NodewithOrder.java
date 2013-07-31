package graph.relation;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * I need to sort the tree node, so the node can not be a string any more.
 * @author ying
 *
 */
public class NodewithOrder extends DefaultMutableTreeNode implements Comparable<NodewithOrder>{
	
	Node node = null;
	
	public NodewithOrder(String name, int index){
		this.node = new Node(name,index);
		super.userObject = node;
	}
	
	public NodewithOrder(Object node){
		this.node = (Node)node;
		super.userObject = node;
	}
	
	@Override
	public int compareTo(NodewithOrder other) {
		return this.node.compareTo(other.node);
	}
	
	public String toString(){
		return this.node.toString();
	}
	
	public int getSentIndex(){
		return this.node.index;
	}
	
	
	class Node  implements Comparable<Node>{
		String name;
		int index; //the token index, this is how the node is sorted.


		public Node(String name, int index){
			this.name = name;
			this.index = index;
		}

		public String getName(){return this.name;}
		public int getIndex(){return this.index;}

		@Override
		public String toString(){return this.name;}



		@Override
		public int compareTo(Node other) {
			if(index<other.index)
				return -1;
			else if(index>other.index)
				return 1;
			else
				return 0;
		}
	}

	

}
