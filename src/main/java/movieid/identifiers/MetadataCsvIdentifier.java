package movieid.identifiers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import movieid.Main;
import movieid.MovieInfo;

/**
 * Identifies movies using the metadata.csv this program outputs in
 * {@link Main#writeMetadata}
 */
public class MetadataCsvIdentifier extends MovieIdentifier {
	public final static String METADATA_FILENAME = ".metadata.csv";
	public static final String METADATA_SEPERATOR = "\t";
	private Map<String, ImdbId> cache = new HashMap<>();
	private Set<Path> checked = new HashSet<>();

	@Override public MovieInfo tryIdentifyMovie(Path input) {
		String fname = input.getFileName().toString();
		if (cache.containsKey(fname))
			return MovieInfo.fromImdb(input, cache.get(fname));
		Path metafile = input.getParent().resolve(METADATA_FILENAME);
		if (checked.contains(metafile))
			return null;
		checked.add(metafile);
		if (Files.isRegularFile(metafile)) {
			try {
				Files.lines(metafile).filter(line -> !line.startsWith("#"))
						.map(line -> line.split(METADATA_SEPERATOR))
						.forEach(l -> cache.put(l[0], ImdbId.fromId(l[1])));
				Main.log(2, "Imported metadata from " + metafile);
				if (cache.containsKey(fname)) {
					return MovieInfo.fromImdb(input, cache.get(fname));
				} else
					return null;
			} catch (IOException e) {
				Main.log(1, "Error reading " + metafile);
				e.printStackTrace();
			}
		}
		return null;
	}

}
