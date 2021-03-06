package movieid;

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;
import movieid.identifiers.ImdbId;
import movieid.util.Util;

public class MovieInfo {
	public static final String DEFAULT_FORMAT = "{Title} ({Year})";
	public static final String DEFAULT_FILENAME = "{Title} ({Year}) [{imdbRating}].{Extension}";

	@Getter
	@Setter
	private Path path;
	private boolean hasMetadata = false;
	@Getter
	private ImdbId imdbId;
	@Getter
	private Map<String, String> information;
	private static Set<String> multivalueKeys = new HashSet<String>(Arrays.asList("Country",
			"Genre", "Director"));

	public boolean hasMetadata() {
		return hasMetadata;
	}

	public static MovieInfo fromImdb(ImdbId imdbId) {
		MovieInfo info = new MovieInfo();
		info.hasMetadata = true;
		info.imdbId = imdbId;
		info.information = imdbId.getMovieInfo();
		return info;
	}

	public static MovieInfo fromImdb(Path file, ImdbId imdbId) {
		MovieInfo info = new MovieInfo();
		info.path = file;
		info.hasMetadata = true;
		info.imdbId = imdbId;
		info.information = imdbId.getMovieInfo();
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

	public List<String> getInformationValues(String name) {
		if (multivalueKeys.contains(name))
			return Stream.of(information.get(name).split(",")).map(String::trim).collect(toList());
		return Arrays.asList(information.get(name));
	}

	/**
	 * @return movie runtime in minutes
	 */
	public Optional<Integer> getRuntime() {
		String runtime = information.get("Runtime");
		runtime = runtime.replace(" min", "");
		return Util.parseInt(runtime);
	}
}