package org.denis.files;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

//Factory class
public class SearchFiles {

	private SearchFiles() {

	}

	static private class Finder extends SimpleFileVisitor<Path> {

		public List<Path> getFiles() {
			return files;
		}

		private final List<Path> files = new ArrayList<>();

		private final PathMatcher matcher;

		Finder(String pattern) {
			matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		}

		// Compares the glob pattern against
		// the file or directory name.
		void find(Path file) {
			Path name = file.getFileName();
			if (name != null && matcher.matches(name)) {
				files.add(file);
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
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			exc.printStackTrace();
			return CONTINUE;
		}
	}

	public static List<Path> traverseTree(Path dir, String extension) throws IOException {
		Finder finder = new Finder(extension);
		Files.walkFileTree(dir, finder);
		return finder.getFiles();
	}
}
