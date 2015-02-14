package movieid;

import java.util.List;

import lombok.RequiredArgsConstructor;
import movieid.util.CachedHashMap;
import movieid.util.Util;

@RequiredArgsConstructor
public class MovieRuntimeValidator {
	private CachedHashMap<String, Integer> runtimes = new CachedHashMap<>("runtime-cache");
	private final int warningDurationOffset;

	public void validate(List<MovieInfo> foundMovies) {
		if (warningDurationOffset == 0)
			return;
		for (MovieInfo info : foundMovies) {
			int runtimeIs = runtimes.getCached(info.getPath().toAbsolutePath().toString(),
					() -> Util.getMovieRuntime(info.getPath()));
			int runtimeWant = info.getRuntime();
			int diff = Math.abs(runtimeIs - runtimeWant);
			if (diff >= warningDurationOffset) {
				Main.log(1, String.format(
						"%d min %s than it should be (expected %d min): %s", diff,
						runtimeIs > runtimeWant ? "longer" : "shorter", runtimeWant, info));
			}
		}
	}
}