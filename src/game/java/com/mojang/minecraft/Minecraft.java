package com.mojang.minecraft;

import com.mojang.comm.SocketConnection;
import com.mojang.minecraft.character.Vec3;
import com.mojang.minecraft.character.Zombie;
import com.mojang.minecraft.character.ZombieModel;
import com.mojang.minecraft.gui.ChatScreen;
import com.mojang.minecraft.gui.ErrorScreen;
import com.mojang.minecraft.gui.Font;
import com.mojang.minecraft.gui.InGameHud;
import com.mojang.minecraft.gui.InventoryScreen;
import com.mojang.minecraft.gui.PauseScreen;
import com.mojang.minecraft.gui.Screen;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.level.LevelIO;
import com.mojang.minecraft.level.levelgen.LevelGen;
import com.mojang.minecraft.level.liquid.Liquid;
import com.mojang.minecraft.level.tile.Tile;
import com.mojang.minecraft.net.ConnectionManager;
import com.mojang.minecraft.net.NetworkPlayer;
import com.mojang.minecraft.net.Packet;
import com.mojang.minecraft.particle.Particle;
import com.mojang.minecraft.particle.ParticleEngine;
import com.mojang.minecraft.phys.AABB;
import com.mojang.minecraft.player.Inventory;
import com.mojang.minecraft.player.MovementInputFromOptions;
import com.mojang.minecraft.player.Player;
import com.mojang.minecraft.renderer.Chunk;
import com.mojang.minecraft.renderer.Frustum;
import com.mojang.minecraft.renderer.LevelRenderer;
import com.mojang.minecraft.renderer.RenderHelper;
import com.mojang.minecraft.renderer.Tesselator;
import com.mojang.minecraft.renderer.Textures;
import com.mojang.minecraft.renderer.texture.TextureFX;
import com.mojang.minecraft.renderer.texture.TextureLavaFX;
import com.mojang.minecraft.renderer.texture.TextureWaterFX;
import com.mojang.minecraft.sound.SoundManager;
import com.mojang.util.GLAllocation;
import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.EagUtils;
import com.mojang.minecraft.renderer.DirtyChunkSorter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import java.util.ArrayList;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import net.lax1dude.eaglercraft.internal.EnumPlatformType;
import net.lax1dude.eaglercraft.internal.IWebSocketFrame;
import net.lax1dude.eaglercraft.internal.buffer.ByteBuffer;
import net.lax1dude.eaglercraft.internal.buffer.FloatBuffer;
import net.lax1dude.eaglercraft.internal.buffer.IntBuffer;
import net.lax1dude.eaglercraft.internal.vfs2.VFile2;

public final class Minecraft implements Runnable {
	private boolean fullscreen = false;
	public int width;
	public int height;
	private Timer timer = new Timer(20.0F);
	public Level level;
	private LevelRenderer levelRenderer;
	public Player player;
	private ParticleEngine particleEngine;
	public User user = null;
	private int yMouseAxis = 1;
	public Textures textures;
	public Font font;
	private int editMode = 0;
	public Screen screen = null;
	public ProgressListener loadingScreen = new ProgressListener(this);
	public RenderHelper renderHelper = new RenderHelper(this);
	public LevelIO levelIo = new LevelIO(this.loadingScreen);
	private LevelGen levelGen = new LevelGen(this.loadingScreen);
	public SoundManager soundManager = new SoundManager();
	private int ticksRan = 0;
	public String loadMapUser = null;
	private InGameHud hud;
	public int loadMapID = 0;
	public boolean hideGui = false;
	public ZombieModel playerModel = new ZombieModel();
	public ConnectionManager connectionManager;
	public HitResult hitResult = null;
	String server = null;
	int port = 0;
	private float fogColorRed = 0.5F;
	private float fogColorGreen = 0.8F;
	private float fogColorBlue = 1.0F;
	volatile boolean running = false;
	public String fpsString = "";
	private boolean mouseGrabbed = false;
	public int prevFrameTime = 0;

	
	public Minecraft(int var2, int var3, boolean var4) {
		this.width = width;
		this.height = height;
		this.fullscreen = false;
		this.textures = new Textures();
		this.textures.registerTextureFX(new TextureLavaFX());
		this.textures.registerTextureFX(new TextureWaterFX());
	}
	
	public final void setServer(String var1) {
		server = var1;
	}
	
	public final void setScreen(Screen var1) {
		if(!(this.screen instanceof ErrorScreen)) {
			if(this.screen != null) {
				this.screen.closeScreen();
			}

			this.screen = var1;
			if(var1 != null) {
				if(this.mouseGrabbed) {
					this.player.releaseAllKeys();
					this.mouseGrabbed = false;
					Mouse.setGrabbed(false);
				}

				int var2 = this.width * 240 / this.height;
				int var3 = this.height * 240 / this.height;
				var1.init(this, var2, var3);
				this.hideGui = false;
			} else {
				this.grabMouse();
			}
		}
	}
	
	private static void checkGlError(String string) {
		int errorCode = GL11.glGetError();
		if(errorCode != 0) {
			String errorString = GLU.gluErrorString(errorCode);
			System.out.println("########## GL ERROR ##########");
			System.out.println("@ " + string);
			System.out.println(errorCode + ": " + errorString);
			throw new RuntimeException(errorCode + ": " + errorString);

		}

	}

	public final void destroy() {
		Minecraft var2 = this;
		try {
			if(this.connectionManager == null && var2.level != null) {
				LevelIO.save(var2.level, new VFile2("level.dat"));
			}
		} catch (Exception var1) {
			var1.printStackTrace();
		}
		if(this.connectionManager != null) {
			connectionManager.connection.disconnect();
		}
		EagRuntime.destroy();
	}

	public final void run() {
		this.running = true;

		try {
			Minecraft var4 = this;
			if(this.fullscreen) {
				Display.toggleFullscreen();
				this.width = Display.getWidth();
				this.height = Display.getHeight();
			} else {
				this.width = Display.getWidth();
				this.height = Display.getHeight();
			}

			Display.setTitle("Minecraft 0.0.22a_05");

			Display.create();
			Keyboard.create();
			Mouse.create();

			checkGlError("Pre startup");
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glShadeModel(GL11.GL_SMOOTH);
			GL11.glClearDepth(1.0D);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
			GL11.glCullFace(GL11.GL_BACK);
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			checkGlError("Startup");
			this.font = new Font("/default.png", this.textures);
			IntBuffer var8 = GLAllocation.createIntBuffer(256);
			var8.clear().limit(256);
			GL11.glViewport(0, 0, this.width, this.height);
			if(this.server != null && this.user != null) {
				this.connectionManager = new ConnectionManager(this, this.server, this.user.name);
				this.level = null;
			} else {
				boolean var9 = false;
	
				try {
					Level var10 = null;
					var10 = var4.levelIo.load(new VFile2("level.dat"));
					var9 = var10 != null;
					if(!var9) {
						var10 = var4.levelIo.loadLegacy(new VFile2("level.dat"));
						var9 = var10 != null;
					}
	
					var4.setLevel(var10);
				} catch (Exception var20) {
					var20.printStackTrace();
					var9 = false;
				}
	
				if(!var9) {
					this.generateLevel(1);
				}
			}

			this.levelRenderer = new LevelRenderer(this.textures);
			this.particleEngine = new ParticleEngine(this.level, this.textures);
			this.player = new Player(this.level, new MovementInputFromOptions());
			this.player.resetPos();
			if(this.level != null) {
				this.setLevel(this.level);
			}

			checkGlError("Post startup");
			this.hud = new InGameHud(this, this.width, this.height);
		} catch (Exception var26) {
			var26.printStackTrace();
			System.out.println("Failed to start Minecraft");
			return;
		}

		long var1 = System.currentTimeMillis();
		int var3 = 0;

		try {
			while(this.running) {
					if(Display.isCloseRequested()) {
						if(this.connectionManager != null) {
							connectionManager.connection.disconnect();
						}
						this.running = false;
					}

					try {
						Timer var42 = this.timer;
						long var47 = System.currentTimeMillis();
						long var51 = var47 - var42.lastSyncSysClock;
						long var56 = System.nanoTime() / 1000000L;
						double var15;
						if(var51 > 1000L) {
							long var13 = var56 - var42.lastSyncHRClock;
							var15 = (double)var51 / (double)var13;
							var42.timeSyncAdjustment += (var15 - var42.timeSyncAdjustment) * (double)0.2F;
							var42.lastSyncSysClock = var47;
							var42.lastSyncHRClock = var56;
						}

						if(var51 < 0L) {
							var42.lastSyncSysClock = var47;
							var42.lastSyncHRClock = var56;
						}

						double var69 = (double)var56 / 1000.0D;
						var15 = (var69 - var42.lastHRTime) * var42.timeSyncAdjustment;
						var42.lastHRTime = var69;
						if(var15 < 0.0D) {
							var15 = 0.0D;
						}

						if(var15 > 1.0D) {
							var15 = 1.0D;
						}

						var42.fps = (float)((double)var42.fps + var15 * (double)var42.timeScale * (double)var42.ticksPerSecond);
						var42.ticks = (int)var42.fps;
						if(var42.ticks > 100) {
							var42.ticks = 100;
						}

						var42.fps -= (float)var42.ticks;
						var42.a = var42.fps;

						for(int var43 = 0; var43 < this.timer.ticks; ++var43) {
							++this.ticksRan;
							this.tick();
						}

						checkGlError("Pre render");
						float var48 = this.timer.a;
						RenderHelper var44 = this.renderHelper;
						
						this.soundManager.updatePosition(this.player, this.timer.a);

						var44.displayActive = Display.isActive();
						int var50;
						int var53;
						int var58;
						int var63;
						if(var44.minecraft.mouseGrabbed) {
							var50 = 0;
							var53 = 0;
							var50 = Mouse.getDX();
							var53 = Mouse.getDY();
							var44.minecraft.player.turn((float)var50, (float)(var53 * var44.minecraft.yMouseAxis));
						}

						if(!var44.minecraft.hideGui) {
							int var68;
							if(var44.minecraft.level != null) {
								Player var16 = var44.minecraft.player;
								Level var5 = var44.minecraft.level;
								LevelRenderer var6 = var44.minecraft.levelRenderer;
								ParticleEngine var49 = var44.minecraft.particleEngine;
								if (Display.wasResized()) {
									if(Display.getHeight() != 0) {
										this.width = Display.getWidth();
										this.height = Display.getHeight();
										if(this.hud !=null) {
											this.hud = new InGameHud(this, this.width, this.height);
										}
										
										if(this.screen != null) {
											Screen sc = this.screen;
											this.setScreen((Screen)null);
											this.setScreen(sc);
										}
									}
								}
								GL11.glViewport(0, 0, var44.minecraft.width, var44.minecraft.height);
								Level var54 = var44.minecraft.level;
								Player var60 = var44.minecraft.player;
								float var65 = 1.0F / (float)(4 - var44.minecraft.levelRenderer.drawDistance);
								var65 = (float)Math.pow((double)var65, 0.25D);
								var44.fogColorRed = 0.6F * (1.0F - var65) + var65;
								var44.fogColorGreen = 0.8F * (1.0F - var65) + var65;
								var44.fogColorBlue = 1.0F * (1.0F - var65) + var65;
								var44.fogColorRed *= var44.fogColorMultiplier;
								var44.fogColorGreen *= var44.fogColorMultiplier;
								var44.fogColorBlue *= var44.fogColorMultiplier;
								Tile var71 = Tile.tiles[var54.getTile((int)var60.x, (int)(var60.y + 0.12F), (int)var60.z)];
								if(var71 != null && var71.getLiquidType() != Liquid.none) {
									Liquid var17 = var71.getLiquidType();
									if(var17 == Liquid.water) {
										var44.fogColorRed = 0.02F;
										var44.fogColorGreen = 0.02F;
										var44.fogColorBlue = 0.2F;
									} else if(var17 == Liquid.lava) {
										var44.fogColorRed = 0.6F;
										var44.fogColorGreen = 0.1F;
										var44.fogColorBlue = 0.0F;
									}
								}

								GL11.glClearColor(var44.fogColorRed, var44.fogColorGreen, var44.fogColorBlue, 0.0F);
								GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
								var60 = var44.minecraft.player;
								var65 = var60.xRotO + (var60.xRot - var60.xRotO) * var48;
								float var72 = var60.yRotO + (var60.yRot - var60.yRotO) * var48;
								float var78 = var60.xo + (var60.x - var60.xo) * var48;
								float var57 = var60.yo + (var60.y - var60.yo) * var48;
								float var55 = var60.zo + (var60.z - var60.zo) * var48;
								Vec3 var59 = new Vec3(var78, var57, var55);
								var55 = (float)Math.cos((double)(-var72) * Math.PI / 180.0D + Math.PI);
								float var66 = (float)Math.sin((double)(-var72) * Math.PI / 180.0D + Math.PI);
								var72 = (float)Math.cos((double)(-var65) * Math.PI / 180.0D);
								var65 = (float)Math.sin((double)(-var65) * Math.PI / 180.0D);
								var66 *= var72;
								var55 *= var72;
								var72 = 5.0F;
								float var10001 = var66 * var72;
								float var10002 = var65 * var72;
								var72 = var55 * var72;
								var65 = var10002;
								var66 = var10001;
								Vec3 var61 = new Vec3(var59.x + var66, var59.y + var65, var59.z + var72);
								var44.minecraft.hitResult = var44.minecraft.level.clip(var59, var61);
								var44.fogColorMultiplier = 1.0F;
								var44.renderDistance = (float)(512 >> (var44.minecraft.levelRenderer.drawDistance << 1));
								GL11.glMatrixMode(GL11.GL_PROJECTION);
								GL11.glLoadIdentity();
								GLU.gluPerspective(70.0F, (float)var44.minecraft.width / (float)var44.minecraft.height, 0.05F, var44.renderDistance);
								GL11.glMatrixMode(GL11.GL_MODELVIEW);
								GL11.glLoadIdentity();
								Player var74 = var44.minecraft.player;
								GL11.glTranslatef(0.0F, 0.0F, -0.3F);
								GL11.glRotatef(var74.xRotO + (var74.xRot - var74.xRotO) * var48, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(var74.yRotO + (var74.yRot - var74.yRotO) * var48, 0.0F, 1.0F, 0.0F);
								var78 = var74.xo + (var74.x - var74.xo) * var48;
								var57 = var74.yo + (var74.y - var74.yo) * var48;
								var55 = var74.zo + (var74.z - var74.zo) * var48;
								GL11.glTranslatef(-var78, -var57, -var55);
								GL11.glEnable(GL11.GL_CULL_FACE);
								Frustum var64 = Frustum.getFrustum();
								Frustum var67 = var64;
								LevelRenderer var62 = var44.minecraft.levelRenderer;

								for(var58 = 0; var58 < var62.sortedChunks.length; ++var58) {
									var62.sortedChunks[var58].isInFrustum(var67);
								}

								var62 = var44.minecraft.levelRenderer;
								List<Chunk> var73 = new ArrayList<>(var62.dirtyChunks);
								var73.sort(new DirtyChunkSorter(var74));
								var73.addAll(var62.dirtyChunks);
								var63 = 4;
								Iterator var75 = var73.iterator();

								while(var75.hasNext()) {
									Chunk var79 = (Chunk)var75.next();
									var79.rebuild();
									var62.dirtyChunks.remove(var79);
									--var63;
									if(var63 == 0) {
										break;
									}
								}

								boolean var45 = var5.isSolid(var16.x, var16.y, var16.z, 0.1F);
								var44.setupFog();
								GL11.glEnable(GL11.GL_FOG);
								var6.render(var16, 0);
								int var46;
								if(var45) {
									var46 = (int)var16.x;
									var53 = (int)var16.y;
									var68 = (int)var16.z;

									for(var58 = var46 - 1; var58 <= var46 + 1; ++var58) {
										for(var63 = var53 - 1; var63 <= var53 + 1; ++var63) {
											for(int var76 = var68 - 1; var76 <= var68 + 1; ++var76) {
												var6.render(var58, var63, var76);
											}
										}
									}
								}

//								var44.toggleLight(true);
								var6.renderEntities(var64, var48);
//								var44.toggleLight(false);
								var44.setupFog();
								var49.render(var16, var48);
								var6.renderSurroundingGround();
								GL11.glDisable(GL11.GL_LIGHTING);
								var44.setupFog();
								var6.renderClouds(var48);
								var44.setupFog();
								GL11.glEnable(GL11.GL_LIGHTING);
								if(var44.minecraft.hitResult != null) {
									GL11.glDisable(GL11.GL_LIGHTING);
									GL11.glDisable(GL11.GL_ALPHA_TEST);
									var6.renderHit(var16, var44.minecraft.hitResult, var44.minecraft.editMode, var16.inventory.getSelected());
									LevelRenderer.renderHitOutline(var44.minecraft.hitResult, var44.minecraft.editMode);
									GL11.glEnable(GL11.GL_ALPHA_TEST);
									GL11.glEnable(GL11.GL_LIGHTING);
								}

								GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
								var44.setupFog();
								var6.renderSurroundingGround();
								GL11.glEnable(GL11.GL_BLEND);
//								GL11.glColorMask(false, false, false, false);
								var46 = var6.render(var16, 1);
//								GL11.glColorMask(true, true, true, true);
								if(var46 > 0) {
									GL11.glEnable(GL11.GL_TEXTURE_2D);
									GL11.glBindTexture(GL11.GL_TEXTURE_2D, var6.textures.getTextureId("/terrain.png"));
									GL11.glCallLists(var6.dummyBuffer);
									GL11.glDisable(GL11.GL_TEXTURE_2D);
								}

								GL11.glDepthMask(true);
								GL11.glDisable(GL11.GL_BLEND);
								GL11.glDisable(GL11.GL_LIGHTING);
								GL11.glDisable(GL11.GL_FOG);
								GL11.glDisable(GL11.GL_TEXTURE_2D);
								if(var44.minecraft.hitResult != null) {
									GL11.glDepthFunc(GL11.GL_LESS);
									GL11.glDisable(GL11.GL_ALPHA_TEST);
//									var6.renderHit(var16, var44.minecraft.hitResult, var44.minecraft.editMode, var16.inventory.getSelected());
									LevelRenderer.renderHitOutline(var44.minecraft.hitResult, var44.minecraft.editMode);
									GL11.glEnable(GL11.GL_ALPHA_TEST);
									GL11.glDepthFunc(GL11.GL_LEQUAL);
								}

								var44.minecraft.hud.render();
							} else {
								GL11.glViewport(0, 0, var44.minecraft.width, var44.minecraft.height);
								GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
								GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
								GL11.glMatrixMode(GL11.GL_PROJECTION);
								GL11.glLoadIdentity();
								GL11.glMatrixMode(GL11.GL_MODELVIEW);
								GL11.glLoadIdentity();
								var44.initGui();
							}

							if(var44.minecraft.screen != null) {
								var50 = var44.minecraft.width * 240 / var44.minecraft.height;
								var53 = var44.minecraft.height * 240 / var44.minecraft.height;
								var68 = Mouse.getX() * var50 / var44.minecraft.width;
								var58 = var53 - Mouse.getY() * var53 / var44.minecraft.height - 1;
								var44.minecraft.screen.render(var68, var58);
							}
							Display.update();
						}

						checkGlError("Post render");
						++var3;
					} catch (Exception var34) {
						this.setScreen(new ErrorScreen("Client error", "The game broke! [" + var34 + "]"));
						var34.printStackTrace();
					}

					while(System.currentTimeMillis() >= var1 + 1000L) {
						this.fpsString = var3 + " fps, " + Chunk.updates + " chunk updates";
						Chunk.updates = 0;
						var1 += 1000L;
						var3 = 0;
					}
				}

			return;
		} catch (StopGameException var35) {
			return;
		} catch (Exception var36) {
			var36.printStackTrace();
		} finally {
			this.destroy();
		}

	}
	
	public final void grabMouse() {
		if(!this.mouseGrabbed) {
			this.mouseGrabbed = true;
			Mouse.setGrabbed(true);
			this.setScreen((Screen)null);
			this.prevFrameTime = this.ticksRan + 10000;
		}
	}
	
	public void pauseGame() {
		if(!(this.screen instanceof PauseScreen)) {
			this.setScreen(new PauseScreen());
		}
	}
	
	private int saveCountdown = 600;

	private void levelSave() {
	    if (level == null) return;

	    saveCountdown--;
	    if (saveCountdown <= 0) {
	    	LevelIO.save(this.level, new VFile2("level.dat"));
	        saveCountdown = 600;
	    }
	}
	

	private void clickMouse() {
		if(this.hitResult != null) {
			int var1 = this.hitResult.x;
			int var2 = this.hitResult.y;
			int var3 = this.hitResult.z;
			if(this.editMode != 0) {
				if(this.hitResult.f == 0) {
					--var2;
				}

				if(this.hitResult.f == 1) {
					++var2;
				}

				if(this.hitResult.f == 2) {
					--var3;
				}

				if(this.hitResult.f == 3) {
					++var3;
				}

				if(this.hitResult.f == 4) {
					--var1;
				}

				if(this.hitResult.f == 5) {
					++var1;
				}
			}

			Tile var4 = Tile.tiles[this.level.getTile(var1, var2, var3)];
			if(this.editMode == 0) {
				if(var4 != Tile.unbreakable || this.player.userType >= 100) {
					boolean var8 = this.level.netSetTile(var1, var2, var3, 0);
					if(var4 != null && var8) {
						if(this.isMultiplayer()) {
							this.connectionManager.sendBlockChange(var1, var2, var3, this.editMode, this.player.inventory.getSelected());
						}

						if(var4.soundType != Tile.SoundType.none) {
							this.level.playSound("step." + var4.soundType.name, (float)var1, (float)var2, (float)var3, (var4.soundType.getVolume() + 1.0F) / 2.0F, var4.soundType.getPitch() * 0.8F);
							var4.destroy(this.level, var1, var2, var3, this.particleEngine);
						}
					}

					return;
				}
			} else {
				int var5 = this.player.inventory.getSelected();
				var4 = Tile.tiles[this.level.getTile(var1, var2, var3)];
				if(var4 == null || var4 == Tile.water || var4 == Tile.calmWater || var4 == Tile.lava || var4 == Tile.calmLava) {
					AABB var7 = Tile.tiles[var5].getTileAABB(var1, var2, var3);
					if(var7 == null || (this.player.bb.intersects(var7) ? false : this.level.isFree(var7))) {
						if(this.isMultiplayer()) {
							this.connectionManager.sendBlockChange(var1, var2, var3, this.editMode, var5);
						}

						this.level.netSetTile(var1, var2, var3, this.player.inventory.getSelected());
						Tile.tiles[var5].onBlockAdded(this.level, var1, var2, var3);
					}
				}
			}

		}
	}
	
	private void tick() {
		SoundManager var1 = this.soundManager;
		if(System.currentTimeMillis() > var1.lastMusic && var1.playMusic()) {
			var1.lastMusic = System.currentTimeMillis() + (long)var1.random.nextInt(900000) + 300000L;
		}

		InGameHud var14 = this.hud;

		int var17;
		for(var17 = 0; var17 < var14.messages.size(); ++var17) {
			++((ChatLine)var14.messages.get(var17)).counter;
		}

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.textures.getTextureId("/terrain.png"));
		Textures var15 = this.textures;

		for(var17 = 0; var17 < var15.textureList.size(); ++var17) {
			TextureFX var3 = (TextureFX)var15.textureList.get(var17);
			var3.onTick();
			var15.textureBuffer.clear();
			var15.textureBuffer.put(var3.imageData);
			var15.textureBuffer.position(0).limit(var3.imageData.length);
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, var3.iconIndex % 16 << 4, var3.iconIndex / 16 << 4, 16, 16, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)var15.textureBuffer);
		}

		int var28;
		if(this.connectionManager != null && !(this.screen instanceof ErrorScreen)) {
			if(!this.connectionManager.isConnected()) {
				this.loadingScreen.beginLevelLoading("Connecting..");
				this.loadingScreen.setLoadingProgress(0);
			} else {
				ConnectionManager var16 = this.connectionManager;
				int var4;
				if(var16.processData) {
					SocketConnection var21 = var16.connection;
					if(var21.manager.isConnected()) {
						try {
							SocketConnection var20 = var16.connection;
							IWebSocketFrame packet = var21.webSocket.getNextBinaryFrame();
							byte[] packetData = packet == null ? null : packet.getByteArray();

							if (packetData != null && packetData.length > 0) {
								var20.readBuffer.put(packetData);
							}
							var4 = 0;

							while(var20.readBuffer.position() > 0 && var4++ != 100) {
								var20.readBuffer.flip();
								byte var5 = var20.readBuffer.get(0);
								Packet var6 = Packet.PACKETS[var5];
								if(var6 == null) {
									throw new IOException("Bad command: " + var5);
								}

								if(var20.readBuffer.remaining() < var6.size + 1) {
									var20.readBuffer.compact();
									break;
								}

								var20.readBuffer.get();
								Object[] var23 = new Object[var6.fields.length];

								for(var28 = 0; var28 < var23.length; ++var28) {
									var23[var28] = var20.read(var6.fields[var28]);
								}

								ConnectionManager var26 = var20.manager;
								if(var26.processData) {
									if(var6 == Packet.LOGIN) {
										var26.minecraft.loadingScreen.beginLevelLoading(var23[1].toString());
										var26.minecraft.loadingScreen.levelLoadUpdate(var23[2].toString());
										var26.minecraft.player.userType = ((Byte)var23[3]).byteValue();
									} else if(var6 == Packet.LEVEL_INITIALIZE) {
										var26.minecraft.setLevel((Level)null);
										var26.levelBuffer = new ByteArrayOutputStream();
									} else {
										byte var8;
										if(var6 == Packet.LEVEL_DATA_CHUNK) {
											short var32 = ((Short)var23[0]).shortValue();
											byte[] var7 = (byte[])((byte[])var23[1]);
											var8 = ((Byte)var23[2]).byteValue();
											var26.minecraft.loadingScreen.setLoadingProgress(var8);
											var26.levelBuffer.write(var7, 0, var32);
										} else {
											short var34;
											short var36;
											short var39;
											if(var6 == Packet.LEVEL_FINALIZE) {
												try {
													var26.levelBuffer.close();
												} catch (IOException var12) {
													var12.printStackTrace();
												}

												byte[] var33 = LevelIO.loadBlocks(new ByteArrayInputStream(var26.levelBuffer.toByteArray()));
												var26.levelBuffer = null;
												var36 = ((Short)var23[0]).shortValue();
												var39 = ((Short)var23[1]).shortValue();
												var34 = ((Short)var23[2]).shortValue();
												Level var9 = new Level();
												var9.setNetworkMode(true);
												var9.setData(var36, var39, var34, var33);
												var26.minecraft.setLevel(var9);
												var26.minecraft.hideGui = false;
											} else if(var6 == Packet.SET_TILE) {
												if(var26.minecraft.level != null) {
													var26.minecraft.level.netSetTile(((Short)var23[0]).shortValue(), ((Short)var23[1]).shortValue(), ((Short)var23[2]).shortValue(), ((Byte)var23[3]).byteValue());
												}
											} else {
												byte var10;
												short var10003;
												short var10004;
												String var35;
												NetworkPlayer var37;
												byte var48;
												if(var6 == Packet.PLAYER_JOIN) {
													var48 = ((Byte)var23[0]).byteValue();
													String var10002 = (String)var23[1];
													var10003 = ((Short)var23[2]).shortValue();
													var10004 = ((Short)var23[3]).shortValue();
													short var10005 = ((Short)var23[4]).shortValue();
													byte var10006 = ((Byte)var23[5]).byteValue();
													byte var11 = ((Byte)var23[6]).byteValue();
													var10 = var10006;
													short var41 = var10005;
													var39 = var10004;
													var36 = var10003;
													var35 = var10002;
													var5 = var48;
													if(var5 >= 0) {
														var37 = new NetworkPlayer(var26.minecraft, var5, var35, var36, var39, var41, (float)(-var10 * 360) / 256.0F, (float)(var11 * 360) / 256.0F);
														var26.players.put(Byte.valueOf(var5), var37);
														var26.minecraft.level.entities.add(var37);
													} else {
														var26.minecraft.level.setSpawnPos(var36 / 32, var39 / 32, var41 / 32, (float)(var10 * 320 / 256));
														var26.minecraft.player.moveTo((float)var36 / 32.0F, (float)var39 / 32.0F, (float)var41 / 32.0F, (float)(var10 * 360) / 256.0F, (float)(var11 * 360) / 256.0F);
													}
												} else {
													byte var43;
													NetworkPlayer var46;
													byte var53;
													if(var6 == Packet.PLAYER_TELEPORT) {
														var48 = ((Byte)var23[0]).byteValue();
														short var49 = ((Short)var23[1]).shortValue();
														var10003 = ((Short)var23[2]).shortValue();
														var10004 = ((Short)var23[3]).shortValue();
														var53 = ((Byte)var23[4]).byteValue();
														var10 = ((Byte)var23[5]).byteValue();
														var43 = var53;
														var39 = var10004;
														var36 = var10003;
														var34 = var49;
														var5 = var48;
														if(var5 < 0) {
															var26.minecraft.player.moveTo((float)var34 / 32.0F, (float)var36 / 32.0F, (float)var39 / 32.0F, (float)(var43 * 360) / 256.0F, (float)(var10 * 360) / 256.0F);
														} else {
															var46 = (NetworkPlayer)var26.players.get(Byte.valueOf(var5));
															if(var46 != null) {
																var46.teleport(var34, var36, var39, (float)(-var43 * 360) / 256.0F, (float)(var10 * 360) / 256.0F);
															}
														}
													} else {
														byte var38;
														byte var40;
														byte var50;
														byte var51;
														if(var6 == Packet.PLAYER_MOVE_AND_ROTATE) {
															var48 = ((Byte)var23[0]).byteValue();
															var50 = ((Byte)var23[1]).byteValue();
															var51 = ((Byte)var23[2]).byteValue();
															byte var52 = ((Byte)var23[3]).byteValue();
															var53 = ((Byte)var23[4]).byteValue();
															var10 = ((Byte)var23[5]).byteValue();
															var43 = var53;
															var8 = var52;
															var40 = var51;
															var38 = var50;
															var5 = var48;
															if(var5 >= 0) {
																var46 = (NetworkPlayer)var26.players.get(Byte.valueOf(var5));
																if(var46 != null) {
																	var46.queue(var38, var40, var8, (float)(-var43 * 360) / 256.0F, (float)(var10 * 360) / 256.0F);
																}
															}
														} else if(var6 == Packet.PLAYER_ROTATE) {
															var48 = ((Byte)var23[0]).byteValue();
															var50 = ((Byte)var23[1]).byteValue();
															var40 = ((Byte)var23[2]).byteValue();
															var38 = var50;
															var5 = var48;
															if(var5 >= 0) {
																NetworkPlayer var44 = (NetworkPlayer)var26.players.get(Byte.valueOf(var5));
																if(var44 != null) {
																	var44.queue((float)(-var38 * 360) / 256.0F, (float)(var40 * 360) / 256.0F);
																}
															}
														} else if(var6 == Packet.PLAYER_MOVE) {
															var48 = ((Byte)var23[0]).byteValue();
															var50 = ((Byte)var23[1]).byteValue();
															var51 = ((Byte)var23[2]).byteValue();
															var8 = ((Byte)var23[3]).byteValue();
															var40 = var51;
															var38 = var50;
															var5 = var48;
															if(var5 >= 0) {
																NetworkPlayer var45 = (NetworkPlayer)var26.players.get(Byte.valueOf(var5));
																if(var45 != null) {
																	var45.queue(var38, var40, var8);
																}
															}
														} else if(var6 == Packet.PLAYER_DISCONNECT) {
															var5 = ((Byte)var23[0]).byteValue();
															if(var5 >= 0) {
																var37 = (NetworkPlayer)var26.players.remove(Byte.valueOf(var5));
																if(var37 != null) {
																	var37.clear();
																	var26.minecraft.level.entities.remove(var37);
																}
															}
														} else if(var6 == Packet.CHAT_MESSAGE) {
															var48 = ((Byte)var23[0]).byteValue();
															var35 = (String)var23[1];
															var5 = var48;
															if(var5 < 0 && var35 != "") {
																var26.minecraft.hud.addChatMessage("&e" + var35);
															} else {
																var26.players.get(Byte.valueOf(var5));
																var26.minecraft.hud.addChatMessage(var35);
															}
														} else if(var6 == Packet.KICK_PLAYER) {
															var26.minecraft.setScreen(new ErrorScreen("Connection lost", (String)var23[0]));
															var26.connection.disconnect();
														}
													}
												}
											}
										}
									}
								}
								var20.readBuffer.compact();
							}
							var20.flush();
							} catch (Exception var13) {
							var16.minecraft.setScreen(new ErrorScreen("Disconnected!", "You\'ve lost connection to the server"));
							var16.minecraft.hideGui = false;
							var13.printStackTrace();
							var16.connection.disconnect();
							var16.minecraft.connectionManager = null;
						}
					}
				}
				Player var27 = this.player;
				var16 = this.connectionManager;
				if(var16.isConnected()) {
					int var22 = (int)(var27.x * 32.0F);
					var4 = (int)(var27.y * 32.0F);
					var28 = (int)(var27.z * 32.0F);
					int var42 = (int)(var27.yRot * 256.0F / 360.0F) & 255;
					var17 = (int)(var27.xRot * 256.0F / 360.0F) & 255;
					var16.connection.sendPacket(Packet.PLAYER_TELEPORT, new Object[]{Integer.valueOf(-1), Integer.valueOf(var22), Integer.valueOf(var4), Integer.valueOf(var28), Integer.valueOf(var42), Integer.valueOf(var17)});
				}
			}
		}


		LevelRenderer var25;
		if(this.screen == null || this.screen.allowUserInput) {
			if(Mouse.isMouseGrabbed() || Mouse.isActuallyGrabbed()) {
				this.mouseGrabbed = true;
			}
			label251:
			while(Mouse.next()) {
				int var18 = Mouse.getEventDWheel();
				if(var18 != 0) {
					this.player.inventory.scrollHotbar(var18);
				}

				if(this.screen == null) {
					if(!this.mouseGrabbed && Mouse.getEventButtonState()) {
						this.grabMouse();
					} else {
						if(Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
							this.clickMouse();
							this.prevFrameTime = this.ticksRan;
						}

						if(Mouse.getEventButton() == 1 && Mouse.getEventButtonState()) {
							this.editMode = (this.editMode + 1) % 2;
						}

						if(Mouse.getEventButton() == 2 && Mouse.getEventButtonState() && this.hitResult != null) {
							var17 = this.level.getTile(this.hitResult.x, this.hitResult.y, this.hitResult.z);
							if(var17 == Tile.grass.id) {
								var17 = Tile.dirt.id;
							}

							Inventory var24 = this.player.inventory;
							var28 = var24.getSlotContainsID(var17);
							if(var28 >= 0) {
								var24.selectedSlot = var28;
							} else if(var17 > 0 && User.creativeTiles.contains(Tile.tiles[var17])) {
								var24.getSlotContainsTile(Tile.tiles[var17]);
							}
						}
					}
				}

				if(this.screen != null) {
					this.screen.updateMouseEvents();
				}
			}

			label298:
			while(true) {
				do {
					do {
						if(!Keyboard.next()) {
							if(this.screen == null && Mouse.isButtonDown(0) && (float)(this.ticksRan - this.prevFrameTime) >= this.timer.ticksPerSecond / 4.0F && this.mouseGrabbed) {
								this.clickMouse();
								this.prevFrameTime = this.ticksRan;
							}
							break label298;
						}

						this.player.setKey(Keyboard.getEventKey(), Keyboard.getEventKeyState());
					} while(!Keyboard.getEventKeyState());

					if(this.screen != null) {
						this.screen.updateKeyboardEvents();
					}

					if(this.screen == null) {
						if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
							this.pauseGame();
						}

						if(Keyboard.getEventKey() == Keyboard.KEY_R) {
							this.player.resetPos();
						}

						if(Keyboard.getEventKey() == Keyboard.KEY_M) {
							soundManager.enabled = !soundManager.enabled;
							soundManager.settingsChanged();
						}

						if(Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
							this.level.setSpawnPos((int)this.player.x, (int)this.player.y, (int)this.player.z, this.player.yRot);
							this.player.resetPos();
						}

						if(Keyboard.getEventKey() == Keyboard.KEY_G && this.connectionManager == null && this.level.entities.size() < 256) {
							this.level.entities.add(new Zombie(this.level, this.player.x, this.player.y, this.player.z));
						}

						if(Keyboard.getEventKey() == Keyboard.KEY_B) {
							this.setScreen(new InventoryScreen());
						}

						if(Keyboard.getEventKey() == Keyboard.KEY_T && this.connectionManager != null && this.connectionManager.isConnected()) {
							this.player.releaseAllKeys();
							this.setScreen(new ChatScreen());
						}
					}

					for(int var18 = 0; var18 < 9; ++var18) {
						if(Keyboard.getEventKey() == var18 + 2) {
							this.player.inventory.selectedSlot = var18;
						}
					}

					if(Keyboard.getEventKey() == Keyboard.KEY_Y) {
						this.yMouseAxis = -this.yMouseAxis;
					}
				} while(Keyboard.getEventKey() != Keyboard.KEY_F);

				LevelRenderer var47 = this.levelRenderer;
				boolean var31 = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
				var25 = var47;
				var25.drawDistance = var25.drawDistance + (var31 ? -1 : 1) & 3;
			}
		}

		if(this.screen != null) {
			this.prevFrameTime = this.ticksRan + 10000;
		}

		if(this.screen != null) {
			this.screen.updateEvents();
			if(this.screen != null) {
				this.screen.tick();
			}
		}
		if(this.connectionManager != null) {
			this.connectionManager.tick();
		}
		if(this.level != null) {
			var25 = this.levelRenderer;
			++var25.cloudTickCounter;
			this.level.tickEntities();
			if(!this.isMultiplayer()) {
				this.level.tick();
			}

			this.particleEngine.tick();
			this.player.tick();
			if(this.connectionManager == null) {
				levelSave();
			}
		}
	}

	private boolean isMultiplayer() {
		return this.connectionManager != null;
	}

	public final void generateLevel(int var1) {
		String var2 = this.user != null ? this.user.name : "anonymous";
		this.setLevel(this.levelGen.generateLevel(var2, 128 << var1, 128 << var1, 64));
	}

	public final void setLevel(Level var1) {
		this.level = var1;
		if(var1 != null) {
			var1.rendererContext = this;
		}

		if(this.levelRenderer != null) {
			LevelRenderer var2 = this.levelRenderer;
			if(var2.level != null) {
				var2.level.removeListener(var2);
			}

			var2.level = var1;
			if(var1 != null) {
				var1.addListener(var2);
				var2.compileSurroundingGround();
			}
		}

		if(this.particleEngine != null) {
			ParticleEngine var4 = this.particleEngine;
			var4.particles.clear();
		}

		if(this.player != null) {
			this.player.setLevel(var1);
			this.player.resetPos();
		}

		System.gc();
	}
}
