package movieid.identifiers;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import movieid.Main;
import movieid.util.CachedHashMap;
import movieid.util.Util;

import org.json.JSONObject;

@RequiredArgsConstructor
@Data
public class ImdbId implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String id;

	private static Pattern IMDBID_PATTERN = Pattern.compile("(tt\\d+)");
	private static Pattern IMDBURL_PATTERN = Pattern.compile("imdb\\.com/title/" + IMDBID_PATTERN);

	public static ImdbId fromUrl(@NonNull String url) {
		Matcher matcher = IMDBURL_PATTERN.matcher(url);
		matcher.find();
		return new ImdbId(matcher.group(1));
	}

	public static ImdbId fromId(@NonNull String imdbid) {
		if (!IMDBID_PATTERN.matcher(imdbid).matches()) {
			throw new IllegalArgumentException("not an imdb id " + imdbid);
		}
		return new ImdbId(imdbid);
	}

	public static Optional<ImdbId> fromTitleAndYear(@NonNull String title, @NonNull String year) {
		String url = Util.addUrlParam("http://www.omdbapi.com/?type=movie&t=%s&y=%s", title, year);
		Main.log(3, "getting: " + url);
		try {
			JSONObject data = Util.getJSON(url);
			if (data.getString("Response").equals("False")) {
				Main.log(2, data.getString("Error"));
				return Optional.empty();
			} else {
				String id = data.getString("imdbID");
				imdbidcache.put(id,
						data.keySet().stream().collect(toMap(key -> key, data::getString)));
				return Optional.of(ImdbId.fromId(id));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	private static CachedHashMap<String, Map<String, String>> imdbidcache = new CachedHashMap<>(
			"imdb-id-cache");

	public Map<String, String> getMovieInfo() {
		return imdbidcache.getCached(id, () -> {
			try {
				Main.log(2, "loading omdb " + id);
				JSONObject data = Util.getJSON("http://www.omdbapi.com/?i=" + id);
				return data.keySet().stream().collect(toMap(key -> key, data::getString));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		});
	}

	public String toString() {
		return id;
	}
}
