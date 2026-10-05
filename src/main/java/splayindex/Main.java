package splayindex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code java splayindex.Main [commands.txt]} runs a batch of queries from a file, or from standard
 * input when no file is given (type commands interactively, finish with Ctrl-D / Ctrl-Z).
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws IOException {
        if (args.length > 1 || (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help")))) {
            System.err.println("usage: java splayindex.Main [commands.txt]");
            System.exit(args.length > 1 ? 2 : 0);
        }
        BufferedReader in = args.length == 1
                ? Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8)
                : new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)));
        try (in) {
            new QueryEngine(new SplayTree(), out).run(in);
        }
        out.flush();
    }
}
