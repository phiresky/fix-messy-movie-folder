package movieid;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import movieid.identifiers.MetadataCsvIdentifier;
import movieid.identifiers.MovieIdentifier;
import movieid.util.Util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Main {
	private enum OutputAction {
		SYMLINK, HARDLINK, COPY, MOVE;
		void doAction(Path newLoc, Path oldLoc, boolean replaceExisting, boolean printCreatedFiles)
				throws IOException {
			CopyOption[] attrs = {};
			if (replaceExisting)
				attrs = new CopyOption[] { java.nio.file.StandardCopyOption.REPLACE_EXISTING };
			switch (this) {
			case COPY:
				Files.copy(oldLoc, newLoc, attrs);
				Main.logNoPrefix(0, "New file: " + newLoc);
				break;
			case MOVE:
				Files.move(oldLoc, newLoc, attrs);
				Main.logNoPrefix(0, "New file: " + newLoc);
				break;
			case SYMLINK:
				makeSymlink(newLoc, oldLoc, printCreatedFiles, replaceExisting);
				break;
			case HARDLINK:
				Files.createLink(newLoc, oldLoc);
			default:
				break;

			}
		}
	}

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
	@Parameter(names = { "-h", "--help", "-help" }, help = true)
	public boolean help;
	@Parameter(names = { "-v" }, description = "Verbose output level")
	private static int verbose = 1;
	@Parameter(
			names = "-action",
			description = "What to do with the new files. If this is symlink, the old folder must not be deleted! Allowed: copy, move, symlink, hardlink")
	private static OutputAction action = OutputAction.SYMLINK;
	@Parameter(
			names = "-filename",
			description = "The pattern for the output filename. Possible keys are currently those that http://www.omdbapi.com/ returns and {Extension}")
	private String filenamePattern = MovieInfo.DEFAULT_FILENAME;

	void run() {
		Path inputdir = Paths.get(inputdirname);
		Path outputdir = Paths.get(outputdirname);
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
		removeDuplicates(foundMovies);

		new MovieRuntimeValidator(warningDurationOffset).validate(foundMovies);
		Main.logNoPrefix(1, "Identified " + foundMovies.size() + "/" + allMovies.size() + " movies");
		createTargetFiles(foundMovies, outputdir);

		writeMetadata(foundMovies, outputdir);
	}

	private void removeDuplicates(List<MovieInfo> foundMovies) {
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
	}

	private void writeMetadata(List<MovieInfo> foundMovies, Path outputdir) {
		Stream<String> metalines =
				foundMovies.stream().map(
						info -> info.getPath().getFileName() + "," + info.getImdbId());
		metalines = Stream
				.concat(Stream
						.of("# this file is used by https://github.com/phiresky/fix-messy-movie-folder to easily identify movies"),
						metalines);
		Iterable<String> lineiter = metalines::iterator;
		try {
			Files.write(outputdir.resolve("all").resolve(MetadataCsvIdentifier.METADATA_FILENAME), lineiter);
		} catch (IOException e) {
			Main.log(0, "Could not write metadata.csv");
			e.printStackTrace();
		}
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

	private void createTargetFiles(List<MovieInfo> foundMovies, Path outputdir) {
		foundMovies.forEach(info -> createTargetFiles(info, outputdir,
				Paths.get(Util.sanitizeFilename(info
						.format(filenamePattern)))));
	}

	private void createTargetFiles(MovieInfo info, Path outputdir, Path outputFilename) {

		try {
			Path allDir = outputdir.resolve("all");
			Files.createDirectories(allDir);
			Path allFile = allDir.resolve(outputFilename);
			action.doAction(allFile, info.getPath(),
					overwrite, printCreatedFiles);
			for (String property : properties) {
				for (String val : info.getInformationValues(property)) {
					Path dir = outputdir.resolve("by-" + Util.sanitizeFilename(property)).resolve(
							Util.sanitizeFilename(val));
					Files.createDirectories(dir);
					makeSymlink(dir.resolve(outputFilename), allFile,
							false, overwrite);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void makeSymlink(Path from, Path to, boolean printIfNew, boolean overwrite)
			throws IOException {
		to = from.getParent().relativize(to);
		if (Files.isSymbolicLink(from)) {
			if (!Files.readSymbolicLink(from).equals(to)) {
				if (overwrite) {
					Files.delete(from);
					makeSymlink(from, to, printIfNew, overwrite);
				} else {
					throw new IOException(from + " already exists and points to "
							+ Files.readSymbolicLink(from) + " instead of " + to);
				}
			}
		} else {
			if (printIfNew)
				Main.logNoPrefix(0, "New link: " + from + " -> " + to);
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
