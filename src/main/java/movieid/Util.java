package movieid;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.json.JSONTokener;

public class Util {
	public static Pattern IMDBURL = Pattern.compile("imdb\\.com/title/tt\\S+");
	public static Pattern FILENAME = Pattern.compile("[/\\:*?\"<>|]+");
	public static Path userAgentsFile = Paths.get("user-agents.txt");
	private static List<String> userAgentsCache;
	// ,__,jpg,nfo,srt,sfv,idx,rar,txt,mds,sup,vob,bup,ifo,sub
	public static final List<String> movieExtensions = Arrays
			.asList(".mkv,.avi,.divx,.mpg,.mp4,.wmv".split(","));

	public static Optional<String> regexInFile(Pattern regex, Path file)
			throws IOException {
		return Files.lines(file, Charset.forName("ISO-8859-1"))
				.map(regex::matcher).filter(Matcher::find).map(Matcher::group)
				.findFirst();
	}

	public static Stream<Path> walkMovies(Path root) {
		try {
			return Files
					.walk(root)
					.filter(path -> path.toFile().isFile())
					.filter(path -> movieExtensions.stream().anyMatch(
							glob -> path.toString().toLowerCase()
									.endsWith(glob)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String getFileExtension(Path path) {
		String fname = path.getFileName().toString();
		return fname.substring(fname.lastIndexOf('.') + 1);
	}

	public static String sanitizeFilename(String name) {
		return FILENAME.matcher(name).replaceAll("-");
	}

	/**
	 * uses ffprobe to get movie duration
	 * 
	 * @param path
	 *            movie filename
	 * @return duration in seconds, NaN if could not read
	 */
	public static double getMovieDuration(Path path) {

		try {
			Process p = new ProcessBuilder("ffprobe", "-loglevel", "quiet",
					"-show_format_entry", "duration", "-of", "json", path
							.toAbsolutePath().toString()).start();
			String duration = new JSONObject(
					new JSONTokener(p.getInputStream()))
					.getJSONObject("format").getString("duration");
			return Double.parseDouble(duration);
		} catch (IOException e) {
			System.out.println("Warning: could not read duration of " + path);
			return Double.NaN;
		}

	}

	private static Pattern IGNORE_ANYWHERE = Pattern.compile(
			"1080p|720p|x264|\\bAC3|\\[..(,..)*\\]", Pattern.CASE_INSENSITIVE
					| Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern IGNORE_NOCASE = Pattern
			.compile(
					"\\b(DL|DTS|unrated|recut|6.1|dvdrip|xvid|dubbed|sow|owk|hdrip|bluray|PS|AC3D|dvdrip|ac3hd|wodkae|bublik|german|viahd|ld|noelite|blubyte|der film)\\b",
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
							| Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern IGNORE = Pattern.compile(
			"\\b(iNTERNAL|CIS|FuN|par2|DEFUSED)\\b",

			Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern NONALPHA = Pattern.compile(
			"[^\\p{Alpha}\\p{Digit}]+", Pattern.UNICODE_CHARACTER_CLASS);

	public static String filenameToMoviename(String filename,
			boolean removeFileExt) {
		if (removeFileExt)
			filename = filename.substring(0, filename.lastIndexOf('.'));
		filename = IGNORE.matcher(filename).replaceAll(" ");
		filename = IGNORE_ANYWHERE.matcher(filename).replaceAll(" ");
		filename = IGNORE_NOCASE.matcher(filename).replaceAll(" ");
		filename = NONALPHA.matcher(filename).replaceAll(" ");
		return filename.trim();
	}

	public static List<String> getIdentificationStrings(Path input) {
		List<String> paths = new ArrayList<>();
		do {
			paths.add(Util.filenameToMoviename(input.getFileName().toString(),
					input.toFile().isFile()));
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
			if (Files.exists(userAgentsFile)) {
				try {
					userAgentsCache = Files.readAllLines(userAgentsFile);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else {
				userAgentsCache = Arrays
						.asList("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/600.2.5 (KHTML, like Gecko) Version/8.0.2 Safari/600.2.5");
			}
		}
		return userAgentsCache.get(ThreadLocalRandom.current().nextInt(
				userAgentsCache.size()));

	}
}
