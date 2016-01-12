package de.uni_koeln.spinfo.information_extraction.data;



public class DependencyTree {

	private WordNode root;

	public void setRoot(WordNode root) {
		this.root = root;
	}

	public WordNode getRoot() {
		return root;
	}

	

	public void add(WordNode toAdd, int parentID) {
		add(toAdd, parentID, root);
	}

	private boolean add(WordNode toAdd, int parentID, WordNode node) {
		if (node.getId() == parentID) {
			node.addChild(toAdd);
			toAdd.setParent(node);
			return true;
		} else {
			if (node.getChilds() != null) {
				for (WordNode child : node.getChilds()) {
					add(toAdd, parentID, child);
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(root.getId() + " " + root.getLemma() + " "
				+ root.getMorphTag() + "  " + root.getPosTag() + " "
				+ root.getRole() + "\n");
		if (root.getChilds() != null) {
			for (WordNode child : root.getChilds()) {
				toString(sb, child);
			}
		}
		return sb.toString();
	}

	private void toString(StringBuffer sb, WordNode node) {
		sb.append(node.getId() + " " + node.getLemma() + " "
				+ node.getMorphTag() + " pos: " + node.getPosTag() + " role: "
				+ node.getRole() + " head: " + node.getParent().getId() + "\n");
		if (node.getChilds() != null) {
			for (WordNode child : node.getChilds()) {
				toString(sb, child);
			}
		}
	}

}
