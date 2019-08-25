package org.denis.files;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.eaio.stringsearch.BoyerMooreHorspool;

import static java.nio.file.FileVisitResult.CONTINUE;

public class SearchFiles {

	private class Finder extends SimpleFileVisitor<Path> {

		private final PathMatcher matcher;
		private String pattern;
		private Consumer<Path> action;
		BoyerMooreHorspool boyerMooreHorspool = new BoyerMooreHorspool();

		Finder(String extension, String pattern, Consumer<Path> action) {
			matcher = FileSystems.getDefault().getPathMatcher("glob:" + extension);
			this.pattern = pattern;
			this.action = action;
		}

		// Compares the glob pattern against
		// the file or directory name.
		private void find(Path file) {
			Path name = file.getFileName();
			if (name != null && matcher.matches(name)) {
				//files.add(file);
				try {
					//поиск подстроки
					if (boyerMooreHorspool.searchBytes(Files.readAllBytes(file), pattern.getBytes()) != -1)
						action.accept(file);
				} catch (IOException e) {
					//throw new RuntimeException(e);
					//System.err.println(e);
				}
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

	public void traverseTree(Path dir, String extension, String pattern, Consumer<Path> action) throws IOException {
		Finder finder = new Finder(extension, pattern, action);
		Files.walkFileTree(dir, finder);
	}
}
