package movieid;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import movieid.identifiers.MetadataCsvIdentifier;
import movieid.identifiers.MovieIdentifier;
import movieid.util.FFProbeUtil;
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
				break;
			default:
				throw new IllegalArgumentException("unknown action: " + this);
			}
		}
	}

	private enum DuplicateAction {
		SKIP_ALL, HIGHER_BITRATE, HIGHER_RESOLUTION;
		Comparator<MovieInfo> getComparator() {
			if (this != SKIP_ALL && !FFProbeUtil.ffprobeAvailable()) {
				throw new IllegalArgumentException("Can't compare " + this + " without ffprobe");
			}
			if (this == HIGHER_BITRATE)
				return Comparator.comparing(i -> FFProbeUtil.getBitrate(i.getPath()));
			if (this == HIGHER_RESOLUTION)
				return Comparator.comparing(i -> FFProbeUtil.getResolution(i.getPath()));
			else
				throw new IllegalArgumentException("no comparator: " + this);
		}
	}

	@Parameter(required = true, names = "-in",
			description = "The input directories containing the files (can be multiple)",
			variableArity = true)
	private List<String> inputdirnames;
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
	@Parameter(
			names = "-duplicates",
			description = "What to do when a movie is found multiple times. By default, all copies are skipped as this might indicate split movie files. Allowed: skip_all, higher_bitrate, higher_resolution")
	private DuplicateAction duplicateAction = DuplicateAction.SKIP_ALL;

	private Map<Path, Path> originalRelativePaths = new HashMap<>();

	void run() {
		Stream<Path> inputfiles = inputdirnames
				.stream()
				.map(Paths::get)
				.flatMap(
						rootdir -> Util.walkMovies(rootdir).peek(
								p -> originalRelativePaths.put(p,
										rootdir.getFileName().resolve(rootdir.relativize(p)))));
		Path outputdir = Paths.get(outputdirname);
		List<MovieInfo> allMovies = inputfiles.map(MovieIdentifier::tryAllIdentify)
				.collect(toList());
		List<MovieInfo> unfoundMovies = allMovies.stream().filter(i -> !i.hasMetadata())
				.collect(toList());
		if (unfoundMovies.size() > 0) {
			Main.log(1, "Could not identify:");
			unfoundMovies.forEach(m -> Main.log(1, m));
		}
		List<MovieInfo> foundMovies = allMovies.stream().filter(MovieInfo::hasMetadata)
				.collect(toList());
		removeDuplicates(foundMovies);

		new MovieRuntimeValidator(warningDurationOffset).validate(foundMovies);
		Main.logNoPrefix(1, "Identified " + foundMovies.size() + "/" + allMovies.size() + " movies");
		Function<MovieInfo, String> getFilename = info -> Util.sanitizeFilename(info
				.format(filenamePattern));
		createTargetFiles(foundMovies, outputdir, getFilename);

		writeMetadata(foundMovies, outputdir, getFilename);
	}

	private void removeDuplicates(List<MovieInfo> foundMovies) {
		BiConsumer<MovieInfo, Boolean> log = (info, keep) -> Main.logNoPrefix(
				1,
				String.format("[%s] %.1f MP, %.1f MBit/s: %s", keep ? "keep" : "skip",
						FFProbeUtil.getResolution(info.getPath()),
						FFProbeUtil.getBitrate(info.getPath()),
						info.getPath()));
		foundMovies
				.stream()
				.collect(groupingBy(info -> info.getImdbId()))
				.values()
				.stream()
				.filter(list -> list.size() > 1)
				.forEach(
						list -> {
							Main.log(1, String.format(
									"found %d duplicates for %s:",
									list.size(), list.get(0).format(MovieInfo.DEFAULT_FILENAME)));
							if (duplicateAction != DuplicateAction.SKIP_ALL) {
								list.sort(duplicateAction.getComparator());
								MovieInfo keep = list.remove(list.size() - 1);
								log.accept(keep, true);
							}
							for (MovieInfo info : list) {
								log.accept(info, false);
								foundMovies.remove(info);
							}
						});
	}

	private void writeMetadata(List<MovieInfo> foundMovies, Path outputdir,
			Function<MovieInfo, String> filename) {
		Stream<String> metalines =
				foundMovies.stream().map(
						info -> filename.apply(info) + MetadataCsvIdentifier.METADATA_SEPERATOR
								+ info.getImdbId() + MetadataCsvIdentifier.METADATA_SEPERATOR
								+ info.getPath());
		metalines = Stream
				.concat(Stream
						.of("# this file is used by https://github.com/phiresky/fix-messy-movie-folder to easily identify movies",
								"# filename, IMDb id, original filename"),
						metalines);
		Iterable<String> lineiter = metalines::iterator;
		Path metadatafile = outputdir.resolve("all").resolve(
				MetadataCsvIdentifier.METADATA_FILENAME);
		try {
			Files.write(metadatafile, lineiter, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			try {
				Files.setAttribute(metadatafile, "dos:hidden", true);
			} catch (FileSystemException e) {
				// can't set hidden, ignore
			}
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

	private static interface Discretizer extends Function<MovieInfo, List<String>> {
	}

	private static Discretizer makeInfoDiscretizer(String propName,
			Function<String, String> discretizer) {
		return (MovieInfo info) -> info.getInformationValues(propName).stream()
				.map(discretizer).map(Util::sanitizeFilename).collect(toList());
	}

	// Map from value name to a discretizer that categorizes near values
	static Map<String, Discretizer> properties = new HashMap<>();
	{
		properties.put("country", makeInfoDiscretizer("Country", c -> c));
		properties.put("year", makeInfoDiscretizer("Year", c -> {
			int year = Integer.parseInt(c);
			return (year / 10 * 10) + " - " + ((((year + 10) / 10) * 10) - 1);
		}));
		properties.put("imdb rating", makeInfoDiscretizer("imdbRating", c ->
				Util.parseDouble(c).map(r -> Math.floor(r) + " - " + Math.ceil(r)).orElse(c)
				));
		properties.put("genre", makeInfoDiscretizer("Genre", c -> c));
		properties.put("director", makeInfoDiscretizer("Director", c -> c));
		properties.put("resolution",
				info -> asList(FFProbeUtil.getResolutionString(info.getPath())));

	}

	private void createTargetFiles(List<MovieInfo> foundMovies, Path outputdir,
			Function<MovieInfo, String> filename) {
		foundMovies.forEach(info -> createTargetFiles(info, outputdir,
				Paths.get(filename.apply(info))));
	}

	private void createTargetFiles(MovieInfo info, Path outputdir,
			Path outputFilename) {
		try {
			Path allDir = outputdir.resolve("all");
			Files.createDirectories(allDir);
			Path allFile = allDir.resolve(outputFilename);
			action.doAction(allFile, info.getPath(),
					overwrite, printCreatedFiles);

			Path origAlias = outputdir.resolve("by original structure").resolve(
					originalRelativePaths.get(info.getPath()));
			Files.createDirectories(origAlias.getParent());
			makeSymlink(origAlias, allFile, false, overwrite);

			for (Entry<String, Discretizer> property : properties.entrySet()) {
				String propName = property.getKey();
				Discretizer discretizer = property.getValue();
				for (String val : discretizer.apply(info)) {
					Path dir = outputdir.resolve("by " + Util.sanitizeFilename(propName)).resolve(
							val);
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

	private static void makeSymlink(Path from, Path toAbsolute, boolean printIfNew,
			boolean overwrite)
			throws IOException {
		Path to = toAbsolute;
		try {
			to = from.getParent().relativize(toAbsolute);
		} catch (IllegalArgumentException e) {
			Main.log(1, "Can't make symlink relative: " + e.getMessage());
		}
		if (Files.isSymbolicLink(from)) {
			if (!Files.readSymbolicLink(from).equals(to)) {
				if (overwrite) {
					Files.delete(from);
					makeSymlink(from, toAbsolute, printIfNew, overwrite);
				} else {
					throw new IOException(from + " already exists and points to "
							+ Files.readSymbolicLink(from) + " instead of " + to);
				}
			}
		} else {
			if (printIfNew)
				Main.logNoPrefix(0, "New link: " + from + " -> " + to);
			try {
				Files.createSymbolicLink(from, to);
			} catch (FileSystemException e) {
				Main.log(0, "Could not create symlink: " + e.getMessage());
				// throw (e);
			}
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
