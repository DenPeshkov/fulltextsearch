package org.denis.gui;

import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FilesToTree {
	private FilesToTree() {
	}

	static public DefaultMutableTreeNode getNode(Iterable<Path> paths) {
		DefaultMutableTreeNode treeRootNode = new DefaultMutableTreeNode(null);
		Map<Path, DefaultMutableTreeNode> map = new HashMap<>();
		map.put(null, treeRootNode);
		for (Path path : paths) {

			int index = 1;

			Path root = path.getRoot();  //TODO учитывать общие пути

			if (!map.containsKey(root)) {
				DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
				treeRootNode.add(rootNode);
				map.put(root, rootNode);
			}

			for (int i = path.getNameCount()-1; i >= 1; i--) {
				Path parent = path.subpath(0, i);
				if (map.containsKey(parent)) {
					index = i;
				}
			}

			for (int i = index; i <= path.getNameCount(); i++) {
				Path subpath = root.resolve(path.subpath(0, i));
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(subpath.getFileName());
				DefaultMutableTreeNode nodePrev = map.putIfAbsent(subpath, node);

				if (nodePrev == null) {
					map.getOrDefault(subpath.getParent(), treeRootNode).add(node);
				}
			}
		}
		return treeRootNode;
	}
}
