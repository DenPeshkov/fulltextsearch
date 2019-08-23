package org.denis.gui;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class FilesToTreeTest {

	@org.junit.jupiter.api.Test
	void getNode() {
		System.out.println(Paths.get("/home/denis/sound").relativize(Paths.get("home/denis/images")));
	}
}