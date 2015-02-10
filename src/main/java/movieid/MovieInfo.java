package movieid;

import java.nio.file.Path;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class MovieInfo {
	@Getter @Setter @NonNull
	private Path path;
	private final boolean hasMetadata;
	@Getter
	private String imdbId;
	@Getter
	private Map<String, String> information;

	public boolean hasMetadata() {
		return hasMetadata;
	}

	public static MovieInfo fromImdb(Path path, String imdbId) {
		MovieInfo info = new MovieInfo(path, true);
		info.imdbId = imdbId;
		info.information = ImdbUtil.getMovieInfo(imdbId);
		return info;
	}

	public String toString() {
		if (!hasMetadata()) {
			return "unknown: " + path;
		}
		return imdbId + ": " + path;
	}

	public static MovieInfo empty(Path path) {
		return new MovieInfo(path, false);
	}

	public String format(String format) {
		for(Map.Entry<String, String> entry: information.entrySet()) {
			format = format.replace("{"+entry.getKey()+"}", entry.getValue());
		}
		format = format.replace("{Extension}", Util.getFileExtension(path));
		return format;
	}
}