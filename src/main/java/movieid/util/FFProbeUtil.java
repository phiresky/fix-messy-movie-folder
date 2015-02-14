package movieid.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import movieid.Main;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class FFProbeUtil {
	static Map<Path, JSONObject> jsonData = new HashMap<>();

	public static JSONObject getJson(Path path) {
		if (!jsonData.containsKey(path))
			try {
				Process p = new ProcessBuilder("ffprobe", "-loglevel", "error", "-print_format",
						"json", "-show_format", "-show_streams", path.toAbsolutePath().toString())
						.start();
				jsonData.put(path, new JSONObject(new JSONTokener(p.getInputStream())));
			} catch (IOException e) {
				Main.log(1, "could not read information of " + path);
				return null;
			}
		return jsonData.get(path);
	}

	/** movie runtime in minutes */
	public static int getMovieRuntime(Path path) {
		String duration = getFormat(path).getString("duration");
		return (int) Math.round(Double.parseDouble(duration) / 60);
	}

	private static JSONObject getFormat(Path path) {
		return getJson(path).getJSONObject(
				"format");
	}

	/** gets the movie resolution in pixels */
	public static double getResolution(Path path) {
		JSONObject stream = getVideoStream(path);
		return ((double) stream.getInt("width") * stream.getInt("height")) / 1e6;
	}

	/** Bitrate in MBit/s */
	public static double getBitrate(Path path) {
		JSONObject format = getFormat(path);
		return Double.parseDouble(format.getString("bit_rate")) / 1e6;
	}

	private static JSONObject getVideoStream(Path path) {
		JSONArray streams = getJson(path).getJSONArray("streams");
		JSONObject out = null;
		for (int i = 0; i < streams.length(); i++) {
			if (streams.getJSONObject(i).getString("codec_type").equals("video")) {
				if (out != null)
					Main.log(1, path + " has multiple video streams");
				else
					out = streams.getJSONObject(i);
			}
		}
		return out;
	}

	public static String getResolutionString(Path path) {
		double res = getResolution(path);
		if (res > 1.3)
			return "1080p Full HD";
		if (res > 0.5)
			return "720p Half HD";
		if (res > 0.28)
			return "DVD";
		if (res > 0.2)
			return "kinda shitty";
		if (res > 0.15)
			return "pretty shitty";
		else
			return "pure shit";
	}
}
