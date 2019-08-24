package org.denis.files;

import java.nio.file.Path;

public interface TreeObservable {
	void registerObserver(TreeObserver o);
	void removeObserver(TreeObserver o);
	void notifyObservers(Path path);
}
