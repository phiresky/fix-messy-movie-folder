package movieid.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import movieid.Main;

@RequiredArgsConstructor
public class CachedHashMap<K, V> implements Map<K, V> {
	private final HashMap<K, V> map;

	public CachedHashMap(String filename) {
		map = CachedHashMap.<HashMap<K, V>> tryReadSerialized(filename).orElseGet(() -> {
			Main.log(2, "creating new " + filename);
			return new HashMap<K, V>();
		});
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Main.log(2, "Writing " + filename);
				writeSerialized(filename, map);
			}
		});
	}

	private static <T> Optional<T> tryReadSerialized(String filename) {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
			@SuppressWarnings("unchecked")
			T c = (T) in.readObject();
			return Optional.of(c);
		} catch (FileNotFoundException e) {
			// ignore
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	private static void writeSerialized(String filename, Object obj) {
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
			out.writeObject(obj);
			out.close();
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
