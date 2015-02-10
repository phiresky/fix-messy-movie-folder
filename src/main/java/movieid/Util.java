package movieid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.json.JSONTokener;

public class Util {
	public static Pattern IMDBURL = Pattern.compile("imdb\\.com/title/tt\\S+");
	public static Pattern FILENAME = Pattern.compile("[/\\:*?\"<>|]+");
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

	public static <T> T serialized(String filename, Supplier<T> defaultVal) {
		T obj = Util.<T> tryReadSerialized(filename).orElseGet(defaultVal);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				Util.writeSerialized(filename, obj);
			}
		});
		return obj;
	}

	public static <T> Optional<T> tryReadSerialized(String filename) {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(
				filename))) {
			@SuppressWarnings("unchecked")
			T c = (T) in.readObject();
			return Optional.of(c);
		} catch (FileNotFoundException e) {
			// ignore
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	public static void writeSerialized(String filename, Object obj) {
		try (ObjectOutputStream out = new ObjectOutputStream(
				new FileOutputStream(filename))) {
			out.writeObject(obj);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
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

	public static String filenameToMoviename(String filename, boolean removeFileExt) {
		if (removeFileExt)
			filename = filename.substring(0, filename.lastIndexOf('.'));
		filename = IGNORE.matcher(filename).replaceAll(" ");
		filename = IGNORE_ANYWHERE.matcher(filename).replaceAll(" ");
		filename = IGNORE_NOCASE.matcher(filename).replaceAll(" ");
		filename = NONALPHA.matcher(filename).replaceAll(" ");
		return filename;
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
}
