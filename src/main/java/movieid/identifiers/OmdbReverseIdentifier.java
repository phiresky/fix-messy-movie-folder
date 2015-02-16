package movieid.identifiers;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import movieid.MovieInfo;
import movieid.util.CachedHashMap;

/**
 * Identifies movies using title search from a filename matching the output of
 * this program ( {Title} ({year}).* )
 */
public class OmdbReverseIdentifier extends MovieIdentifier {
	Pattern PATTERN = Pattern.compile("(.*) \\(([12][0-9][0-9][0-9])\\) .*");
	private CachedHashMap<String, String> ids = new CachedHashMap<>("omdb-title-search-cache");

	@Override public MovieInfo tryIdentifyMovie(Path input) {
		String fname = input.getFileName().toString();
		Matcher m = PATTERN.matcher(fname);
		if (m.matches()) {
			String title = m.group(1).replace('-', ' ');
			String year = m.group(2);
			String id = ids.getCached(title + " " + year, () -> {
				return ImdbId.fromTitleAndYear(title, year).map(i -> i.getId()).orElse(null);
			});
			if (id != null)
				return MovieInfo.fromImdb(input, ImdbId.fromId(id));
		}
		return null;
	}

}
