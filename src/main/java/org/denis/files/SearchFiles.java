package org.denis.files;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

public class SearchFiles implements TreeObservable {

	List<TreeObserver> list = new ArrayList<>();

	@Override
	public void registerObserver(TreeObserver o) {
		list.add(o);
	}

	@Override
	public void removeObserver(TreeObserver o) {
		list.remove(o);
	}

	@Override
	public void notifyObservers(Path path) {
		for (TreeObserver treeObserver : list) {
			treeObserver.updateTree(path);
		}
	}

	private class Finder extends SimpleFileVisitor<Path> {

		private final PathMatcher matcher;

		Finder(String pattern) {
			matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		}

		// Compares the glob pattern against
		// the file or directory name.
		void find(Path file) {
			Path name = file.getFileName();
			if (name != null && matcher.matches(name)) {
				//files.add(file);
				notifyObservers(file);
			}
		}

		// Invoke the pattern matching
		// method on each file.
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			find(file);
			return CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
			return CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			//exc.printStackTrace();
			return CONTINUE;
		}
	}

	public void traverseTree(Path dir, String extension) throws IOException {
		Finder finder = new Finder(extension);
		Files.walkFileTree(dir, finder);
	}
}
