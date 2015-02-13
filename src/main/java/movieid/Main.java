package movieid;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import movieid.identifiers.MovieIdentifier;
import movieid.util.Util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Main {
	@Parameter(required = true, names = "-in",
			description = "The input directory containing the files")
	private String inputdirname;
	@Parameter(
			required = true,
			names = "-out",
			description = "The output directory where the generated links are put. Will be created if it does not exist")
	private String outputdirname;
	@Parameter(names = "-printCreated",
			description = "Print a list of movies that were newly found")
	private boolean printCreatedFiles;
	@Parameter(names = "-overwrite", description = "Overwrite existing files")
	private boolean overwrite;
	@Parameter(
			names = "-durationWarning",
			description = "minimum minutes of offset in expected movie duration from imdb compared to movie file that print a warning. 0 to disable")
	private int warningDurationOffset = 10;
	@Parameter(names = { "-h", "--help" }, help = true)
	public boolean help;
	@Parameter(names = { "-v" }, description = "Verbose output level")
	private static int verbose = 1;

	void run() {
		Path inputdir = Paths.get(inputdirname).toAbsolutePath();
		Path outputdir = Paths.get(outputdirname).toAbsolutePath();
		List<MovieInfo> allMovies = Util.walkMovies(inputdir).map(MovieIdentifier::tryAllIdentify)
				.collect(toList());
		List<MovieInfo> unfoundMovies = allMovies.stream().filter(i -> !i.hasMetadata())
				.collect(toList());
		if (unfoundMovies.size() > 0) {
			Main.log(1, "Not found:");
			unfoundMovies.forEach(m -> Main.log(1, m));
		}
		List<MovieInfo> foundMovies = allMovies.stream().filter(MovieInfo::hasMetadata)
				.collect(toList());
		foundMovies
				.stream()
				.collect(groupingBy(info -> info.getImdbId()))
				.values()
				.stream()
				.filter(list -> list.size() > 1)
				.forEach(
						list -> {
							Main.log(1, String.format(
									"found %d duplicates for %s, ignoring all:",
									list.size(), list.get(0).format(MovieInfo.DEFAULT_FILENAME)));
							for (MovieInfo info : list) {
								Main.logNoPrefix(1, info.getPath());
								foundMovies.remove(info);
							}
						});

		new MovieRuntimeValidator(warningDurationOffset).validate(foundMovies);
		Main.log(1, "Identified " + foundMovies.size() + "/" + allMovies.size() + " movies");
		foundMovies.forEach(info -> createTargetLinks(info, outputdir));
	}

	public static void main(String[] args) throws IOException {
		Main main = new Main();
		JCommander c = new JCommander(main);
		try {
			c.parse(args);
			if (main.help) {
				c.usage();
				return;
			}
			main.run();
		} catch (ParameterException e) {
			e.printStackTrace();
			c.usage();
		}

	}

	static List<String> properties = Arrays.asList("Country", "Year", "imdbRating", "Genre",
			"Director");

	private void createTargetLinks(MovieInfo info, Path outputdir) {
		Path normalizedFilename = Paths.get(Util.sanitizeFilename(info
				.format(MovieInfo.DEFAULT_FILENAME)));
		try {
			Path allDir = outputdir.resolve("all");
			Files.createDirectories(allDir);
			makeSymlink(allDir.resolve(normalizedFilename), allDir.relativize(info.getPath()),
					printCreatedFiles);
			for (String property : properties) {
				for (String val : info.getInformationValues(property)) {
					Path dir = outputdir.resolve("by-" + Util.sanitizeFilename(property)).resolve(
							Util.sanitizeFilename(val));
					Files.createDirectories(dir);
					makeSymlink(dir.resolve(normalizedFilename), dir.relativize(info.getPath()),
							false);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void makeSymlink(Path from, Path to, boolean printIfNew) throws IOException {
		if (Files.isSymbolicLink(from)) {
			if (!Files.readSymbolicLink(from).equals(to)) {
				if (overwrite) {
					Files.delete(from);
					makeSymlink(from, to, printIfNew);
				} else {
					throw new IOException(from + " already exists and points to "
							+ Files.readSymbolicLink(from) + " instead of " + to);
				}
			}
		} else {
			if (printIfNew)
				Main.logNoPrefix(0, "New link: " + from + "->" + to);
			Files.createSymbolicLink(from, to);
		}
	}

	private static String[] levelNames = { "Error", "Warning", "Info", "Debug" };

	public static void log(int level, Object o) {
		if (verbose >= level)
			System.out.println(levelNames[level] + ": " + o);
	}

	public static void logNoPrefix(int level, Object o) {
		if (verbose >= level)
			System.out.println(o);
	}
}
