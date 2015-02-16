package movieid.identifiers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import movieid.Main;
import movieid.MovieInfo;
import movieid.util.Util;

/**
 * When there is a .nfo file with the same name as the movie, search that for
 * imdb links
 */
public class NfoMovieIdentifier extends MovieIdentifier {

	@Override public MovieInfo tryIdentifyMovie(Path path) {
		Path nfo = Paths.get(path.toString().replaceFirst("\\.[^.]+$", ".nfo"));
		if (nfo.toFile().isFile()) {
			try {
				return Util.regexInFile(Util.IMDBURL, nfo)
						.map(imdburl -> MovieInfo.fromImdb(path, ImdbId.fromUrl(imdburl)))
						.orElseGet(() -> {
							Main.log(1, "Invalid nfo: " + nfo);
							return null;
						});

			} catch (UncheckedIOException | IOException e) {
				Main.log(1, "Error in file " + nfo);
				e.printStackTrace();
			}
		}
		return null;
	}

}
