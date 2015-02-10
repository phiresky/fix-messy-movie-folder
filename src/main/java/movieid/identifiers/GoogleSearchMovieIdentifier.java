package movieid.identifiers;

import java.io.IOException;
import java.util.Optional;

import movieid.util.Util;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class GoogleSearchMovieIdentifier extends FilenameMovieIdentifier {
	public GoogleSearchMovieIdentifier() {
		super("google", (search) -> {
			String url = Util.addUrlParam("http://google.com/search?q=%s", "site:imdb.com "
					+ search);

			System.out.println("getting " + url);
			try {

				Response resp = Jsoup.connect(url).userAgent(Util.randomUserAgent())
				/* .referrer(REFERRER) */.followRedirects(true).ignoreHttpErrors(true).execute();

				Document doc;
				if (resp.statusCode() == 503 || resp.statusCode() == 502) {
					doc = resp.parse();
					solveCaptcha(doc.select("img").first().absUrl("src"));
				} else if (resp.statusCode() >= 300) {
					throw new RuntimeException(resp.statusCode() + " " + resp.statusMessage());
				} else {
					doc = resp.parse();
				}
				Element ele = doc.select("a[href*=//www.imdb.com/title/tt]").first();
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

	private static void solveCaptcha(String absUrl) {
		throw new RuntimeException("Google wants captcha.");
	}

	private static final String REFERRER = "https://www.google.com/";

}
