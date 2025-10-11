package com.mojang.minecraft.sound;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.minecraft.Entity;
import com.mojang.minecraft.level.tile.Tile.SoundType;
import com.mojang.minecraft.player.Player;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.EaglerInputStream;
import net.lax1dude.eaglercraft.internal.EnumPlatformType;
import net.lax1dude.eaglercraft.internal.IAudioCacheLoader;
import net.lax1dude.eaglercraft.internal.IAudioHandle;
import net.lax1dude.eaglercraft.internal.IAudioResource;
import net.lax1dude.eaglercraft.internal.PlatformAudio;
import net.peytonsound.SoundPool;

public final class SoundManager {
	public Map<String, IAudioResource> sounds = new HashMap<String, IAudioResource>();
	private Map<String, IAudioResource> music = new HashMap<String, IAudioResource>();
	public Random random = new Random();
	public long lastMusic = System.currentTimeMillis() + 60000L;
	
	private IAudioHandle musicHandle;
	public boolean enabled = true;
	public boolean playMusic() {
		if(enabled) {
			String music = SoundPool.getRandomMusic();
			IAudioResource trk = this.music.get(music);
			if (trk == null) {
				if (EagRuntime.getPlatformType() != EnumPlatformType.DESKTOP) {
					trk = PlatformAudio.loadAudioDataNew(music, false, browserResourceLoader);
				} else {
					trk = PlatformAudio.loadAudioData(music, false);
				}
				if (trk != null) {
					this.music.put(music, trk);
				}
			}

			if (trk != null) {
				musicHandle = PlatformAudio.beginPlaybackStatic(trk, 1.0f, 1.0f, false);
				return true;
			}

			return false;
		} else {
			return false;
		}
	}

	public void playSound(String var1, Entity var2) {
		if(enabled) {
			SoundType type = SoundType.getSoundType(var1);
			String sound = null;
			if (type != null) {
				sound = SoundPool.getRandomSound(type);
			}
			if (sound == null) {
				System.out.println("Missing sound: " + var1);
				return;
			}
			IAudioResource trk = sounds.get(sound);

			if (trk == null) {
				if (EagRuntime.getPlatformType() != EnumPlatformType.DESKTOP) {
					trk = PlatformAudio.loadAudioDataNew(sound, true, browserResourceLoader);
				} else {
					trk = PlatformAudio.loadAudioData(sound, true);
				}

				if (trk != null) {
					this.sounds.put(sound, trk);
				}
			}

			if (trk != null) {
				PlatformAudio.beginPlayback(trk, var2.x, var2.y, var2.z, type.getVolume(), type.getPitch(), false);
			}
		}
	}

	public void playSound(String var1, float x, float y, float z) {
		if(enabled) {
			SoundType type = SoundType.getSoundType(var1);
			String sound = null;
			if (type != null) {
				sound = SoundPool.getRandomSound(type);
			}
			if (sound == null) {
				System.out.println("Missing sound: " + var1);
				return;
			}
			IAudioResource trk = sounds.get(sound);
	
			if (trk == null) {
				if (EagRuntime.getPlatformType() != EnumPlatformType.DESKTOP) {
					trk = PlatformAudio.loadAudioDataNew(sound, true, browserResourceLoader);
				} else {
					trk = PlatformAudio.loadAudioData(sound, true);
				}
	
				if (trk != null) {
					this.sounds.put(sound, trk);
				}
			}
	
			if (trk != null) {
				PlatformAudio.beginPlayback(trk, x, y, z, type.getVolume(), type.getPitch(), false);
			}
		}
	}

	public void updatePosition(Player var1, float var2) {
		if (var1 != null) {
			try {
				float var9 = var1.xRotO + (var1.xRot - var1.xRotO) * var2;
				float var3 = var1.yRotO + (var1.yRot - var1.yRotO) * var2;
				double var4 = var1.xo + (var1.x - var1.xo) * (double)var2;
				double var6 = var1.yo + (var1.y - var1.yo) * (double)var2;
				double var8 = var1.zo + (var1.z - var1.zo) * (double)var2;
				PlatformAudio.setListener((float) var4, (float) var6, (float) var8, (float) var9, (float) var3);
			} catch (Exception e) {
			}
		}
	}
	
	public void settingsChanged() {
		if (musicHandle != null && !musicHandle.shouldFree() && !enabled) {
			musicHandle.end();
			this.lastMusic = EagRuntime.steadyTimeMillis();
		}
	}

	
	private final IAudioCacheLoader browserResourceLoader = fileName -> {
		try {
			return EaglerInputStream.inputStreamToBytesQuiet(EagRuntime.getRequiredResourceStream(fileName));
		} catch (Throwable t) {
			return null;
		}
	};
}
