package movieid.identifiers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import movieid.Main;
import movieid.MovieInfo;

/**
 * Identifies movies using the metadata.csv this program outputs
 */
public class MetadataCsvIdentifier extends MovieIdentifier {
	public final static String METADATA_FILENAME = ".metadata.csv";
	private Map<String, ImdbId> cache = new HashMap<>();

	@Override public MovieInfo tryIdentifyMovie(Path input) {
		if (cache.containsKey(input.getFileName().toString()))
			return MovieInfo.fromImdb(input, cache.get(input.getFileName()));
		Path metafile = input.getParent().resolve(METADATA_FILENAME);
		if (Files.isRegularFile(metafile)) {
			try {
				Files.lines(metafile).map(line -> line.split(","))
						.forEach(l -> cache.put(l[0], ImdbId.fromId(l[1])));
				return MovieInfo.fromImdb(input, cache.get(input.getFileName().toString()));
			} catch (IOException e) {
				Main.log(1, "Error reading " + metafile);
				e.printStackTrace();
			}
		}
		return null;
	}

}
