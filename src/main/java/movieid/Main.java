package movieid;

import static java.util.stream.Collectors.toList;
import identifiers.MovieIdentifier;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {
	

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("invalid usage");
			System.exit(1);
		}
		Path root = Paths.get(args[0]);
		List<MovieInfo> movies = Util.walkMovies(root)
				.map(MovieIdentifier::tryAllIdentify).collect(toList());
		//System.out.println("Not found:");
		//movies.stream().filter(i -> !i.hasMetadata())
		//		.forEach(System.out::println);
		System.out.println("Found: "
				+ movies.stream().filter(MovieInfo::hasMetadata).count() + "/"
				+ movies.size());

	}
}
