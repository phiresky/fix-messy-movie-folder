package identifiers;

import java.nio.file.Path;
import java.util.regex.Pattern;

import movieid.ImdbUtil;
import movieid.MovieInfo;
import movieid.Util;

public class ImdbTitleSearchMovieIdentifier extends MovieIdentifier {
	private static Pattern IGNORE_ANYWHERE = Pattern.compile(
			"1080p|720p|x264|\\bAC3", Pattern.CASE_INSENSITIVE
					| Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern IGNORE_NOCASE = Pattern
			.compile(
					"\\b(DL|DTS|unrated|recut|dvdrip|xvid|dubbed|sow|owk|hdrip|bluray|PS|AC3D|dvdrip|ac3hd|wodkae|bublik|german|viahd|ld|noelite|blubyte|der film)\\b",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
							| Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern IGNORE = Pattern.compile(
			"\\b(iNTERNAL|CIS|FuN|par2|DEFUSED)\\b",

			Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern NONALPHA = Pattern.compile(
			"[^\\p{Alpha}\\p{Digit}]+", Pattern.UNICODE_CHARACTER_CLASS);

	private String log;

	@Override
	public MovieInfo tryIdentifyMovie(Path input) {
		log = "";
		MovieInfo info = tryIdentifyMovie(input, true);
		if (info == null) {
			System.out.println("Could not find " + input.getFileName() + ":\n"
					+ log);
		} else {
			info.setPath(input);
		}
		return info;
	}

	public MovieInfo tryIdentifyMovie(Path input, boolean isFile) {
		String filename = input.getFileName().toString();
		String searchname = filenameToMoviename(filename, isFile);
		log += "searched " + searchname + "\n";
		MovieInfo x = ImdbUtil.getMovieInfoFromSearch(input, searchname);
		if (x == null && Util.walkMovies(input.getParent()).count() == 1)
			return tryIdentifyMovie(input.getParent(), false);
		return x;
	}

	public static String filenameToMoviename(String filename, boolean isFile) {
		if (isFile)
			filename = filename.substring(0, filename.lastIndexOf('.'));
		filename = IGNORE.matcher(filename).replaceAll(" ");
		filename = IGNORE_ANYWHERE.matcher(filename).replaceAll(" ");
		filename = IGNORE_NOCASE.matcher(filename).replaceAll(" ");
		filename = NONALPHA.matcher(filename).replaceAll(" ");
		return filename;
	}
}
