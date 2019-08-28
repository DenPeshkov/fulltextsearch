package org.denis.gui;

import com.alee.laf.WebLookAndFeel;
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
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

class Form extends JFrame {

	private JPanel mainWindow;
	private JButton browseButton;
	private JTextField filePath;
	private JTextField textToSearch;
	private JTree fileTree;
	private JTextField extension;
	private JButton searchButton;
	private JScrollPane treeScrollPane;
	private JTabbedPane fileContentTabbedPane;
	private JProgressBar fileTreeProgressBar;
	private JButton cancelButton;
	private final JFileChooser fileChooser = new JFileChooser();

	private final Set<TreePath> fileTreeExpandedPaths = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final List<FileContentTableScrollPane> fileContentTableScrollPaneList = new ArrayList<>();
	private final DefaultMutableTreeNode treeRootNode = new DefaultMutableTreeNode(null);
	private final Map<Path, DefaultMutableTreeNode> map = new HashMap<>();
	private boolean isWorkerDone = false;
	private SwingWorker<Void, DefaultMutableTreeNode> worker = null;

	private Form() {
		setContentPane(mainWindow);
		setVisible(true);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.pack();
		this.setVisible(true);
		this.pack();

		this.setPreferredSize(new Dimension(1000, 1000));
		this.setMinimumSize(new Dimension(1000, 1000));

		extension.setText(".log");

		filePath.setText("Search ...");

		((DefaultTreeModel) fileTree.getModel()).setRoot(treeRootNode);
		fileTree.setRootVisible(false);

		cancelButton.setEnabled(false);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				fileContentTableScrollPaneList.stream()
						.map(FileContentTableScrollPane::getFileContentTable)
						.map(FileContentTable::getModel)
						.forEach(ReadBigFileTableModel::close);
				e.getWindow().dispose();
			}
		});

		browseButton.addActionListener(e -> {
			fileChooser.setDialogTitle("Choose browseButton");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fileChooser.showOpenDialog(Form.this);
			if (result == JFileChooser.APPROVE_OPTION)
				filePath.setText(fileChooser.getSelectedFile().toString());
		});

		cancelButton.addActionListener(e -> isWorkerDone = true);

		searchButton.addActionListener(e -> {

			if (!filePath.getText().equals("Search ...")) {

				map.clear();
				fileTreeExpandedPaths.clear();
				treeRootNode.removeAllChildren();
				((DefaultTreeModel) fileTree.getModel()).reload(treeRootNode);

				worker = new SwingWorker<Void, DefaultMutableTreeNode>() {

					//выполняетя поиск файлов и заполение дерева
					@Override
					protected Void doInBackground() throws Exception {
						fileTreeProgressBar.setVisible(true);
						fileTreeProgressBar.setIndeterminate(true);
						searchButton.setEnabled(false);
						browseButton.setEnabled(false);
						cancelButton.setEnabled(true);

						SearchFiles.traverseTree(Paths.get(filePath.getText()), extension.getText().isEmpty() ? "*" : extension.getText(), textToSearch.getText(), searchFileResult -> {
							//обрабатываем файлы больше 2Gb
							if (searchFileResult.isExceedSize())
								System.out.println("File is too big");
							else
								publish(updateTree(searchFileResult.getFile()));
						}, () -> isWorkerDone);
						return null;
					}

					//перерисовка дерева
					@Override
					protected void process(List<DefaultMutableTreeNode> nodes) {
						if (!isDone()) {
							for (DefaultMutableTreeNode file : nodes) {
								((DefaultTreeModel) fileTree.getModel()).reload(file);
							}
							fileTreeExpandedPaths.forEach(fileTree::expandPath);
						}
					}

					@Override
					protected void done() {
						fileTreeProgressBar.setIndeterminate(false);
						fileTreeProgressBar.setVisible(false);
						searchButton.setEnabled(true);
						browseButton.setEnabled(true);
						isWorkerDone = false;
						cancelButton.setEnabled(false);
					}
				};

				worker.execute();
			}
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

							String filePath = "";

							for (int pathCompIndex = 1; pathCompIndex < selPath.getPathCount(); pathCompIndex++) {
								filePath = filePath.concat(selPath.getPathComponent(pathCompIndex).toString());
								if (pathCompIndex < selPath.getPathCount() - 1)
									filePath = filePath.concat(Matcher.quoteReplacement(File.separator));
							}

							if (!filePath.equals("")) {

								ReadBigFileTableModel fileContentTableModel = new ReadBigFileTableModel(filePath);
								FileContentTable fileContentTable = new FileContentTable(fileContentTableModel);

								FileContentTableScrollPane fileContentTableScrollPane = new FileContentTableScrollPane(fileContentTable);
								fileContentTableScrollPane.getFileContentTable().getModel().fireTableDataChanged();

								ButtonTabComponent buttonTabComponent = new ButtonTabComponent(selPath);
								buttonTabComponent.setBackground(null);

								fileContentTabbedPane.add(fileContentTableScrollPane);
								fileContentTabbedPane.setTabComponentAt(fileContentTabbedPane.getTabCount() - 1, buttonTabComponent);
								fileContentTabbedPane.setSelectedIndex(fileContentTabbedPane.getTabCount() - 1);

								fileContentTableScrollPaneList.add(fileContentTableScrollPane);
							}

						}
					}
				}
				//}
			}
		});

		fileTreeProgressBar.setVisible(false);

		initializeFileContentTabbedPane();
	}

	/*
	Используется оптимизация. Для создания дерева мы дожны добавлять к родительским узлам узлы потомки.
	В данном случае путь проверятеся с конца, позволяя уменьшить обращения к хеш таблице.
	То есть путь:
	/home/denis/file/1.txt полностью заполнит хеш таблицу за исключением имени файла, которое не хранится.
	А путь /home/denis/file/2.txt добавит к уже существующему узлу /home/denis/file/ узел 2.txt и завершится, не просматривая дальше узлы.
	Также в хеш таблице не хранятся полные пути к файлам,сокращяя место.
	 */
	private DefaultMutableTreeNode updateTree(Path path) {
		Path root = path.getRoot();

		if (!map.containsKey(root)) {
			DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
			map.put(root, rootNode);
			treeRootNode.add(rootNode);
			((DefaultTreeModel) fileTree.getModel()).reload(treeRootNode);
		}

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


	private class ButtonTabComponent extends JPanel {
		private final TreePath filePath;

		ButtonTabComponent(TreePath filePath) {
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

				if (tabIndex != -1) {
					fileContentTableScrollPaneList.get(tabIndex).getFileContentTable().getModel().close();
					fileContentTabbedPane.remove(tabIndex);
					fileContentTableScrollPaneList.remove(tabIndex);
				}
			}

		}

		TreePath getFilePath() {
			return filePath;
		}
	}

	private static class FileContentTableScrollPane extends JScrollPane {
		FileContentTableScrollPane(FileContentTable fileContentTable) {
			super(fileContentTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			final int MAX_FILE_SIZE = 10_000;

			getVerticalScrollBar().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (fileContentTable.getModel().getFileSize() > MAX_FILE_SIZE) {
						fileContentTable.getModel().isMousePressed = false;
						fileContentTable.getModel().fireTableDataChanged();
					}
				}
			});
			getVerticalScrollBar().addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (fileContentTable.getModel().getFileSize() > MAX_FILE_SIZE) {
						fileContentTable.getModel().isMousePressed = true;
					}
				}
			});

			getHorizontalScrollBar().addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (fileContentTable.getModel().getFileSize() > MAX_FILE_SIZE) {
						fileContentTable.getModel().isMousePressed = false;
						fileContentTable.getModel().fireTableDataChanged();
					}
				}
			});
			getHorizontalScrollBar().addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (fileContentTable.getModel().getFileSize() > MAX_FILE_SIZE) {
						fileContentTable.getModel().isMousePressed = true;
					}
				}
			});

			getViewport().setBackground(null);
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
			setBackground(null);
			//getColumnModel().getColumn(0).setPreferredWidth(50);
			//getColumnModel().getColumn(0).setMaxWidth(50);
			setAutoscrolls(true);
			getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Component rendererComp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					//rendererComp.setBackground(new Color(228, 228, 228));
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

	private void initializeFileContentTabbedPane() {
		fileContentTabbedPane.addChangeListener(e -> {
			int tabCount = fileContentTabbedPane.getTabCount();

			if (tabCount > 0) {
				int selectedIndex = fileContentTabbedPane.getSelectedIndex();
				if (fileContentTabbedPane.getTabComponentAt(selectedIndex) != null) {
					for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
						fileContentTabbedPane.getTabComponentAt(tabIndex).setBackground(null);
					}
				}
			}
		});
	}

}