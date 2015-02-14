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
	public static final String METADATA_SEPERATOR = "\t";
	private Map<String, ImdbId> cache = new HashMap<>();

	@Override public MovieInfo tryIdentifyMovie(Path input) {
		String fname = input.getFileName().toString();
		if (cache.containsKey(fname))
			return MovieInfo.fromImdb(input, cache.get(fname));
		Path metafile = input.getParent().resolve(METADATA_FILENAME);
		if (Files.isRegularFile(metafile)) {
			try {
				Files.lines(metafile).filter(line -> !line.startsWith("#"))
						.map(line -> line.split(METADATA_SEPERATOR))
						.forEach(l -> cache.put(l[0], ImdbId.fromId(l[1])));
				return MovieInfo.fromImdb(input, cache.get(fname));
			} catch (IOException e) {
				Main.log(1, "Error reading " + metafile);
				e.printStackTrace();
			}
		}
		return null;
	}

}
