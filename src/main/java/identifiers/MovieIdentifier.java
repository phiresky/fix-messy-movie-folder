package identifiers;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import movieid.MovieInfo;

public abstract class MovieIdentifier {
	public static List<MovieIdentifier> IDENTIFIERS = Arrays.asList(new NfoMovieIdentifier(), new GoogleSearchMovieIdentifier(), new ImdbTitleSearchMovieIdentifier());

	public abstract MovieInfo tryIdentifyMovie(Path input);

	public static MovieInfo tryAllIdentify(Path input) {
		return IDENTIFIERS.stream()
				.map(identifier -> identifier.tryIdentifyMovie(input))
				.filter(x -> x != null).findFirst()
				.orElse(MovieInfo.empty(input));
	}
}
