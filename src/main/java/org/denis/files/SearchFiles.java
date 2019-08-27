package org.denis.files;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.eaio.stringsearch.BoyerMooreHorspool;

import static java.nio.file.FileVisitResult.CONTINUE;

public class SearchFiles {

	public static class SearchFileResult {
		private Path file;
		private boolean exceedSize;

		public SearchFileResult(Path file, boolean exceedSize) {
			this.file = file;
			this.exceedSize = exceedSize;
		}

		public Path getFile() {
			return file;
		}

		public boolean isExceedSize() {
			return exceedSize;
		}
	}

	static class Finder extends SimpleFileVisitor<Path> {

		private final PathMatcher matcher;
		private String pattern;
		private Consumer<SearchFileResult> action;
		BoyerMooreHorspool boyerMooreHorspool = new BoyerMooreHorspool();

		Finder(String extension, String pattern, Consumer<SearchFileResult> action) {
			//сравниваем расширение файла
			matcher = FileSystems.getDefault().getPathMatcher("glob:*" + extension);
			this.pattern = pattern;
			this.action = action;
		}

		void find(Path file) {
			Path name = file.getFileName();
			if (name != null && matcher.matches(name)) {
				//поиск подстроки
				//использууем один поток, т.к. поиск очень быстрый и тем самым избавляемся от оверхеда управления потоков
				try (FileChannel filechanel = FileChannel.open(file)) {
					if (filechanel.size() > Integer.MAX_VALUE) {
						action.accept(new SearchFileResult(file, true));
						return;
					}
					MappedByteBuffer mb = filechanel.map(FileChannel.MapMode.READ_ONLY, 0L, filechanel.size());
					byte[] barray = new byte[(int) filechanel.size()];
					mb.get(barray);
					//byte[] barray = new byte[] {1,2,32,42,4,};
					if (boyerMooreHorspool.searchBytes(barray, pattern.getBytes()) != -1) {
						action.accept(new SearchFileResult(file, false));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// Invoke the pattern matching
		// method on each file.
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			find(file);
			System.gc();
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

	public static void traverseTree(Path dir, String extension, String pattern, Consumer<SearchFileResult> action) throws IOException {
		Finder finder = new Finder(extension, pattern, action);
		Files.walkFileTree(dir, finder);
	}
}
