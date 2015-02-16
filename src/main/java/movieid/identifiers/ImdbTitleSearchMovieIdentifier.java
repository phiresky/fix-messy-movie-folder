package movieid.identifiers;

import java.io.IOException;
import java.util.Optional;

import movieid.Main;
import movieid.util.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * search on imdb by title. works less often than google search 
 */
public class ImdbTitleSearchMovieIdentifier extends FilenameMovieIdentifier {

	public ImdbTitleSearchMovieIdentifier() {
		super("imdb", (search) -> {
			String url = Util.addUrlParam("http://www.imdb.com/find?s=tt&ttype=ft&q=%s", search);

			Main.log(2, "getting " + url);
			try {
				Document doc = Jsoup.connect(url).get();
				Element ele = doc.select("table.findList a").first();
				if (ele == null) {
					return Optional.empty();
				}
				return Optional.of(ImdbId.fromUrl(ele.attr("abs:href")));
			} catch (IOException e) {
				Main.log(0, "Could not get url " + url);
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		});
	}

}
