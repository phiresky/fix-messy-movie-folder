package movieid.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import movieid.identifiers.FilenameMovieIdentifier;

import org.json.JSONObject;
import org.json.JSONTokener;

public class Util {
	public static Pattern IMDBURL = Pattern.compile("imdb\\.com/title/tt\\S+");
	public static Pattern FILENAME = Pattern.compile("[/\\:*?\"<>|]+");
	private static List<String> userAgentsCache;
	// ,__,jpg,nfo,srt,sfv,idx,rar,txt,mds,sup,vob,bup,ifo,sub
	public static final List<String> movieExtensions = Arrays
			.asList(".mkv,.avi,.divx,.mpg,.mp4,.wmv".split(","));

	public static Optional<String> regexInFile(Pattern regex, Path file) throws IOException {
		return Files.lines(file, Charset.forName("ISO-8859-1")).map(regex::matcher)
				.filter(Matcher::find).map(Matcher::group).findFirst();
	}

	public static Stream<Path> walkMovies(Path root) {
		try {
			return Files
					.walk(root)
					.filter(path -> path.toFile().isFile())
					.filter(path -> movieExtensions.stream().anyMatch(
							glob -> path.toString().toLowerCase().endsWith(glob)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String getFileExtension(Path path) {
		String fname = path.getFileName().toString();
		return fname.substring(fname.lastIndexOf('.') + 1);
	}

	public static String sanitizeFilename(String name) {
		String o = FILENAME.matcher(name).replaceAll("-");
		if (o.endsWith("."))
			return o.substring(0, o.length() - 1);
		return o;
	}

	private static Pattern IGNORE_ANYWHERE = Pattern.compile(
			"1080p|720p|xvid|\\bicq4711|x264|\\bAC3|\\[..(,..)*\\]", Pattern.CASE_INSENSITIVE
					| Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern IGNORE_NOCASE = Pattern
			.compile(
					"\\b(DL|DTS|unrated|recut|repack|6.1|dvdrip|brrip|hdw|cis|5\\.1|yiffy|2brothers|xvid|dubbed|sow|owk|hdrip|bluray|PS|AC3D|dvdrip|ac3hd|wodkae|bublik|german|viahd|ld|noelite|blubyte)\\b",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
							| Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern IGNORE = Pattern.compile(
			"\\b(iNTERNAL|VCF|FuN|par2|TS|DEFUSED|LameHD|PR)\\b",

			Pattern.UNICODE_CHARACTER_CLASS);
	/**
	 * matches name structure like
	 * Der.Hobbit.Eine.Unerwartete.Reise.2012.GERMAN.
	 * DTS.1080p.BluRay.x264-WodkaE, ignore part after that
	 */
	private static Pattern YEAR_PATTERN = Pattern.compile("\\.\\d\\d\\d\\d\\.");
	private static Pattern NONALPHA = Pattern.compile("[^\\p{Alpha}\\p{Digit}]+",
			Pattern.UNICODE_CHARACTER_CLASS);

	public static String filenameToMoviename(String filename, boolean removeFileExt) {
		Matcher m = YEAR_PATTERN.matcher(filename);
		if (m.find()) {
			filename = filename.substring(0, m.end());
		}
		if (removeFileExt)
			filename = filename.substring(0, filename.lastIndexOf('.'));
		filename = IGNORE.matcher(filename).replaceAll(" ");
		filename = IGNORE_ANYWHERE.matcher(filename).replaceAll(" ");
		filename = IGNORE_NOCASE.matcher(filename).replaceAll(" ");
		filename = NONALPHA.matcher(filename).replaceAll(" ");
		return filename.trim();
	}

	/** {@link FilenameMovieIdentifier} */
	public static List<String> getIdentificationStrings(Path input) {
		LinkedList<String> paths = new LinkedList<>();
		do {
			paths.addFirst(Util.filenameToMoviename(input.getFileName().toString(), input.toFile()
					.isFile()));
			input = input.getParent();
		} while (Util.walkMovies(input).count() == 1);

		return paths;
	}

	public static String addUrlParam(String pattern, String... replacements) {
		Object[] rep = new Object[replacements.length];
		try {
			for (int i = 0; i < replacements.length; i++) {
				rep[i] = URLEncoder.encode(replacements[i], "UTF-8");
			}
			return String.format(pattern, rep);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String randomUserAgent() {
		if (userAgentsCache == null) {
			userAgentsCache = new ArrayList<>();
			try (BufferedReader res = new BufferedReader(new InputStreamReader(
					Util.class.getResourceAsStream("user-agents.txt")))) {
				String line;
				while ((line = res.readLine()) != null)
					userAgentsCache.add(line);
			} catch (IOException e1) {
				throw new UncheckedIOException(e1);
			}
		}
		return userAgentsCache.get(ThreadLocalRandom.current().nextInt(userAgentsCache.size()));
	}

	public static Optional<Double> parseDouble(String in) {
		try {
			return Optional.of(Double.parseDouble(in));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public static Optional<Integer> parseInt(String in) {
		try {
			return Optional.of(Integer.parseInt(in));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public static JSONObject getJSON(String url) throws IOException {
		return new JSONObject(new JSONTokener(new URL(url).openStream()));
	}
}
