package identifiers;

import java.nio.file.Path;
import java.util.regex.Pattern;

import movieid.ImdbUtil;
import movieid.MovieInfo;
import movieid.Util;

public class ImdbTitleSearchMovieIdentifier extends MovieIdentifier {
	private static Pattern IGNORE = Pattern
			.compile(
					"1080p|DL|DTS|720p|recut|xvid|\\bsow|bluray|x264|\\bPS|AC3D|dvdrip|ac3hd|wodkae|bublik|german|viahd|\\bld\\b|noelite|blubyte",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
							| Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern NONALPHA = Pattern.compile(
			"[^\\p{Alpha}\\p{Digit}]+", Pattern.UNICODE_CHARACTER_CLASS);

	@Override
	public MovieInfo tryIdentifyMovie(Path input) {
		MovieInfo info = tryIdentifyMovie(input, true);
		if (info == null) {
			System.out.println("Could not find " + input.getFileName());
		}
		return info;
	}

	public MovieInfo tryIdentifyMovie(Path input, boolean isFile) {
		String filename = input.getFileName().toString();
		String searchname = filenameToMoviename(filename, isFile);
		MovieInfo x = ImdbUtil.getMovieInfoFromSearch(input, searchname);
		if (x == null && Util.walkMovies(input.getParent()).count() == 1)
			return tryIdentifyMovie(input.getParent(), false);
		return x;
	}

	public static String filenameToMoviename(String filename, boolean isFile) {
		if (isFile)
			filename = filename.substring(0, filename.lastIndexOf('.'));
		filename = IGNORE.matcher(filename).replaceAll(" ");
		filename = NONALPHA.matcher(filename).replaceAll(" ");
		return filename;
	}
}
