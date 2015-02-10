package identifiers;

import java.io.IOException;
import java.util.Optional;

import movieid.Util;

import org.jsoup.Connection.Response;
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
				Response resp = Jsoup.connect(url)
						.userAgent(Util.randomUserAgent())
						/* .referrer(REFERRER) */.followRedirects(true)
						.execute();
				if (resp.statusCode() == 503 || resp.statusCode() == 502) {
					throw new RuntimeException("Google wants captcha");
				}
				Document doc = resp.parse();
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
