package movieid.util;

import static java.util.stream.Collectors.toMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lombok.RequiredArgsConstructor;
import movieid.Main;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

@RequiredArgsConstructor
public class CachedHashMap<K, V> implements Map<K, V> {
	private final HashMap<K, V> map;
	private final static HashMap<String, HashMap<?, ?>> maps;
	private final static String CACHE_NAME = "movieid-cache.json.gz";

	static {
		maps = CachedHashMap.<HashMap<String, HashMap<?, ?>>> tryReadSerialized(CACHE_NAME)
				.orElseGet(() -> {
					Main.log(2, "creating new cache");
					return new HashMap<String, HashMap<?, ?>>();
				});
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Main.log(2, "Writing " + "movieid-cache.json.gz");
				writeSerialized(CACHE_NAME, maps);
			}
		});
	}

	@SuppressWarnings("unchecked") public CachedHashMap(String cachename) {
		if (!maps.containsKey(cachename))
			maps.put(cachename, new HashMap<>());
		map = (HashMap<K, V>) maps.get(cachename);

	}

	private static <T> Optional<T> tryReadSerialized(String filename) {
		try (JsonReader in = new JsonReader(new GZIPInputStream(new FileInputStream(filename)))) {
			@SuppressWarnings("unchecked")
			T c = (T) in.readObject();
			return Optional.of(c);
		} catch (FileNotFoundException e) {
			// ignore
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	private static void writeSerialized(String filename, Object obj) {
		Map<String, Object> args = Stream.of(0).collect(toMap(x -> JsonWriter.PRETTY_PRINT, x -> true));
		try (JsonWriter out = new JsonWriter(new GZIPOutputStream(new FileOutputStream(filename)),
				args)) {
			out.write(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override public int size() {
		return map.size();
	}

	@Override public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override public V get(Object key) {
		return map.get(key);
	}

	public V getCached(K key, Supplier<V> otherwise) {
		if (!containsKey(key)) {
			put(key, otherwise.get());
		}
		return get(key);
	}

	@Override public V put(K key, V value) {
		return map.put(key, value);
	}

	@Override public V remove(Object key) {
		return map.remove(key);
	}

	@Override public void putAll(Map<? extends K, ? extends V> m) {
		map.putAll(m);
	}

	@Override public void clear() {
		map.clear();
	}

	@Override public Set<K> keySet() {
		return map.keySet();
	}

	@Override public Collection<V> values() {
		return map.values();
	}

	@Override public Set<java.util.Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}
}
