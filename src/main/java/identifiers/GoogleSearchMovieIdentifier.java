package identifiers;

import java.io.IOException;
import java.util.Optional;

import movieid.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class GoogleSearchMovieIdentifier extends FilenameMovieIdentifier {
	public GoogleSearchMovieIdentifier() {
		super("google", (search) -> {
			String url = Util.addUrlParam("http://google.com/search?q=%s",
					"site:imdb.com " + search);

			System.out.println("getting " + url);
			try {
				Document doc = Jsoup.connect(url)
						.userAgent(Util.randomUserAgent())
						/* .referrer(REFERRER) */.followRedirects(true).get();
				Element ele = doc.select("a[href*=//www.imdb.com/title/tt]")
						.first();
				if (ele == null) {
					return Optional.empty();
				}
				return Optional.of(ImdbId.fromUrl(ele.attr("abs:href")));
			} catch (IOException e) {
				System.out.println("could not get url " + url);
				e.printStackTrace();
			}
			return null;
		});
	}

	private static final String REFERRER = "https://www.google.com/";

}
