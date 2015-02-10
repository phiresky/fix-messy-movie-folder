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
	@Parameter(required = true, names = "-in", description = "The input directory containing the files") private String inputdirname;
	@Parameter(required = true, names = "-out", description = "The output directory where the generated links are put. Will be created if it does not exist") private String outputdirname;

	void run() {
		Path inputdir = Paths.get(inputdirname).toAbsolutePath();
		Path outputdir = Paths.get(outputdirname).toAbsolutePath();
		List<MovieInfo> allMovies = Util.walkMovies(inputdir).map(MovieIdentifier::tryAllIdentify)
				.collect(toList());
		List<MovieInfo> unfoundMovies = allMovies.stream().filter(i -> !i.hasMetadata())
				.collect(toList());
		if (unfoundMovies.size() > 0) {
			System.out.println("Not found:");
			unfoundMovies.forEach(System.out::println);
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
							System.out.println(String.format(
									"Warning: found %d duplicates for %s:", list.size(), list
											.get(0).format(MovieInfo.DEFAULT_FILENAME)));
							for (MovieInfo info : list) {
								System.out.println(info.getPath());
								foundMovies.remove(info);
							}
						});

		new MovieRuntimeValidator().validate(foundMovies);
		System.out.println("Found: " + foundMovies.size() + "/" + allMovies.size() + " movies");
		foundMovies.forEach(info -> createTargetLinks(info, outputdir));
	}

	public static void main(String[] args) throws IOException {
		Main main = new Main();
		JCommander c = new JCommander(main);
		try {
			c.parse(args);
			main.run();
		} catch (ParameterException e) {
			e.printStackTrace();
			c.usage();
		}

	}

	static List<String> properties = Arrays.asList("Country", "Year", "imdbRating", "Genre",
			"Director");

	private static void createTargetLinks(MovieInfo info, Path outputdir) {
		Path normalizedFilename = Paths.get(Util.sanitizeFilename(info
				.format(MovieInfo.DEFAULT_FILENAME)));
		try {
			Path allDir = outputdir.resolve("all");
			Files.createDirectories(allDir);
			makeSymlink(allDir.resolve(normalizedFilename), allDir.relativize(info.getPath()));
			for (String property : properties) {
				for (String val : info.getInformationValues(property)) {
					Path dir = outputdir.resolve("by-" + Util.sanitizeFilename(property)).resolve(
							Util.sanitizeFilename(val));
					System.out.println(dir);
					Files.createDirectories(dir);
					makeSymlink(dir.resolve(normalizedFilename), dir.relativize(info.getPath()));
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void makeSymlink(Path from, Path to) throws IOException {
		if (Files.isSymbolicLink(from)) {
			if (!Files.readSymbolicLink(from).equals(to)) {
				throw new IOException(from + " already exists and points to "
						+ Files.readSymbolicLink(from) + " instead of " + to);
			}
		} else {
			Files.createSymbolicLink(from, to);
		}
	}
}
