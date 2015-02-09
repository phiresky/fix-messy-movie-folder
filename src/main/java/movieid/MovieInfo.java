package movieid;

import java.nio.file.Path;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MovieInfo {
	@Getter
	private final Path path;
	private final boolean hasMetadata;
	private String imdbId;
	private String title;

	public boolean hasMetadata() {
		return hasMetadata;
	}

	public static MovieInfo fromImdb(Path path, String imdbUrl) {
		return fromImdb(path, imdbUrl, null);
	}

	public static MovieInfo fromImdb(Path path, String imdbUrl, String title) {
		MovieInfo info = new MovieInfo(path, true);
		info.imdbId = imdbUrl;
		info.title = title;
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
}