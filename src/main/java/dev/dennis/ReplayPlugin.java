package dev.dennis;

import com.google.inject.Provides;
import javax.inject.Inject;

import dev.dennis.proxy.ProxyServer;
import dev.dennis.proxy.record.RecordClientInitializer;
import dev.dennis.proxy.replay.ReplayClientInitializer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

@Slf4j
@PluginDescriptor(
	name = "Replay"
)
public class ReplayPlugin extends Plugin
{
	private static final int PORT = 43594;

	@Inject
	private Client client;

	@Inject
	private WorldService worldService;

	@Inject
	private ReplayConfig config;

	private final ProxyServer proxyServer;

	private final RecordClientInitializer recordClientInitializer;
	private final ReplayClientInitializer replayClientInitializer;

	private int lastWorld = -1;

	public ReplayPlugin() {
		this.proxyServer = new ProxyServer();
		this.recordClientInitializer = new RecordClientInitializer(this);
		this.replayClientInitializer = new ReplayClientInitializer(this);
	}

	@Provides
	ReplayConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ReplayConfig.class);
	}

	public <T> T getStaticField(String className, String fieldName) throws ClassNotFoundException,
			NoSuchFieldException, IllegalAccessException {
		Class<?> clazz = this.client.getClass().getClassLoader().loadClass(className);

		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);

		return (T) field.get(null);
	}

	public static <T> T getField(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);

		return (T) field.get(object);
	}

	public static Method getMethod(Object object, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Method method = object.getClass().getDeclaredMethod(methodName, parameterTypes);
		method.setAccessible(true);

		return method;
	}

	public int[] getIsaacKey() {
        try {
            return getStaticField("cq", "hc");
        } catch (Exception e) {
			log.error("Couldn't get ISAAC key", e);
			return null;
        }
    }

	public Object getNetWriter() throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		return getStaticField("client", "hn");
	}

	public Object getNetWriterPacketBuffer() throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
		Object netWriter = this.getNetWriter();
		return getField(netWriter, "ao");
	}

	public void seedIsaac(int[] key) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Object packetBuffer = getNetWriterPacketBuffer();

		Method seedMethod = getMethod(packetBuffer, "az", int[].class, short.class);
		seedMethod.invoke(packetBuffer, key, (short) -25131);
	}

	@Override
	protected void startUp() throws Exception
	{
		lastWorld = -1;

		boolean record = true;
		if (record) {
			this.proxyServer.start(PORT, this.recordClientInitializer);
		} else {
			this.proxyServer.start(PORT, this.replayClientInitializer);
			this.replayClientInitializer.setRecordingPath(Paths.get("recordings", "2024-05-14_01-56-16"));
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
		this.proxyServer.stop();
	}

	private World getWorldData(int worldId)
	{
		WorldResult result = worldService.getWorlds();
		if (result == null) {
			return null;
		}
		return result.findWorld(client.getWorld());
	}

	private boolean updateWorld(int worldId)
	{
		World world = this.getWorldData(worldId);
		if (world == null) {
			return false;
		}

		this.updateWorld(world);
		return true;
	}

	private void updateWorld(World world)
	{
		log.info("Updating world {}, {}", world.getId(), world.getAddress());

		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress("127.0.0.1");
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		client.changeWorld(rsWorld);

		this.recordClientInitializer.setAddress(world.getAddress());
		this.recordClientInitializer.setPort(PORT); // TODO: get this from the client
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		if (this.lastWorld != client.getWorld()) {
			this.updateWorld(client.getWorld());
			this.lastWorld = client.getWorld();
		}
	}
}
