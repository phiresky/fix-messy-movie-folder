package movieid;

import java.io.File;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
			e.printStackTrace();
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
}
