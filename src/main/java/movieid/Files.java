package movieid;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class Files {
	public static boolean simulate = false;

	public static void copy(Path oldLoc, Path newLoc, CopyOption[] attrs) throws IOException {
		if (simulate) {
			log(oldLoc, newLoc, attrs);
			return;
		}
		java.nio.file.Files.copy(oldLoc, newLoc, attrs);
	}

	private static void log(Object... args) {
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		String list = Stream.of(args).map(Object::toString).collect(joining(", "));
		Main.logNoPrefix(0, ste.getMethodName() + "\t\t" + list);
	}

	public static void createLink(Path newLoc, Path oldLoc) throws IOException {
		if (simulate) {
			log(newLoc, oldLoc);
			return;
		}
		java.nio.file.Files.createLink(newLoc, oldLoc);
	}

	public static void move(Path oldLoc, Path newLoc, CopyOption[] attrs) throws IOException {
		if (simulate) {
			log(oldLoc, newLoc);
			return;
		}
		java.nio.file.Files.move(oldLoc, newLoc, attrs);
	}

	public static void write(Path f, Iterable<String> lineiter,
			StandardOpenOption... opts) throws IOException {
		if (simulate) {
			for (String l : lineiter) {
				log(f, '"' + l + '"');
			}
			return;
		}
		java.nio.file.Files.write(f, lineiter, opts);
	}

	public static void setAttribute(Path f, String s, boolean b) throws IOException {
		if (simulate) {
			log(f, s, b);
			return;
		}
		java.nio.file.Files.setAttribute(f, s, b);
	}

	public static void createDirectories(Path dir) throws IOException {
		if (simulate) {
			// log(dir);
			return;
		}
		java.nio.file.Files.createDirectories(dir);
	}

	public static boolean isSymbolicLink(Path path) {
		return java.nio.file.Files.isSymbolicLink(path);
	}

	public static Path readSymbolicLink(Path path) throws IOException {
		return java.nio.file.Files.readSymbolicLink(path);
	}

	public static void createSymbolicLink(Path from, Path to) throws IOException {
		if (simulate) {
			log(from, to);
			return;
		}
		java.nio.file.Files.createSymbolicLink(from, to);
	}

	public static void delete(Path from) throws IOException {
		if (simulate) {
			log(from);
			return;
		}
		java.nio.file.Files.delete(from);
	}

	public static boolean isRegularFile(Path f) {
		return java.nio.file.Files.isRegularFile(f);
	}

}
