package org.denis.gui;

import com.alee.laf.WebLookAndFeel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.denis.files.SearchFiles;
import sun.reflect.generics.tree.Tree;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Form extends JFrame {

	private JPanel mPanel;
	private JButton directory;
	private JTextField pathText;
	private JTextField textToSearch;
	private JTree tree;
	private JTextField extension;
	private JButton search;
	private JTable textEditor;
	private JScrollPane textEditorScrollPane;
	private JScrollPane treeScrollPane;
	private JFileChooser fileChooser = new JFileChooser();

	private final Set<TreePath> fileTreeExpandedPaths = new HashSet<>();
	private DefaultMutableTreeNode treeRootNode = new DefaultMutableTreeNode(null);

	public Form() {
		$$$setupUI$$$();
		setContentPane(mPanel);
		setVisible(true);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.pack();
		//this.setLocationByPlatform(true);
		this.setMinimumSize(this.getSize());
		this.setVisible(true);
		this.pack();

		mPanel.setMinimumSize(this.getSize());

		extension.setText(".log");
		mPanel.setMinimumSize(new Dimension(1000, 1000));

		pathText.setText("Search ...");

		((DefaultTreeModel) tree.getModel()).setRoot(treeRootNode);
		tree.setRootVisible(false);

		directory.addActionListener(e -> {
			fileChooser.setDialogTitle("Choose directory");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fileChooser.showOpenDialog(Form.this);
			if (result == JFileChooser.APPROVE_OPTION)
				pathText.setText(fileChooser.getSelectedFile().toString());
		});

		search.addActionListener(e -> {
			if (!pathText.getText().equals("Search ...")) {
				treeRootNode.removeAllChildren();
				drawTree();
			}

			fileTreeExpandedPaths.clear();
		});

		tree.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				fileTreeExpandedPaths.add(event.getPath());
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				fileTreeExpandedPaths.remove(event.getPath());
			}
		});
	}

	private void drawTree() {
		Map<Path, DefaultMutableTreeNode> map = new HashMap<>();
		SearchFiles searchFiles = new SearchFiles();
		HashSet<DefaultMutableTreeNode> nodeSet = new HashSet<>();

		Path root = Paths.get(pathText.getText()).getRoot();

		//add root
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
		map.put(root, rootNode);
		treeRootNode.add(rootNode);

		((DefaultTreeModel) tree.getModel()).reload(treeRootNode);

		//background task
		new SwingWorker<Void, DefaultMutableTreeNode>() {

			@Override
			protected Void doInBackground() throws Exception {
				searchFiles.traverseTree(Paths.get(pathText.getText()), extension.getText().isEmpty() ? "*" : extension.getText(), textToSearch.getText(), path -> publish(updateTree(path, map)));
				return null;
			}

			//edt
			@Override
			protected void process(List<DefaultMutableTreeNode> nodes) {
				//System.out.println(files);
				for (DefaultMutableTreeNode file : nodes) {
					((DefaultTreeModel) tree.getModel()).reload(file);
				}
				//System.out.println(Thread.currentThread());
				fileTreeExpandedPaths.forEach(tree::expandPath);
			}

			@Override
			protected void done() {
				super.done();
			}
		}.execute();
	}

	/*
	Используется оптимизация. Для создания дерева мы дожны добавлять к родительским узлам узлы потомки.
	В данном случае путь проверятеся с конца, позволяя уменьшить обращения к хеш таблице.
	То есть путь:
	/home/denis/file/1.txt полностью заполнит хеш таблицу за исключением имени файла, которое не хранится.
	А путь /home/denis/file/2.txt добавит к уже существующему узлу /home/denis/file/ узел 2.txt и завершится, не просматривая дальше узлы.
	Также в хеш таблице не хранятся полные пути к файлам,сокращяя место.
	 */
	private DefaultMutableTreeNode updateTree(Path path, Map<Path, DefaultMutableTreeNode> map) {
		Path root = path.getRoot();

		DefaultMutableTreeNode prevNode = new DefaultMutableTreeNode(path); //filename unique
		DefaultMutableTreeNode parentNode = treeRootNode;

		for (int i = path.getNameCount(); i >= 1; i--) {
			Path child = root.resolve(path.subpath(0, i));
			Path parent = child.getParent();

			parentNode = map.get(parent);

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

		return parentNode;
	}

	public static void main(String[] args) {
		//edt
		SwingUtilities.invokeLater(() -> {
			//WebLookAndFeel.install();

			Form form = new Form();
		});
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		mPanel = new JPanel();
		mPanel.setLayout(new GridLayoutManager(4, 4, new Insets(5, 5, 5, 5), -1, -1));
		mPanel.putClientProperty("html.disable", Boolean.TRUE);
		treeScrollPane = new JScrollPane();
		mPanel.add(treeScrollPane, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(200, 200), new Dimension(200, 600), null, 0, false));
		tree = new JTree();
		treeScrollPane.setViewportView(tree);
		pathText = new JTextField();
		pathText.setEditable(false);
		pathText.setEnabled(true);
		mPanel.add(pathText, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		extension = new JTextField();
		mPanel.add(extension, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		directory = new JButton();
		directory.setHorizontalTextPosition(0);
		directory.setLabel("Browse ...");
		directory.setOpaque(false);
		directory.setText("Browse ...");
		mPanel.add(directory, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label1 = new JLabel();
		label1.setText("Extension");
		mPanel.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label2 = new JLabel();
		label2.setText("Text to find");
		mPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		textToSearch = new JTextField();
		mPanel.add(textToSearch, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		search = new JButton();
		search.setText("Search");
		mPanel.add(search, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		textEditorScrollPane = new JScrollPane();
		textEditorScrollPane.setEnabled(true);
		mPanel.add(textEditorScrollPane, new GridConstraints(3, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		textEditor = new JTable();
		textEditorScrollPane.setViewportView(textEditor);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return mPanel;
	}

}