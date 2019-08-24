package org.denis.files;

import java.nio.file.Path;

public interface TreeObserver {
	void updateTree(Path file);
}
