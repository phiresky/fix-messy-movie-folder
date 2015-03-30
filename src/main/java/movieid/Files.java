package movieid;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

public class Files {
	public static boolean printCalls = false, simulate = false;

	public static void copy(Path oldLoc, Path newLoc, CopyOption[] attrs) throws IOException {
		if (printCalls) {
			log(oldLoc, newLoc, attrs);
		}
		if (simulate) {
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
		if (printCalls) {
			log(newLoc, oldLoc);
		}
		if (simulate) {
			return;
		}
		java.nio.file.Files.createLink(newLoc, oldLoc);
	}

	public static void move(Path oldLoc, Path newLoc, CopyOption[] attrs) throws IOException {
		if (printCalls) {
			log(oldLoc, newLoc);
		}
		if (simulate) {
			return;
		}
		java.nio.file.Files.move(oldLoc, newLoc, attrs);
	}

	public static void write(Path f, Stream<String> lines,
			StandardOpenOption... opts) throws IOException {
		if (printCalls) {
			List<String> tmp = lines.collect(toList());
			for (String l : tmp) {
				log(f, '"' + l + '"');
			}
			lines = tmp.stream();
		}
		if (simulate) {
			return;
		}
		java.nio.file.Files.write(f, (Iterable<String>) lines::iterator, opts);
	}

	public static void setAttribute(Path f, String s, boolean b) throws IOException {
		if (printCalls) {
			log(f, s, b);
		}
		if (simulate) {
			return;
		}
		java.nio.file.Files.setAttribute(f, s, b);
	}

	public static void createDirectories(Path dir) throws IOException {
		if (printCalls) {
			// log(dir);
		}
		if (simulate) {
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
		if (printCalls) {
			log(from, to);
		}
		if (simulate) {
			return;
		}
		java.nio.file.Files.createSymbolicLink(from, to);
	}

	public static void delete(Path from) throws IOException {
		if (printCalls) {
			log(from);
		}
		if (simulate) {
			return;
		}
		java.nio.file.Files.delete(from);
	}

	public static boolean isRegularFile(Path f) {
		return java.nio.file.Files.isRegularFile(f);
	}

}
