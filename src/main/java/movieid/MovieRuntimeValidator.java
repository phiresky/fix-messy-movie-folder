package movieid;

import java.util.List;

import movieid.util.CachedHashMap;
import movieid.util.Util;

public class MovieRuntimeValidator {
	private CachedHashMap<String, Integer> runtimes = new CachedHashMap<>("runtime-cache");

	public void validate(List<MovieInfo> foundMovies) {
		for (MovieInfo info : foundMovies) {
			int runtimeIs = runtimes.getCached(info.getPath().toAbsolutePath().toString(),
					() -> Util.getMovieRuntime(info.getPath()));
			int runtimeWant = info.getRuntime();
			int diff = Math.abs(runtimeIs - runtimeWant);
			if (diff > 5) {
				System.out.println("Warning: Runtime difference of " + info + " is " + diff
						+ "min (" + runtimeWant + ")");
			}
		}
	}

}
