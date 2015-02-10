package identifiers;

import java.io.IOException;
import java.util.Optional;

import movieid.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ImdbTitleSearchMovieIdentifier extends FilenameMovieIdentifier {

	public ImdbTitleSearchMovieIdentifier() {
		super("imdb", (search) -> {
			String url = Util.addUrlParam("http://www.imdb.com/find?s=tt&ttype=ft&q=%s", search);

			System.out.println("getting " + url);
			try {
				Document doc = Jsoup.connect(url).get();
				Element ele = doc.select("table.findList a").first();
				if (ele == null) {
					return Optional.empty();
				}
				return Optional.of(ImdbId.fromUrl(ele.attr("abs:href")));
			} catch (IOException e) {
				System.out.println("Could not get url " + url);
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		});
	}

}
