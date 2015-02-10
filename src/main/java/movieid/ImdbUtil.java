package movieid;

import static java.util.stream.Collectors.toMap;
import identifiers.CachedHashMap;
import identifiers.ImdbId;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

public class ImdbUtil {
	private static CachedHashMap<String, Map<String, String>> imdbidcache = new CachedHashMap<>(
			"imdb-id-cache");

	public static Map<String, String> getMovieInfo(ImdbId imdbId) {
		String id = imdbId.getId();
		return imdbidcache.getCached(
				id,
				() -> {
					try {
						System.out.println("loading omdb " + id);
						JSONObject data = new JSONObject(new JSONTokener(
								new URL("http://www.omdbapi.com/?i=" + id)
										.openStream()));
						return data.keySet().stream()
								.collect(toMap(key -> key, data::getString));
					} catch (IOException e) {
						e.printStackTrace();
					}
					return null;
				});

	}
}
