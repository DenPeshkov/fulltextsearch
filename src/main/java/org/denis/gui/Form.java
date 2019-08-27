package org.denis.gui;

import com.alee.laf.WebLookAndFeel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.denis.files.SearchFiles;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class Form extends JFrame {

	private JPanel mPanel;
	private JButton directory;
	private JTextField pathText;
	private JTextField textToSearch;
	private JTree fileTree;
	private JTextField extension;
	private JButton search;
	private JScrollPane treeScrollPane;
	private JTabbedPane fileContentTabbedPane;
	private JProgressBar fileTreeProgressBar;
	private JFileChooser fileChooser = new JFileChooser();

	private final Set<TreePath> fileTreeExpandedPaths = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final List<FileContentTableScrollPane> fileContentTableScrollPaneList = new ArrayList<>();
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

		((DefaultTreeModel) fileTree.getModel()).setRoot(treeRootNode);
		fileTree.setRootVisible(false);

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

		fileTree.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				fileTreeExpandedPaths.add(event.getPath());
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				fileTreeExpandedPaths.remove(event.getPath());
			}
		});

		fileTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				//int selRow = fileTree.getRowForLocation(e.getX(), e.getY());

				TreePath selPath = fileTree.getPathForLocation(e.getX(), e.getY());

				int tabCount = fileContentTabbedPane.getTabCount();

				for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
					if (((ButtonTabComponent) fileContentTabbedPane.getTabComponentAt(tabIndex)).getFilePath().equals(selPath)) {
						fileContentTabbedPane.setSelectedIndex(tabIndex);
						return;
					}
				}

				//if(selRow != -1) {
				if (selPath != null) {
					if (e.getClickCount() == 1) {
						fileTree.getSelectionModel().clearSelection();
					} else if (e.getClickCount() == 2) {
						if (fileTree.getModel().isLeaf(selPath.getLastPathComponent())) {

							//TODO
							//String filePath = selPath.toString().replaceAll("\\]|\\[", "").replaceFirst("Root, ", "").replaceAll(", ", Matcher.quoteReplacement(File.separator));

							String filePath = selPath.toString().replaceAll("\\]|\\[", "").replaceFirst("Root, ", "").replaceAll(", ", Matcher.quoteReplacement(File.separator));

							System.out.println("filePath = " + filePath);

							ReadBigFileTableModel fileContentTableModel = new ReadBigFileTableModel(filePath);
							FileContentTable fileContentTable = new FileContentTable(fileContentTableModel);

							FileContentTableScrollPane fileContentTableScrollPane = new FileContentTableScrollPane(fileContentTable);
							fileContentTableScrollPane.getFileContentTable().getModel().fireTableDataChanged();

							ButtonTabComponent buttonTabComponent = new ButtonTabComponent(selPath);
							buttonTabComponent.setBackground(Color.WHITE);

							fileContentTabbedPane.add(fileContentTableScrollPane);
							fileContentTabbedPane.setTabComponentAt(fileContentTabbedPane.getTabCount() - 1, buttonTabComponent);
							fileContentTabbedPane.setSelectedIndex(fileContentTabbedPane.getTabCount() - 1);

							fileContentTableScrollPaneList.add(fileContentTableScrollPane);

							for (int i = 0; i < fileContentTableScrollPaneList.size(); i++) {
								System.out.println("index = " + i + " pane = " + ((ButtonTabComponent) fileContentTabbedPane.getTabComponentAt(i)).getFilePath());
							}

							//fileContentTable.changeSelection(100, 1, false, false);
						}
					}
				}
				//}
			}
		});

		fileTreeProgressBar.setVisible(false);
	}

	private void drawTree() {
		Map<Path, DefaultMutableTreeNode> map = new HashMap<>();
		HashSet<DefaultMutableTreeNode> nodeSet = new HashSet<>();

		Path root = Paths.get(pathText.getText()).getRoot();

		//add root
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
		map.put(root, rootNode);
		treeRootNode.add(rootNode);

		((DefaultTreeModel) fileTree.getModel()).reload(treeRootNode);

		//background task
		SwingWorker<Void, DefaultMutableTreeNode> worker = new SwingWorker<Void, DefaultMutableTreeNode>() {

			@Override
			protected Void doInBackground() throws Exception {
				fileTreeProgressBar.setVisible(true);
				fileTreeProgressBar.setIndeterminate(true);
				SearchFiles.traverseTree(Paths.get(pathText.getText()), extension.getText().isEmpty() ? "*" : extension.getText(), textToSearch.getText(), searchFileResult -> publish(updateTree(searchFileResult.getFile(), map)));
				return null;
			}

			//edt
			@Override
			protected void process(List<DefaultMutableTreeNode> nodes) {
				for (DefaultMutableTreeNode file : nodes) {
					((DefaultTreeModel) fileTree.getModel()).reload(file);
				}
				fileTreeExpandedPaths.forEach(fileTree::expandPath);
			}

			@Override
			protected void done() {
				fileTreeProgressBar.setIndeterminate(false);
				fileTreeProgressBar.setVisible(false);
			}
		};
		worker.execute();
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

		DefaultMutableTreeNode prevNode = new DefaultMutableTreeNode(path.getFileName()); //filename unique
		DefaultMutableTreeNode parentNode = treeRootNode;

		for (int i = path.getNameCount(); i >= 1; i--) {
			Path child = root.resolve(path.subpath(0, i));
			Path parent = child.getParent();

			parentNode = map.get(parent);

			if (parentNode == null) {
				parentNode = new DefaultMutableTreeNode(parent.getFileName());
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

			Form form = new Form();
		});
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}


	private class ButtonTabComponent extends JPanel {
		private final TreePath filePath;

		public ButtonTabComponent(TreePath filePath) {
			this.filePath = filePath;
			String tabText = filePath.getLastPathComponent().toString();
			JLabel label = new JLabel(tabText);
			add(label);
			//add more space between the label and the button
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
			//tab button
			JButton button = new TabButton();
			add(button);
			//add more space to the top of the component
			//setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		}

		private class TabButton extends JButton implements ActionListener {
			TabButton() {
				setContentAreaFilled(false);
				setFocusable(false);
				setBorder(BorderFactory.createEtchedBorder());
				setBorderPainted(false);
				setRolloverEnabled(true);
				int size = 17;
				setPreferredSize(new Dimension(size, size));

				URL imgURL = getClass().getClassLoader().getResource("icons/close.png");
				if (imgURL != null) {
					Icon closeIcon = new ImageIcon(imgURL);
					setIcon(closeIcon);
				}

				addActionListener(this);

				//paint tab border
				//TODO не работает
				addMouseListener(new MouseAdapter() {
					public void mouseEntered(MouseEvent e) {
						setBorderPainted(true);
					}

					public void mouseExited(MouseEvent e) {
						setBorderPainted(false);
					}
				});
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				int tabIndex = fileContentTabbedPane.indexOfTabComponent(ButtonTabComponent.this);
				System.out.println("close button index = " + tabIndex);
				if (tabIndex != -1) {
					fileContentTableScrollPaneList.get(tabIndex).getFileContentTable().getModel().close();
					fileContentTabbedPane.remove(tabIndex);
					fileContentTableScrollPaneList.remove(tabIndex);
				}
			}

		}

		public TreePath getFilePath() {
			return filePath;
		}
	}

	private static class FileContentTableScrollPane extends JScrollPane {
		public FileContentTableScrollPane(FileContentTable fileContentTable) {
			super(fileContentTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			getVerticalScrollBar().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (fileContentTable.getRowCount() > 5000) {
						System.out.println("Released");
						fileContentTable.getModel().isMousePressed = false;
						fileContentTable.getModel().fireTableDataChanged();
					}
				}
			});
			getVerticalScrollBar().addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (fileContentTable.getRowCount() > 5000) {
						System.out.println("Pressed");
						fileContentTable.getModel().isMousePressed = true;
					}
				}
			});

			getHorizontalScrollBar().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (fileContentTable.getRowCount() > 5000) {
						System.out.println("Released");
						fileContentTable.getModel().isMousePressed = false;
						fileContentTable.getModel().fireTableDataChanged();
					}
				}
			});
			getHorizontalScrollBar().addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (fileContentTable.getRowCount() > 5000) {
						System.out.println("Pressed");
						fileContentTable.getModel().isMousePressed = true;
					}
				}
			});

			//getViewport().setBackground(Color.WHITE);
		}

		FileContentTable getFileContentTable() {
			return (FileContentTable) getViewport().getView();
		}
	}

	private static class FileContentTable extends JTable {
		FileContentTable(ReadBigFileTableModel fileContentTableModel) {
			super(fileContentTableModel);

			setShowGrid(false);
			setIntercellSpacing(new Dimension(5, 0));
			setTableHeader(null);
			//getColumnModel().getColumn(0).setPreferredWidth(50);
			//getColumnModel().getColumn(0).setMaxWidth(50);
			setAutoscrolls(true);
			getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Component rendererComp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					rendererComp.setBackground(new Color(228, 228, 228));
					rendererComp.setForeground(new Color(128, 128, 128));
					setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
					setHorizontalAlignment(JLabel.CENTER);
					return rendererComp;
				}
			});
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			adjustColumnsSize();
		}

		private void adjustColumnsSize() {
			for (int colIndex = 0; colIndex < 2; colIndex++) {
				TableColumn tableColumn = getColumnModel().getColumn(colIndex);
				int preferredWidth = tableColumn.getMinWidth();
				int row;

				if (colIndex == 0)
					row = getModel().getRowCount() - 1;
				else
					row = getModel().getNumOfLongestRow();

				System.out.println("longest row = " + row);

				TableCellRenderer cellRenderer = getCellRenderer(row, colIndex);
				Component c = prepareRenderer(cellRenderer, row, colIndex);
				int width = c.getPreferredSize().width + getIntercellSpacing().width;
				preferredWidth = Math.max(preferredWidth, width);

				tableColumn.setPreferredWidth(preferredWidth);
			}
		}

		@Override
		public ReadBigFileTableModel getModel() {
			return (ReadBigFileTableModel) super.getModel();
		}
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
		mPanel.setLayout(new GridLayoutManager(5, 4, new Insets(5, 5, 5, 5), -1, -1));
		mPanel.putClientProperty("html.disable", Boolean.TRUE);
		treeScrollPane = new JScrollPane();
		mPanel.add(treeScrollPane, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(200, 200), new Dimension(200, 600), null, 0, false));
		fileTree = new JTree();
		fileTree.setEnabled(true);
		treeScrollPane.setViewportView(fileTree);
		pathText = new JTextField();
		pathText.setEditable(false);
		pathText.setEnabled(true);
		pathText.setText("");
		mPanel.add(pathText, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		extension = new JTextField();
		mPanel.add(extension, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
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
		fileContentTabbedPane = new JTabbedPane();
		mPanel.add(fileContentTabbedPane, new GridConstraints(3, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
		fileTreeProgressBar = new JProgressBar();
		fileTreeProgressBar.setStringPainted(false);
		mPanel.add(fileTreeProgressBar, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return mPanel;
	}

}