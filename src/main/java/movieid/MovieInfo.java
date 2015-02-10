package movieid;

import identifiers.ImdbId;

import java.nio.file.Path;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class MovieInfo {
	public static final String DEFAULT_FORMAT = "{Title} ({Year})";
	public static final String DEFAULT_FILENAME = "{Title} ({Year}).{Extension}";

	@Getter
	@Setter
	private Path path;
	private boolean hasMetadata = false;
	@Getter
	private ImdbId imdbId;
	@Getter
	private Map<String, String> information;

	public boolean hasMetadata() {
		return hasMetadata;
	}

	public static MovieInfo fromImdb(ImdbId imdbId) {
		MovieInfo info = new MovieInfo();
		info.hasMetadata = true;
		info.imdbId = imdbId;
		info.information = ImdbUtil.getMovieInfo(imdbId);
		return info;
	}

	public static MovieInfo fromImdb(Path file, ImdbId imdbId) {
		MovieInfo info = new MovieInfo();
		info.path = file;
		info.hasMetadata = true;
		info.imdbId = imdbId;
		info.information = ImdbUtil.getMovieInfo(imdbId);
		return info;
	}

	public String toString() {
		if (!hasMetadata()) {
			return "unknown[" + path + "]";
		}
		return format(DEFAULT_FORMAT) + "[" + path + "]";
	}

	public static MovieInfo empty(Path path) {
		MovieInfo m = new MovieInfo();
		m.setPath(path);
		return m;
	}

	public String format(String format) {
		for (Map.Entry<String, String> entry : information.entrySet()) {
			format = format.replace("{" + entry.getKey() + "}", entry.getValue());
		}
		if (path != null)
			format = format.replace("{Extension}", Util.getFileExtension(path));
		return format;
	}
}