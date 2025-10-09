package net.peytonsound;

import java.util.ArrayList;
import java.util.List;

public class ResourceLoader {
	
	private static final ArrayList<Resource> resources = new ArrayList<>();
	
	private static final ArrayList<String> blockSounds = new ArrayList<>();
	private static final ArrayList<String> music = new ArrayList<>();
	
	public static List<String> getSounds() {
		return blockSounds;
	}
	
	public static List<String> getMusic() {
		return music;
	}

	public static void onResourceLoad(String fileName) {
		String filePath = fileName.contains("resources/") ? fileName.replace("resources/", "") : fileName;
		
		Resource resource;
		for (int i = 0, j = resources.size(); i < j; ++i) {
			resource = resources.get(i);
			
			if (fileName.contains(resource.dir) && fileName.endsWith(resource.ext)) {
				i = j;
				
				System.out.println("Resource loaded: " + filePath);
				if (resource.hasList) {
					resource.getList().add(filePath);
				}
			}
		}
	}
	
	static {
		resources.add(new Resource("sounds/sound/step", "ogg", false, blockSounds));
		resources.add(new Resource("sounds/music", "ogg", false, music));
	}
}
