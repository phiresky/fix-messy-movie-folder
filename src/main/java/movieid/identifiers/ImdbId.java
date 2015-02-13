package movieid.identifiers;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import movieid.Main;
import movieid.util.CachedHashMap;

import org.json.JSONObject;
import org.json.JSONTokener;

@RequiredArgsConstructor
@Data
public class ImdbId implements Serializable {
	private static final long serialVersionUID = 1L;

	@Getter
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

	private static CachedHashMap<String, Map<String, String>> imdbidcache = new CachedHashMap<>(
			"imdb-id-cache");

	public Map<String, String> getMovieInfo() {
		return imdbidcache.getCached(id, () -> {
			try {
				Main.log(2, "loading omdb " + id);
				JSONObject data = new JSONObject(new JSONTokener(new URL(
						"http://www.omdbapi.com/?i=" + id).openStream()));
				return data.keySet().stream().collect(toMap(key -> key, data::getString));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		});

	}
}
