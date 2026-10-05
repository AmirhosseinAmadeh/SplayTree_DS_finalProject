package splayindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.Random;
import java.util.TreeSet;

/** Dependency-free test runner: {@code java -cp out splayindex.SplayTreeTests} exits non-zero on failure. */
public final class SplayTreeTests {
    private static int checks = 0;
    private static int failures = 0;

    static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            failures++;
            System.err.println("FAIL: " + what);
        }
    }

    // ---------------------------------------------------------------- unit tests

    static void testBasics() {
        SplayTree t = new SplayTree();
        check(t.isEmpty() && t.size() == 0 && t.sum() == 0 && t.height() == 0, "empty tree");
        check(!t.contains(5) && !t.remove(5), "nothing in an empty tree");
        check(t.first().isEmpty() && t.last().isEmpty(), "no min/max when empty");
        check(t.add(5) && t.add(3) && t.add(8), "adds succeed");
        check(!t.add(5), "duplicate add is rejected");
        check(t.size() == 3 && t.sum() == 16, "size and sum");
        check(t.contains(3) && !t.contains(4), "contains");
        check(t.remove(3) && !t.remove(3), "remove once");
        check(t.size() == 2 && t.sum() == 13, "size and sum after remove");
        t.validate();
        t.clear();
        check(t.isEmpty(), "clear");
    }

    static void testOrderStatistics() {
        SplayTree t = new SplayTree();
        long[] keys = {50, 10, 30, 70, 20, 60, 40};
        for (long k : keys) t.add(k);
        check(t.select(0) == 10 && t.select(3) == 40 && t.select(6) == 70, "select");
        check(t.rank(10) == 0 && t.rank(35) == 3 && t.rank(71) == 7 && t.rank(-100) == 0, "rank");
        check(t.first().getAsLong() == 10 && t.last().getAsLong() == 70, "first/last");
        check(t.floor(35).getAsLong() == 30 && t.floor(30).getAsLong() == 30 && t.floor(9).isEmpty(), "floor");
        check(t.ceiling(35).getAsLong() == 40 && t.ceiling(40).getAsLong() == 40 && t.ceiling(71).isEmpty(), "ceiling");
        check(t.lower(30).getAsLong() == 20 && t.lower(10).isEmpty(), "lower");
        check(t.higher(30).getAsLong() == 40 && t.higher(70).isEmpty(), "higher");
        boolean threw = false;
        try { t.select(7); } catch (IndexOutOfBoundsException e) { threw = true; }
        check(threw, "select out of range throws");
        t.validate();
    }

    static void testRanges() {
        SplayTree t = new SplayTree();
        for (long k = 1; k <= 10; k++) t.add(k * 10);   // 10..100
        check(t.sumRange(20, 50) == 20 + 30 + 40 + 50, "inclusive range sum");
        check(t.countRange(20, 50) == 4, "inclusive range count");
        check(t.sumRange(25, 45) == 30 + 40, "bounds between keys");
        check(t.sumRange(60, 20) == 0 && t.countRange(60, 20) == 0, "inverted range is empty");
        check(t.sumRange(Long.MIN_VALUE, Long.MAX_VALUE) == 550, "full range with extreme bounds");
        check(t.countRange(Long.MIN_VALUE, Long.MAX_VALUE) == 10, "full count with extreme bounds");
        check(t.sumRange(101, 200) == 0 && t.sumRange(-5, 9) == 0, "ranges outside the data");
        t.add(Long.MAX_VALUE);
        t.add(Long.MIN_VALUE);
        check(t.countRange(Long.MAX_VALUE, Long.MAX_VALUE) == 1, "key at Long.MAX_VALUE is reachable");
        check(t.countRange(Long.MIN_VALUE, Long.MIN_VALUE) == 1, "key at Long.MIN_VALUE is reachable");
        t.validate();
    }

    static void testIterator() {
        SplayTree t = new SplayTree();
        Random r = new Random(7);
        TreeSet<Long> ref = new TreeSet<>();
        for (int i = 0; i < 500; i++) {
            long k = r.nextInt(1000) - 500;
            t.add(k);
            ref.add(k);
        }
        int i = 0;
        long[] expect = ref.stream().mapToLong(Long::longValue).toArray();
        boolean same = t.size() == expect.length;
        for (long k : t) same &= i < expect.length && expect[i++] == k;
        check(same && i == expect.length, "iterator yields keys in ascending order");
        boolean threw = false;
        var it = new SplayTree().iterator();
        try { it.next(); } catch (NoSuchElementException e) { threw = true; }
        check(threw, "iterator on empty tree throws");
        check(java.util.Arrays.equals(t.toArray(), expect), "toArray");
    }

    static void testDegenerateShapes() {
        // Sequential inserts produce a path-shaped tree; nothing may recurse or overflow the stack.
        SplayTree t = new SplayTree();
        final int n = 300_000;
        for (int i = 0; i < n; i++) t.add(i);
        check(t.size() == n, "sequential inserts");
        check(t.height() >= n - 1, "sequential inserts do build a path (height " + t.height() + ")");
        check(t.sumRange(0, n - 1) == (long) n * (n - 1) / 2, "range sum on a path-shaped tree");
        check(t.countRange(1000, 1999) == 1000, "range count on a path-shaped tree");
        t.validate();
        for (int i = 0; i < n; i += 2) t.remove(i);
        check(t.size() == n / 2, "removals on a deep tree");
        t.validate();
    }

    // ---------------------------------------------------------------- randomized differential test

    /** Applies random operations to the tree and a {@link TreeSet} and compares every observable result. */
    static void differential(long seed, int ops, int keyRange, boolean extremes) {
        Random r = new Random(seed);
        SplayTree t = new SplayTree();
        TreeSet<Long> ref = new TreeSet<>();
        boolean ok = true;
        String firstBad = null;
        for (int i = 0; i < ops && ok; i++) {
            long k = key(r, keyRange, extremes);
            long k2 = key(r, keyRange, extremes);
            long lo = Math.min(k, k2), hi = Math.max(k, k2);
            String what = null;
            switch (r.nextInt(12)) {
                case 0, 1 -> { if (t.add(k) != ref.add(k)) what = "add " + k; }
                case 2 -> { if (t.remove(k) != ref.remove(k)) what = "remove " + k; }
                case 3 -> { if (t.contains(k) != ref.contains(k)) what = "contains " + k; }
                case 4 -> { if (t.sumRange(lo, hi) != refSum(ref, lo, hi)) what = "sumRange " + lo + " " + hi; }
                case 5 -> { if (t.countRange(lo, hi) != ref.subSet(lo, true, hi, true).size()) what = "countRange " + lo + " " + hi; }
                case 6 -> { if (t.rank(k) != ref.headSet(k, false).size()) what = "rank " + k; }
                case 7 -> {
                    if (!ref.isEmpty()) {
                        int idx = r.nextInt(ref.size());
                        if (t.select(idx) != nth(ref, idx)) what = "select " + idx;
                    }
                }
                case 8 -> { if (!same(t.floor(k), ref.floor(k)) || !same(t.ceiling(k), ref.ceiling(k))) what = "floor/ceiling " + k; }
                case 9 -> { if (!same(t.lower(k), ref.lower(k)) || !same(t.higher(k), ref.higher(k))) what = "lower/higher " + k; }
                case 10 -> {
                    OptionalLong f = ref.isEmpty() ? OptionalLong.empty() : OptionalLong.of(ref.first());
                    OptionalLong l = ref.isEmpty() ? OptionalLong.empty() : OptionalLong.of(ref.last());
                    if (!f.equals(t.first()) || !l.equals(t.last())) what = "first/last";
                }
                default -> { if (t.size() != ref.size() || t.sum() != refSum(ref, Long.MIN_VALUE, Long.MAX_VALUE)) what = "size/sum"; }
            }
            if (what != null) {
                ok = false;
                firstBad = "op #" + i + " " + what;
            }
            if (i % 997 == 0) {
                try {
                    t.validate();
                } catch (IllegalStateException e) {
                    ok = false;
                    firstBad = "op #" + i + " invariant: " + e.getMessage();
                }
            }
        }
        try {
            t.validate();
        } catch (IllegalStateException e) {
            ok = false;
            firstBad = "final invariant: " + e.getMessage();
        }
        check(ok, "differential seed=" + seed + " range=" + keyRange + " -> " + firstBad);
    }

    static long key(Random r, int range, boolean extremes) {
        if (extremes) {
            switch (r.nextInt(20)) {
                case 0: return Long.MAX_VALUE;
                case 1: return Long.MIN_VALUE;
                case 2: return Long.MAX_VALUE - r.nextInt(3);
                case 3: return Long.MIN_VALUE + r.nextInt(3);
                default:
            }
        }
        return r.nextInt(range * 2) - range;
    }

    static long refSum(NavigableSet<Long> s, long lo, long hi) {
        long sum = 0;
        for (long k : s.subSet(lo, true, hi, true)) sum += k;
        return sum;
    }

    static long nth(TreeSet<Long> s, int idx) {
        int i = 0;
        for (long k : s) if (i++ == idx) return k;
        throw new IllegalStateException();
    }

    static boolean same(OptionalLong a, Long b) {
        return b == null ? a.isEmpty() : a.isPresent() && a.getAsLong() == b;
    }

    // ---------------------------------------------------------------- query engine

    static String run(String script) throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        new QueryEngine(new SplayTree(), pw).run(new BufferedReader(new StringReader(script)));
        return sw.toString().replace("\r\n", "\n");
    }

    static void testQueryEngine() throws IOException {
        check(run("add 5\nadd 7\nfind 5\nfind 6\nsum 1 10\ncount 6 7\nrank 7\nselect 1\nselect 9\nmin\nmax\nfloor 6\nceil 6\nsize\n")
                .equals("true\nfalse\n12\n1\n1\n7\nnone\n5\n7\n5\n7\n2\n"), "all commands");
        check(run("3\nadd 1\nadd 2\nsum 1 2\n").equals("3\n"), "optional count header is skipped");
        check(run("").equals(""), "empty script");
        String bad = run("frobnicate 1\nadd x\nsum 1\nfind 3\n");
        check(bad.equals("error: unknown command 'frobnicate'\nerror: expected an integer in 'add x'\n"
                + "error: 'sum' takes 2 argument(s)\nfalse\n"), "bad input reports an error and carries on: " + bad);
        check(run("add 4\nsum 9 1\n").equals("0\n"), "inverted range is 0");
    }

    /** The assignment's original 100,000-command test: output must match line for line. */
    static void testGolden() throws IOException {
        Path dir = Path.of("src/test/resources/golden");
        if (!Files.exists(dir.resolve("input25.txt"))) {
            System.err.println("skipping golden test (run from the repository root)");
            return;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try (BufferedReader in = Files.newBufferedReader(dir.resolve("input25.txt"), StandardCharsets.UTF_8)) {
            new QueryEngine(new SplayTree(), pw).run(in);
        }
        List<String> got = sw.toString().lines().toList();
        List<String> expected = Files.readAllLines(dir.resolve("output25.txt"), StandardCharsets.UTF_8);
        check(got.equals(expected), "golden output matches (" + got.size() + " vs " + expected.size() + " lines)");
    }

    public static void main(String[] args) throws Exception {
        testBasics();
        testOrderStatistics();
        testRanges();
        testIterator();
        testDegenerateShapes();
        for (long seed = 1; seed <= 6; seed++) differential(seed, 40_000, 50, false);       // heavy duplicates and removals
        for (long seed = 11; seed <= 14; seed++) differential(seed, 40_000, 100_000, false); // sparse keys
        for (long seed = 21; seed <= 24; seed++) differential(seed, 40_000, 30, true);       // Long.MIN/MAX edges
        testQueryEngine();
        testGolden();
        System.out.println((checks - failures) + "/" + checks + " checks passed");
        System.exit(failures == 0 ? 0 : 1);
    }
}
