package graph.relation;

import graph.DPPath2TB;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Sort the tree, so the child's value is from left-min to right-max.
 * @author ying
 *
 */
public class SortTree {
	
	/**
	 * Sort the tree, so the child's value is from left-min to right-max.
	 * Note that you have to re-assign the return value to the node.
	 * @param node
	 * @return
	 */
	public static NodewithOrder sort(NodewithOrder node){
		//System.out.println("before sort:");
		//DPPath2TBwithRordered.outputTree(node);
		int childSize = node.getChildCount();
		if(childSize<=1)
			return node;
		NodewithOrder root = new NodewithOrder(node.getUserObject());
		ArrayList list = new ArrayList();
		
		for(int i=0;i<childSize;i++){
			list.add((NodewithOrder)node.getChildAt(i));
		}
		Collections.sort(list);
		for(int i=0;i<childSize;i++){
			NodewithOrder child = (NodewithOrder)list.get(i);
			child = sort(child);
			root.add(child);
		}
		//System.out.println("after sort:");
		//DPPath2TBwithRordered.outputTree(root);
		return root;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NodewithOrder node = new NodewithOrder("a",3);
		NodewithOrder nodeChild1 = new NodewithOrder("b",7);
		nodeChild1.add(new NodewithOrder("b1",5));
		node.add(nodeChild1);
		node.add(new NodewithOrder("c",4));
		NodewithOrder nodeChild = new NodewithOrder("d",8);
		node.add(nodeChild);
		nodeChild.add(new NodewithOrder("dd",1));
		nodeChild.add(new NodewithOrder("de",5));
		nodeChild.add(new NodewithOrder("dh",3));
		node = sort(node);
		DPPath2TB.outputTree(node);
	}

}
