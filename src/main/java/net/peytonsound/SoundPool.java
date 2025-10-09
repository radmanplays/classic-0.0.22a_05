package net.peytonsound;

import java.util.List;
import java.util.Random;
import com.mojang.minecraft.level.tile.Tile.SoundType;

public class SoundPool {
	
	private static Random random = new Random();
	private static int lastIdx = -1;
	
	public static String getRandomMusic() {
		List<String> music = ResourceLoader.getMusic();
		
		int idx = random.nextInt(music.size());
		while (idx == lastIdx) {
			idx = random.nextInt(music.size());
		}
		
		return music.get(idx);
	}
	
	public static String getRandomSound(SoundType type) {
		int num = random.nextInt(type.num) + 1;
		String sound = type.name + num;
		List<String> sounds = ResourceLoader.getSounds();
		for (int i = 0, j = sounds.size(); i < j; ++i) {
			String sound_ = sounds.get(i);
			if (sound_.contains(sound)) {
				return sound_;
			}
		}
		return null;
	}
	
	

}
