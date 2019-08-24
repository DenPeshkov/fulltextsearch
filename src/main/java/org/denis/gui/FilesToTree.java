package org.denis.gui;

import org.denis.files.TreeObserver;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FilesToTree implements TreeObserver {
	private JTree tree;
	private DefaultMutableTreeNode treeRootNode = new DefaultMutableTreeNode(null);
	private Map<Path, DefaultMutableTreeNode> map = new HashMap<>();


	FilesToTree(JTree tree) {
		this.tree = tree;
		map.put(null, treeRootNode);
		((DefaultTreeModel) tree.getModel()).setRoot(treeRootNode);
	}

	@Override
	public void updateTree(Path path) {
		DefaultMutableTreeNode node = null;

		Path root = path.getRoot();

		if (!map.containsKey(root)) {
			DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
			treeRootNode.add(rootNode);
			map.put(root, rootNode);
		}

		DefaultMutableTreeNode prevNode = new DefaultMutableTreeNode(path); //filename unique

		for (int i = path.getNameCount(); i >= 1; i--) {
			Path child = root.resolve(path.subpath(0, i));
			Path parent = child.getParent();

			DefaultMutableTreeNode parentNode = map.get(parent);

			if (parentNode == null) {
				parentNode = new DefaultMutableTreeNode(parent);
				parentNode.add(prevNode);
				map.put(parent, parentNode);
				prevNode = parentNode;
			} else {
				parentNode.add(prevNode);
				break;
			}
		}
		//tree.updateUI();
		((DefaultTreeModel) tree.getModel()).reload();
	}
}
