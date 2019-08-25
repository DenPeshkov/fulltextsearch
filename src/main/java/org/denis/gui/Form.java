package org.denis.gui;

import com.alee.laf.WebLookAndFeel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.denis.files.SearchFiles;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Form extends JFrame {

	private JPanel mPanel;
	private JButton directory;
	private JTextField pathText;
	private JTextField textToSearch;
	private JTree tree;
	private JTextField extension;
	private JButton search;
	private JFileChooser fileChooser = new JFileChooser();

	private final HashMap<TreePath, TreePath> fileTreeExpandedPaths = new HashMap<>();
	DefaultMutableTreeNode treeRootNode = new DefaultMutableTreeNode(null);

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

		((DefaultTreeModel) tree.getModel()).setRoot(null);
		tree.setRootVisible(false);

		directory.addActionListener(e -> {
			fileChooser.setDialogTitle("Choose directory");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fileChooser.showOpenDialog(Form.this);
			if (result == JFileChooser.APPROVE_OPTION)
				pathText.setText(fileChooser.getSelectedFile().toString());
		});

		search.addActionListener(e -> {
			drawTree();

			fileTreeExpandedPaths.clear();
		});

		((DefaultTreeModel) tree.getModel()).setRoot(treeRootNode);

		tree.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				fileTreeExpandedPaths.putIfAbsent(event.getPath(), event.getPath());
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

		if (!pathText.getText().isEmpty()) {
			//background task
			new SwingWorker<Void, Path>() {

				@Override
				protected Void doInBackground() throws Exception {
					//searchFiles.traverseTree(Paths.get(pathText.getText()), extension.getText().isEmpty() ? "*" : extension.getText(), textToSearch.getText(), this::publish);
					int i = 0;
					while (i < 100000) {
						publish(Paths.get("/" + i + ".txt"));
						i++;
						//Thread.sleep(100);
					}
					return null;
				}

				//edt
				@Override
				protected void process(List<Path> files) {
					System.out.println(files);
					for (Path file : files) {
						DefaultMutableTreeNode parentNode = updateTree(file, map);
						((DefaultTreeModel) tree.getModel()).reload(parentNode);
					}
					fileTreeExpandedPaths.keySet().forEach(tree::expandPath);
				}

				@Override
				protected void done() {
					super.done();
				}
			}.execute();
		}
	}

	/*
	Используется оптимизация. Для создания дерева мы дожны добавлять к родительским узлам узлы потомки.
	В данном случае путь проверятеся с конца позволяя уменьшить обращения к хеш таблице.
	То есть путь:
	/home/denis/file/1.txt полностью заполнит хеш таблицу за исключением имени файла, которое не хранится.
	А путь /home/denis/file/2.txt добавит к уже существующему узлу /home/denis/file/ узел 2.txt и завершится, не просматривая дальше узлы.
	Также в хеш таблице не хранятся полные пути к файлам,сокращяя место.
	 */
	private DefaultMutableTreeNode updateTree(Path path, Map<Path, DefaultMutableTreeNode> map) {
		Path root = path.getRoot();

		if (!map.containsKey(root)) {
			DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
			treeRootNode.add(rootNode);
			((DefaultTreeModel) tree.getModel()).reload(treeRootNode);
			map.put(root, rootNode);
		}

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
			WebLookAndFeel.install();
			//UIManager.getFont("Label.font");
			//UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");

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
		mPanel.setLayout(new GridLayoutManager(3, 5, new Insets(0, 0, 0, 0), -1, -1));
		mPanel.putClientProperty("html.disable", Boolean.TRUE);
		final JScrollPane scrollPane1 = new JScrollPane();
		mPanel.add(scrollPane1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(200, 200), new Dimension(200, 600), null, 0, false));
		tree = new JTree();
		scrollPane1.setViewportView(tree);
		search = new JButton();
		search.setText("Search");
		mPanel.add(search, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		directory = new JButton();
		directory.setHorizontalTextPosition(0);
		directory.setLabel("...");
		directory.setOpaque(false);
		directory.setText("...");
		mPanel.add(directory, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		pathText = new JTextField();
		mPanel.add(pathText, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		extension = new JTextField();
		mPanel.add(extension, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		textToSearch = new JTextField();
		mPanel.add(textToSearch, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return mPanel;
	}

}
