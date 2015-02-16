package movieid;

import java.util.List;

import lombok.RequiredArgsConstructor;
import movieid.util.CachedHashMap;
import movieid.util.FFProbeUtil;

@RequiredArgsConstructor
public class MovieRuntimeValidator {
	private CachedHashMap<String, Integer> runtimes = new CachedHashMap<>("runtime-cache");
	private final int warningDurationOffset;

	public void validate(List<MovieInfo> foundMovies) {
		if (!FFProbeUtil.ffprobeAvailable())
			return;
		if (warningDurationOffset == 0)
			return;
		for (MovieInfo info : foundMovies) {
			int runtimeIs = runtimes.getCached(info.getPath().toAbsolutePath().toString(),
					() -> FFProbeUtil.getMovieRuntime(info.getPath()));
			int runtimeWant = info.getRuntime().orElse(runtimeIs);
			int diff = Math.abs(runtimeIs - runtimeWant);
			if (diff >= warningDurationOffset) {
				Main.log(1, String.format(
						"%d min %s than it should be (expected %d min): %s", diff,
						runtimeIs > runtimeWant ? "longer" : "shorter", runtimeWant, info));
			}
		}
	}
}
