package movieid.identifiers;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class ImdbId implements Serializable {
	private static final long serialVersionUID = 1L;

	@Getter private final String id;

	private static Pattern IMDBID_PATTERN = Pattern.compile("(tt\\d+)");
	private static Pattern IMDBURL_PATTERN = Pattern.compile("imdb\\.com/title/" + IMDBID_PATTERN);

	public static ImdbId fromUrl(@NonNull String url) {
		Matcher matcher = IMDBURL_PATTERN.matcher(url);
		matcher.find();
		return new ImdbId(matcher.group(1));
	}

	public static ImdbId fromId(@NonNull String imdbid) {
		if (!IMDBID_PATTERN.matcher(imdbid).matches()) {
			throw new IllegalArgumentException("not an imdb id " + imdbid);
		}
		return new ImdbId(imdbid);
	}
}
