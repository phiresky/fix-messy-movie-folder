package movieid;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import identifiers.MovieIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("invalid usage");
			System.exit(1);
		}
		Path inputdir = Paths.get(args[0]);
		Path outputdir = Paths.get(args[1]);
		List<MovieInfo> allMovies = Util.walkMovies(inputdir)
				.map(MovieIdentifier::tryAllIdentify).collect(toList());
		// System.out.println("Not found:");
		// movies.stream().filter(i -> !i.hasMetadata())
		// .forEach(System.out::println);
		List<MovieInfo> foundMovies = allMovies.stream()
				.filter(MovieInfo::hasMetadata).collect(toList());
		foundMovies
				.stream()
				.collect(groupingBy(info -> info.getImdbId()))
				.values()
				.stream()
				.filter(list -> list.size() > 1)
				.forEach(
						list -> {
							System.out.println(String.format(
									"Warning: found %d duplicates for %s:",
									list.size(),
									list.get(0).format(
											MovieInfo.DEFAULT_FILENAME)));
							for (MovieInfo info : list) {
								System.out.println(info.getPath());
								foundMovies.remove(info);
							}
						});
		System.out.println("Found: " + foundMovies.size() + "/"
				+ allMovies.size() + " movies");
		foundMovies.forEach(info -> createTargetLinks(info, outputdir));
	}

	static List<String> properties = Arrays.asList("Country", "Year",
			"imdbRating");

	private static void createTargetLinks(MovieInfo info, Path outputdir) {
		Path normalizedFilename = Paths.get(Util.sanitizeFilename(info
				.format(MovieInfo.DEFAULT_FILENAME)));
		try {
			Path allDir = outputdir.resolve("all");
			Files.createDirectories(allDir);
			makeSymlink(allDir.resolve(normalizedFilename), info.getPath());
			for (String property : properties) {
				Path dir = outputdir.resolve("by-" + property)
						.resolve(
								info.getInformation().getOrDefault(property,
										"Unknown"));
				Files.createDirectories(dir);
				makeSymlink(dir.resolve(normalizedFilename), info.getPath());
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
