package movieid;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ImdbUtil {
	private static String NOTFOUND = "[[NOTFOUND]]";
	private static Pattern IMDBURL = Pattern
			.compile("imdb\\.com/title/(tt\\d+)");
	private static Map<String, String> imdbsearchcache;
	private static Map<String, Map<String, String>> imdbidcache;
	static {
		imdbsearchcache = Util.<Map<String, String>> serialized(
				"imdb-search-cache", () -> {
					System.out.println("Creating new search cache");
					return new HashMap<>();
				});
		imdbidcache = Util.<Map<String, Map<String, String>>> serialized(
				"imdb-id-cache", () -> {
					System.out.println("Creating new id cache");
					return new HashMap<>();
				});
		imdbsearchcache.remove("owk bolt");

	}

	public static MovieInfo getMovieInfoFromSearch(String search) {
		search = search.trim();
		System.out.println(search);
		String imdbid = imdbsearchcache.get(search);
		if (imdbid != null) {
			if (imdbid.equals(NOTFOUND)) {
				// System.out.println("IMDB: no results for " +
				// search+" ("+file.getFileName()+")");
				return null;
			}
			return MovieInfo.fromImdb(imdbid);
		}
		String url = null;
		try {
			url = "http://www.imdb.com/find?s=tt&ttype=ft&q="
					+ URLEncoder.encode(search, "UTF-8");
			System.out.println("getting " + url);
			Document doc = Jsoup.connect(url).get();
			Element ele = doc.select("table.findList a").first();
			if (ele == null) {
				// System.out.println("IMDB: no results for " +
				// search+" ("+file.getFileName()+")");
				imdbsearchcache.put(search, "[[NOTFOUND]]");
				return null;
			}
			imdbid = ele.attr("abs:href");
			imdbid = imdbIdFromImdbUrl(imdbid);
			imdbsearchcache.put(search, imdbid);
			return MovieInfo.fromImdb(imdbid);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Could not get url " + url);
			e.printStackTrace();
		}
		throw new RuntimeException();
	}

	public static String imdbIdFromImdbUrl(String url) {
		Matcher matcher = IMDBURL.matcher(url);
		matcher.find();
		return matcher.group(1);
	}

	public static void main(String[] args) {
		System.out
				.println(imdbIdFromImdbUrl("http://www.imdb.com/title/tt0125766/?ref_=fn_tt_tt_1"));
	}

	public static Map<String, String> getMovieInfo(String imdbId) {
		Map<String, String> information = imdbidcache.get(imdbId);
		if (information == null) {
			try {
				System.out.println("loading omdb "+imdbId);
				JSONObject data = new JSONObject(new JSONTokener(new URL(
						"http://www.omdbapi.com/?i=" + imdbId).openStream()));
				information = data.keySet().stream()
						.collect(toMap(key -> key, data::getString));
				imdbidcache.put(imdbId, information);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return information;

	}
}
