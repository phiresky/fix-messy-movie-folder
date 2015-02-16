package movieid.identifiers;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import movieid.Main;
import movieid.MovieInfo;
import movieid.util.CachedHashMap;
import movieid.util.Util;

/**
 * Identifies movies by searching some search engine for the file name, removing
 * some common strings such as "BluRay". Also tests folder names:
 * 
 * The uppermost folder name that only has this video file is searched first,
 * then the subfolders and in the end the file name. Example:
 * 
 * input:
 * <code>/Action/Knight.and.Day.Extended.Cut.German.720p.BluRay.x264-DECENT/dec-kade.720p.mkv</code>
 * 
 * searches: "Knight and Day Extended Cut", and if that is not found, searches
 * "dec kade".
 */
public abstract class FilenameMovieIdentifier extends MovieIdentifier {
	private final CachedHashMap<String, String> searchcache;
	private String log;
	private Function<String, Optional<ImdbId>> searchfn;
	private String sourcename;

	public FilenameMovieIdentifier(String sourcename, Function<String, Optional<ImdbId>> searchfn) {
		this.sourcename = sourcename;
		searchcache = new CachedHashMap<>(sourcename + "-search-cache");
		this.searchfn = searchfn;
	}

	@Override public MovieInfo tryIdentifyMovie(Path input) {
		log = "";
		MovieInfo info = Util.getIdentificationStrings(input).stream()
				.peek(e -> log += "tried " + e + "\n").map(this::tryIdentifyMovie)
				.filter(Objects::nonNull).findFirst().orElse(null);
		if (info == null) {
			Main.log(2, sourcename + ": Could not find " + input.getFileName() + ":\n" + log);
		} else {
			info.setPath(input);
		}
		return info;
	}

	public MovieInfo tryIdentifyMovie(String search) {
		String imdbid = searchcache.getCached(search,
				() -> searchfn.apply(search).map(x -> x.getId()).orElse(null));
		if (imdbid == null)
			return null;
		MovieInfo x = MovieInfo.fromImdb(ImdbId.fromId(imdbid));
		Main.log(3, sourcename + ": " + search + " -> " + x);
		return x;
	}
}
