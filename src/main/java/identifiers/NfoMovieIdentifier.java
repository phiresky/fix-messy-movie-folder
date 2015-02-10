package identifiers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import movieid.MovieInfo;
import movieid.Util;

public class NfoMovieIdentifier extends MovieIdentifier {

	@Override
	public MovieInfo tryIdentifyMovie(Path path) {
		Path nfo = Paths.get(path.toString().replaceFirst("\\.[^.]+$", ".nfo"));
		if (nfo.toFile().isFile()) {
			try {
				return Util
						.regexInFile(Util.IMDBURL, nfo)
						.map(imdburl -> MovieInfo.fromImdb(path,
								ImdbId.fromUrl(imdburl)))
						.orElseGet(() -> {
							System.out.println("Invalid nfo: " + nfo);
							return null;
						});

			} catch (UncheckedIOException | IOException e) {
				System.out.println("Error in file " + nfo);
				e.printStackTrace();
			}
		}
		return null;
	}

}
