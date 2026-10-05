package splayindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.OptionalLong;

/**
 * Line-oriented query language over a {@link SplayTree}.
 *
 * <pre>
 * add x        insert x (no output)
 * del x        remove x (no output)
 * find x       true / false
 * sum l r      sum of keys in [l, r]
 * count l r    number of keys in [l, r]
 * rank x       number of keys smaller than x
 * select i     i-th smallest key (0-based), or "none" when out of range
 * min | max    smallest / largest key, or "none"
 * floor x      largest key &lt;= x, or "none"
 * ceil x       smallest key &gt;= x, or "none"
 * size         number of keys
 * height       current tree height
 * </pre>
 *
 * The first line of a batch may be a command count (as in the original assignment format); it is
 * recognised by being a bare integer and is otherwise ignored, so interactive use needs no header.
 * Malformed lines produce an {@code error: ...} line and never abort the run.
 */
public final class QueryEngine {
    private final SplayTree tree;
    private final PrintWriter out;

    public QueryEngine(SplayTree tree, PrintWriter out) {
        this.tree = tree;
        this.out = out;
    }

    /** Executes every line of {@code in}. Returns the number of commands run. */
    public int run(BufferedReader in) throws IOException {
        int count = 0;
        String line;
        boolean first = true;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (first) {
                first = false;
                if (line.matches("\\d+")) continue;  // optional command-count header
            }
            execute(line);
            count++;
        }
        out.flush();
        return count;
    }

    /** Executes a single command line. */
    public void execute(String line) {
        String[] t = line.trim().split("\\s+");
        try {
            switch (t[0]) {
                case "add" -> { need(t, 1); tree.add(Long.parseLong(t[1])); }
                case "del" -> { need(t, 1); tree.remove(Long.parseLong(t[1])); }
                case "find" -> { need(t, 1); out.println(tree.contains(Long.parseLong(t[1]))); }
                case "sum" -> { need(t, 2); out.println(tree.sumRange(Long.parseLong(t[1]), Long.parseLong(t[2]))); }
                case "count" -> { need(t, 2); out.println(tree.countRange(Long.parseLong(t[1]), Long.parseLong(t[2]))); }
                case "rank" -> { need(t, 1); out.println(tree.rank(Long.parseLong(t[1]))); }
                case "select" -> {
                    need(t, 1);
                    long i = Long.parseLong(t[1]);
                    out.println(i < 0 || i >= tree.size() ? "none" : String.valueOf(tree.select((int) i)));
                }
                case "min" -> out.println(show(tree.first()));
                case "max" -> out.println(show(tree.last()));
                case "floor" -> { need(t, 1); out.println(show(tree.floor(Long.parseLong(t[1])))); }
                case "ceil" -> { need(t, 1); out.println(show(tree.ceiling(Long.parseLong(t[1])))); }
                case "size" -> out.println(tree.size());
                case "height" -> out.println(tree.height());
                default -> out.println("error: unknown command '" + t[0] + "'");
            }
        } catch (NumberFormatException e) {
            out.println("error: expected an integer in '" + line.trim() + "'");
        } catch (IllegalArgumentException e) {
            out.println("error: " + e.getMessage());
        }
    }

    private static void need(String[] t, int args) {
        if (t.length != args + 1) throw new IllegalArgumentException("'" + t[0] + "' takes " + args + " argument(s)");
    }

    private static String show(OptionalLong v) { return v.isPresent() ? String.valueOf(v.getAsLong()) : "none"; }
}
