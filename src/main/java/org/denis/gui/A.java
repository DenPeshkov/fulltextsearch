package org.denis.gui;

import org.denis.files.SearchFiles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Display a file system in a JTree view
 *
 * @author Ian Darwin
 * @version $Id: A.java,v 1.9 2004/02/23 03:39:22 ian Exp $
 */
public class A extends JPanel {
	/**
	 * Construct a A
	 */
	public A(File dir) {
		setLayout(new BorderLayout());

		// Make a tree list with all the nodes, and make it a JTree
		JTree tree = new JTree(addNodes(null, dir));

		// Add a listener
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) e
						.getPath().getLastPathComponent();
				System.out.println("You selected " + node);
			}
		});

		// Lastly, put the JTree into a JScrollPane.
		JScrollPane scrollpane = new JScrollPane();
		scrollpane.getViewport().add(tree);
		add(BorderLayout.CENTER, scrollpane);
	}

	/**
	 * Add nodes from under "dir" into curTop. Highly recursive.
	 */
	DefaultMutableTreeNode addNodes(DefaultMutableTreeNode curTop, Path dir) throws IOException {
		String curPath = dir.toString();
		DefaultMutableTreeNode curDir = new DefaultMutableTreeNode(curPath);
		if (curTop != null) { // should only be null at root
			curTop.add(curDir);
		}

		List<Path> paths = SearchFiles.traverseTree(Paths.get("/"), "*");


	}

	public Dimension getMinimumSize() {
		return new Dimension(200, 400);
	}

	public Dimension getPreferredSize() {
		return new Dimension(200, 400);
	}

	/**
	 * Main: make a Frame, add a A
	 */
	public static void main(String[] av) {

		JFrame frame = new JFrame("A");
		frame.setForeground(Color.black);
		frame.setBackground(Color.lightGray);
		Container cp = frame.getContentPane();

		if (av.length == 0) {
			cp.add(new A(new File(".")));
		} else {
			cp.setLayout(new BoxLayout(cp, BoxLayout.X_AXIS));
			for (int i = 0; i < av.length; i++)
				cp.add(new A(new File(av[i])));
		}

		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
