package net.peytonsound;

import java.util.ArrayList;
import java.util.List;

public class Resource {

	public final String dir;
	public final String ext;

	private List<String> list;

	public final boolean hasList;

	public Resource(String dir, String fileType, boolean sendToServer) {
		this(dir, fileType, sendToServer, null);
	}

	public Resource(String dir, String fileType, boolean sendToServer, List<String> addTo) {
		this.dir = dir;
		this.ext = fileType;
		this.list = addTo;
		hasList = addTo != null;
	}

	public List<String> getList() {
		if (list == null) {
			list = new ArrayList<>();
		}
		return list;
	}

}
