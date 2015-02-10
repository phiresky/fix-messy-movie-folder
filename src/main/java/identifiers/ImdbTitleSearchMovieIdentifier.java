package identifiers;

import movieid.ImdbUtil;
import movieid.MovieInfo;

public class ImdbTitleSearchMovieIdentifier extends FilenameMovieIdentifier {
	@Override
	public MovieInfo tryIdentifyMovie(String searchname) {
		return ImdbUtil.getMovieInfoFromSearch(searchname);
	}
}
