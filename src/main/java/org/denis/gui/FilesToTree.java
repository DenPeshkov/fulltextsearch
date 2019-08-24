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

	/*
	Используется оптимизация. Для создания дерева мы дожны добавлять к родительским узлам узлы потомки.
	В данном случае путь проверятеся с конца позволяя уменьшить обращения к хеш таблице.
	То есть путь:
	/home/denis/file/1.txt полностью заполнит хеш таблицу за исключением имени файла, которое не хранится.
	А путь /home/denis/file/2.txt добавит к уже существующему узлу /home/denis/file/ узел 2.txt и завершится, не просматривая дальше узлы.
	Также в хеш таблице не хранятся полные пути к файлам,сокращяя место.
	 */
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
		((DefaultTreeModel) tree.getModel()).reload();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
