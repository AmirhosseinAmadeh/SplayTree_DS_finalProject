package splayindex;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.OptionalLong;

/**
 * An ordered set of {@code long} keys backed by a self-adjusting splay tree whose nodes also store
 * the size and the sum of their subtree.
 *
 * <p>Besides the usual add / remove / contains this makes order statistics and range aggregates
 * cheap: {@link #rank}, {@link #select}, {@link #countRange} and {@link #sumRange} all run in
 * O(log n) amortized time instead of visiting every key in the range.
 *
 * <p>Every operation, including the read-only queries, splays the node it touched to the root.
 * That is what gives splay trees their amortized bounds (a query on a deep node can no longer be
 * repeated cheaply for free) and it makes recently used keys cheap to reach again. A consequence is
 * that the instance is <b>not thread-safe even for reads</b>; synchronize externally.
 *
 * <p>All algorithms are iterative, so a degenerate (path-shaped) tree never overflows the stack.
 * Sums use {@code long} arithmetic and wrap on overflow, like {@code long} addition in Java.
 */
public final class SplayTree implements Iterable<Long> {

    private static final class Node {
        final long key;
        Node left, right, parent;
        int size = 1;   // keys in this subtree
        long sum;       // sum of keys in this subtree

        Node(long key) {
            this.key = key;
            this.sum = key;
        }
    }

    private Node root;

    // ================================================================ basic operations

    public int size() { return root == null ? 0 : root.size; }

    public boolean isEmpty() { return root == null; }

    public void clear() { root = null; }

    /** Sum of all keys. */
    public long sum() { return root == null ? 0 : root.sum; }

    /** Adds {@code key}; returns false (and just splays it) if it was already present. */
    public boolean add(long key) {
        Node parent = null;
        Node cur = root;
        while (cur != null) {
            parent = cur;
            if (key < cur.key) cur = cur.left;
            else if (key > cur.key) cur = cur.right;
            else {
                splay(cur);
                return false;
            }
        }
        Node node = new Node(key);
        node.parent = parent;
        if (parent == null) root = node;
        else if (key < parent.key) parent.left = node;
        else parent.right = node;
        for (Node a = parent; a != null; a = a.parent) pull(a);
        splay(node);
        return true;
    }

    /** Removes {@code key}; returns whether it was present. */
    public boolean remove(long key) {
        Node node = locate(key);
        if (node == null) return false;
        splay(node);
        if (node.key != key) return false;

        Node left = node.left;
        Node right = node.right;
        if (left != null) left.parent = null;
        if (right != null) right.parent = null;
        if (left == null) {
            root = right;
        } else {
            root = left;
            Node max = left;
            while (max.right != null) max = max.right;
            splay(max);              // max is now the root of the left tree and has no right child
            max.right = right;
            if (right != null) right.parent = max;
            pull(max);
        }
        return true;
    }

    /** Membership test. Splays the last node on the search path, hit or miss. */
    public boolean contains(long key) {
        Node node = locate(key);
        if (node == null) return false;
        splay(node);
        return node.key == key;
    }

    // ================================================================ order statistics

    /** Number of keys strictly smaller than {@code key}. */
    public int rank(long key) { return (int) below(key, false, false); }

    /** The {@code index}-th smallest key (0-based). */
    public long select(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("index " + index + ", size " + size());
        Node cur = root;
        while (true) {
            int leftSize = size(cur.left);
            if (index < leftSize) cur = cur.left;
            else if (index == leftSize) {
                splay(cur);
                return cur.key;
            } else {
                index -= leftSize + 1;
                cur = cur.right;
            }
        }
    }

    public OptionalLong first() { return root == null ? OptionalLong.empty() : OptionalLong.of(select(0)); }

    public OptionalLong last() { return root == null ? OptionalLong.empty() : OptionalLong.of(select(size() - 1)); }

    /** Largest key {@code <= key}. */
    public OptionalLong floor(long key) { return neighbour(key, true, true); }

    /** Smallest key {@code >= key}. */
    public OptionalLong ceiling(long key) { return neighbour(key, false, true); }

    /** Largest key {@code < key}. */
    public OptionalLong lower(long key) { return neighbour(key, true, false); }

    /** Smallest key {@code > key}. */
    public OptionalLong higher(long key) { return neighbour(key, false, false); }

    // ================================================================ range aggregates

    /** Number of keys in the inclusive range [{@code lo}, {@code hi}]; 0 when {@code lo > hi}. */
    public int countRange(long lo, long hi) {
        if (lo > hi) return 0;
        return (int) (below(hi, true, false) - below(lo, false, false));
    }

    /** Sum of keys in the inclusive range [{@code lo}, {@code hi}]; 0 when {@code lo > hi}. */
    public long sumRange(long lo, long hi) {
        if (lo > hi) return 0;
        return below(hi, true, true) - below(lo, false, true);
    }

    // ================================================================ iteration and diagnostics

    /** In-order (ascending) iterator. Does not restructure the tree; invalid after a modification. */
    @Override
    public Iterator<Long> iterator() {
        return new Iterator<>() {
            private Node next = minimum(root);

            @Override
            public boolean hasNext() { return next != null; }

            @Override
            public Long next() {
                if (next == null) throw new NoSuchElementException();
                Node result = next;
                if (next.right != null) {
                    next = minimum(next.right);
                } else {
                    Node child = next;
                    Node p = next.parent;
                    while (p != null && p.right == child) {
                        child = p;
                        p = p.parent;
                    }
                    next = p;
                }
                return result.key;
            }
        };
    }

    public long[] toArray() {
        long[] out = new long[size()];
        int i = 0;
        for (long k : this) out[i++] = k;
        return out;
    }

    /** Height of the tree in nodes (0 for an empty tree). */
    public int height() {
        if (root == null) return 0;
        int height = 0;
        java.util.ArrayDeque<Node> level = new java.util.ArrayDeque<>();
        level.add(root);
        while (!level.isEmpty()) {
            height++;
            for (int n = level.size(); n > 0; n--) {
                Node x = level.poll();
                if (x.left != null) level.add(x.left);
                if (x.right != null) level.add(x.right);
            }
        }
        return height;
    }

    /**
     * Checks every structural invariant (BST order, parent links, stored sizes and sums).
     *
     * @throws IllegalStateException describing the first violation found
     */
    public void validate() {
        if (root == null) return;
        if (root.parent != null) throw new IllegalStateException("root has a parent");
        java.util.ArrayDeque<Node> stack = new java.util.ArrayDeque<>();
        Node prev = null;
        Node cur = root;
        // Iterative in-order walk checking order and links.
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) {
                if (cur.left != null && cur.left.parent != cur) throw new IllegalStateException("bad parent link at " + cur.key);
                if (cur.right != null && cur.right.parent != cur) throw new IllegalStateException("bad parent link at " + cur.key);
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop();
            if (prev != null && prev.key >= cur.key) throw new IllegalStateException("keys out of order at " + cur.key);
            int expectedSize = 1 + size(cur.left) + size(cur.right);
            long expectedSum = cur.key + sumOf(cur.left) + sumOf(cur.right);
            if (cur.size != expectedSize) throw new IllegalStateException("bad size at " + cur.key);
            if (cur.sum != expectedSum) throw new IllegalStateException("bad sum at " + cur.key);
            prev = cur;
            cur = cur.right;
        }
    }

    // ================================================================ internals

    private static int size(Node n) { return n == null ? 0 : n.size; }

    private static long sumOf(Node n) { return n == null ? 0 : n.sum; }

    private static Node minimum(Node n) {
        if (n != null) while (n.left != null) n = n.left;
        return n;
    }

    /** Recomputes a node's aggregates from its children. */
    private static void pull(Node n) {
        n.size = 1 + size(n.left) + size(n.right);
        n.sum = n.key + sumOf(n.left) + sumOf(n.right);
    }

    /** Last node on the search path for {@code key} (the match itself if present); null for an empty tree. */
    private Node locate(long key) {
        Node last = null;
        Node cur = root;
        while (cur != null) {
            last = cur;
            if (key < cur.key) cur = cur.left;
            else if (key > cur.key) cur = cur.right;
            else return cur;
        }
        return last;
    }

    /**
     * Aggregate over keys below {@code key} (or up to and including it): their count, or their sum
     * when {@code sum} is true. Splays the last node visited so repeated queries stay cheap.
     */
    private long below(long key, boolean inclusive, boolean sum) {
        long acc = 0;
        Node last = null;
        Node cur = root;
        while (cur != null) {
            last = cur;
            if (key > cur.key || (inclusive && key == cur.key)) {
                acc += sum ? sumOf(cur.left) + cur.key : size(cur.left) + 1;
                cur = cur.right;
            } else {
                cur = cur.left;
            }
        }
        if (last != null) splay(last);
        return acc;
    }

    private OptionalLong neighbour(long key, boolean wantSmaller, boolean inclusive) {
        Node best = null;
        Node last = null;
        Node cur = root;
        while (cur != null) {
            last = cur;
            if (cur.key == key && inclusive) {
                best = cur;
                break;
            }
            if (wantSmaller) {
                if (cur.key < key) { best = cur; cur = cur.right; } else cur = cur.left;
            } else {
                if (cur.key > key) { best = cur; cur = cur.left; } else cur = cur.right;
            }
        }
        if (last != null) splay(last);
        return best == null ? OptionalLong.empty() : OptionalLong.of(best.key);
    }

    /** Rotates {@code x} above its parent, keeping parent links and aggregates correct. */
    private void rotate(Node x) {
        Node p = x.parent;
        Node g = p.parent;
        if (x == p.left) {
            p.left = x.right;
            if (x.right != null) x.right.parent = p;
            x.right = p;
        } else {
            p.right = x.left;
            if (x.left != null) x.left.parent = p;
            x.left = p;
        }
        p.parent = x;
        x.parent = g;
        if (g == null) root = x;
        else if (g.left == p) g.left = x;
        else g.right = x;
        pull(p);
        pull(x);
    }

    /** Moves {@code x} to the root with zig, zig-zig and zig-zag steps. */
    private void splay(Node x) {
        while (x.parent != null) {
            Node p = x.parent;
            Node g = p.parent;
            if (g == null) {
                rotate(x);                                   // zig
            } else if ((g.left == p) == (p.left == x)) {
                rotate(p);                                   // zig-zig
                rotate(x);
            } else {
                rotate(x);                                   // zig-zag
                rotate(x);
            }
        }
        root = x;
    }
}
