package splayindex.bench;

import java.util.Random;
import java.util.TreeSet;

import splayindex.SplayTree;

/**
 * Compares {@link SplayTree} with {@link java.util.TreeSet} under different access patterns and on
 * range sums. Run with {@code java -cp out splayindex.bench.Benchmark [keys] [queries]}.
 *
 * <p>Absolute numbers depend on the machine; the interesting part is how each structure reacts to
 * skewed access (splay trees adapt, red-black trees do not) and the cost of range aggregates.
 */
public final class Benchmark {
    private Benchmark() {}

    private static volatile long sink;  // keeps the JIT from discarding the work

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 200_000;
        int q = args.length > 1 ? Integer.parseInt(args[1]) : 2_000_000;
        System.out.printf("keys=%d, queries per run=%d, JVM=%s%n%n", n, q, System.getProperty("java.version"));

        long[] keys = new long[n];
        Random r = new Random(42);
        for (int i = 0; i < n; i++) keys[i] = r.nextLong() >> 8;   // spread over a wide range
        long[] sorted = keys.clone();
        java.util.Arrays.sort(sorted);

        SplayTree splay = new SplayTree();
        TreeSet<Long> tree = new TreeSet<>();
        for (long k : keys) {
            splay.add(k);
            tree.add(k);
        }

        System.out.println("## contains() throughput (million lookups / second)");
        System.out.println("| access pattern | SplayTree | TreeSet | tree height after run |");
        System.out.println("|---|---:|---:|---:|");
        String[] names = {"uniform random", "skewed (Zipf, hot keys)", "hot set (20 keys)", "sequential sweep"};
        for (int p = 0; p < names.length; p++) {
            long[] pattern = pattern(p, sorted, q, new Random(7));
            // warm up both implementations on the same pattern, then measure
            time(splay, pattern);
            time(tree, pattern);
            double s = mops(q, time(splay, pattern));
            double t = mops(q, time(tree, pattern));
            System.out.printf("| %s | %.1f | %.1f | %d |%n", names[p], s, t, splay.height());
        }

        System.out.println();
        System.out.println("## Range sum over 5% of the keys (queries / second)");
        System.out.println("| structure | queries/s |");
        System.out.println("|---|---:|");
        int rangeQueries = 2_000;
        long[][] ranges = new long[rangeQueries][2];
        Random rr = new Random(9);
        int span = n / 20;
        for (int i = 0; i < rangeQueries; i++) {
            int lo = rr.nextInt(n - span);
            ranges[i][0] = sorted[lo];
            ranges[i][1] = sorted[lo + span];
        }
        rangeSplay(splay, ranges);
        rangeTree(tree, ranges);
        double sp = rangeSplay(splay, ranges);
        double tr = rangeTree(tree, ranges);
        System.out.printf("| SplayTree.sumRange (augmented, O(log n)) | %,.0f |%n", rangeQueries / sp);
        System.out.printf("| TreeSet.subSet + loop (O(k)) | %,.0f |%n", rangeQueries / tr);
        System.out.printf("%nspeed-up on range sums: %.0fx%n", tr / sp);
    }

    /** Builds a query sequence of {@code q} keys drawn from the existing keys. */
    private static long[] pattern(int kind, long[] sorted, int q, Random r) {
        long[] out = new long[q];
        int n = sorted.length;
        switch (kind) {
            case 0 -> { for (int i = 0; i < q; i++) out[i] = sorted[r.nextInt(n)]; }
            case 1 -> {   // Zipf-like: rank = n^u, so low ranks are hit far more often
                for (int i = 0; i < q; i++) {
                    int rank = (int) Math.min(n - 1, Math.pow(n, r.nextDouble()) - 1);
                    out[i] = sorted[(int) ((rank * 2654435761L) % n)];   // scatter hot ranks across the key space
                }
            }
            case 2 -> {
                long[] hot = new long[20];
                for (int i = 0; i < hot.length; i++) hot[i] = sorted[r.nextInt(n)];
                for (int i = 0; i < q; i++) out[i] = hot[r.nextInt(hot.length)];
            }
            default -> { for (int i = 0; i < q; i++) out[i] = sorted[i % n]; }
        }
        return out;
    }

    private static double time(SplayTree t, long[] pattern) {
        long start = System.nanoTime();
        long hits = 0;
        for (long k : pattern) if (t.contains(k)) hits++;
        sink = hits;
        return (System.nanoTime() - start) / 1e9;
    }

    private static double time(TreeSet<Long> t, long[] pattern) {
        long start = System.nanoTime();
        long hits = 0;
        for (long k : pattern) if (t.contains(k)) hits++;
        sink = hits;
        return (System.nanoTime() - start) / 1e9;
    }

    private static double rangeSplay(SplayTree t, long[][] ranges) {
        long start = System.nanoTime();
        long acc = 0;
        for (long[] r : ranges) acc += t.sumRange(r[0], r[1]);
        sink = acc;
        return (System.nanoTime() - start) / 1e9;
    }

    private static double rangeTree(TreeSet<Long> t, long[][] ranges) {
        long start = System.nanoTime();
        long acc = 0;
        for (long[] r : ranges)
            for (long k : t.subSet(r[0], true, r[1], true)) acc += k;
        sink = acc;
        return (System.nanoTime() - start) / 1e9;
    }

    private static double mops(int q, double seconds) { return q / seconds / 1e6; }
}
