package movieid.identifiers;

import java.io.IOException;

import movieid.Main;
import movieid.util.Util;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Searches using normal google search, looking for the first imdb link on the
 * page
 * 
 * this is the identification method that works most often
 */
public class GoogleSearchMovieIdentifier extends FilenameMovieIdentifier {
	public GoogleSearchMovieIdentifier() {
		super("google", (search) -> {
			String url = Util.addUrlParam("http://google.com/search?q=%s", "site:imdb.com "
					+ search);

			Main.log(2, "getting " + url);
			try {

				Response resp = Jsoup.connect(url).userAgent(Util.randomUserAgent())
						/* .referrer(REFERRER) */.followRedirects(true).ignoreHttpErrors(true)
						.execute();

				Document doc;
				if (resp.statusCode() == 503 || resp.statusCode() == 502) {
					doc = resp.parse();
					solveCaptcha(doc.select("img").first().absUrl("src"));
				} else if (resp.statusCode() >= 300) {
					throw new RuntimeException(resp.statusCode() + " " + resp.statusMessage());
				} else {
					doc = resp.parse();
				}
				return doc.select("h3 a[href*=//www.imdb.com/title/tt]").stream()
						.map(ele -> ImdbId.fromUrl(ele.attr("abs:href")))
						.filter(id -> id.getMovieInfo().get("Type").equals("movie")).findFirst();
			} catch (IOException e) {
				Main.log(0, "could not get url " + url);
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
