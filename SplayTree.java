package SplayTree_DS_finalProject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class SplayTree {
    private BTNode root = null;
    int size = 0;

    public boolean search(Long data) {
        if (root == null)
            return false;
        BTNode node = findNode(data);
        if (node != null) {
            splay(node);
            return true;
        }
        return false;
    }

    private BTNode findNode(Long data) {
        BTNode x = root;
        if (data == x.data)
            return x;

        while (x != null) {
            if (data < x.data)
                x = x.leftChild;
            else if (data > x.data)
                x = x.rightChild;
            else
                return x;

        }

        return null;
    }

    public void insert(long data) {
        BTNode newNode = new BTNode(data);
        BTNode y = null;
        BTNode x = root;

        while (x != null) {
            y = x;
            if (data < x.data)
                x = x.leftChild;
            else if (data > x.data)
                x = x.rightChild;
            else {
                splay(x);
                return;
            } // The node already exists
        }

        newNode.parent = y;

        if (y == null)
            root = newNode;
        else if (data < y.data)
            y.leftChild = newNode;
        else
            y.rightChild = newNode;

        splay(newNode);
        size++;
    }

    private void splay(BTNode x) {
        while (x.parent != null) {
            BTNode parent = x.parent;
            BTNode grandParent = parent.parent;

            if (grandParent == null) {// zig
                if (x == parent.leftChild)
                    rotateRight(parent);
                else
                    rotateLeft(parent);
            } else {
                if (x == parent.leftChild) {
                    if (parent == grandParent.leftChild) {// zig-zig
                        rotateRight(grandParent);
                        rotateRight(parent);
                    } else {// zig-zag
                        rotateRight(parent);
                        rotateLeft(grandParent);
                    }
                } else {// x == parent.rightChild
                    if (parent == grandParent.leftChild) {// zig-zag
                        rotateLeft(parent);
                        rotateRight(grandParent);
                    } else {// zig-zig
                        rotateLeft(grandParent);
                        rotateLeft(parent);
                    }
                }
            }
        }

        root = x;
    }

    private void rotateLeft(BTNode x) {
        BTNode y = x.rightChild;
        x.rightChild = y.leftChild;

        if (y.leftChild != null)
            y.leftChild.parent = x;

        y.parent = x.parent;

        if (x.parent == null)
            root = y;
        else if (x == x.parent.leftChild)
            x.parent.leftChild = y;
        else
            x.parent.rightChild = y;

        y.leftChild = x;
        x.parent = y;
    }

    private void rotateRight(BTNode x) {
        BTNode y = x.leftChild;
        x.leftChild = y.rightChild;

        if (y.rightChild != null)
            y.rightChild.parent = x;

        y.parent = x.parent;

        if (x.parent == null)
            root = y;
        else if (x == x.parent.leftChild)
            x.parent.leftChild = y;
        else
            x.parent.rightChild = y;

        y.rightChild = x;
        x.parent = y;
    }

    public void remove(long data) {
        BTNode nodeToRemove = findNode(data);
        if (nodeToRemove == null)
            return; // The node doesn't exist in the tree

        else {

            if (nodeToRemove.leftChild == null)
                transPlant(nodeToRemove, nodeToRemove.rightChild);
            else if (nodeToRemove.rightChild == null)
                transPlant(nodeToRemove, nodeToRemove.leftChild);
            else {
                BTNode minRight = minimum(nodeToRemove.rightChild);

                if (minRight.parent != nodeToRemove) {
                    transPlant(minRight, minRight.rightChild);
                    minRight.rightChild = nodeToRemove.rightChild;
                    minRight.rightChild.parent = minRight;
                }

                transPlant(nodeToRemove, minRight);
                minRight.leftChild = nodeToRemove.leftChild;
                minRight.leftChild.parent = minRight;
            }
            if (nodeToRemove.parent != null)
                splay(nodeToRemove.parent);
            size--;
        }
    }

    private BTNode minimum(BTNode x) {
        while (x.leftChild != null)
            x = x.leftChild;

        return x;
    }

    public int getSize() {
        return size;
    }

    private void transPlant(BTNode oldNode, BTNode newNode) {
        if (oldNode.parent == null)
            root = newNode;
        else if (oldNode == oldNode.parent.leftChild)
            oldNode.parent.leftChild = newNode;
        else
            oldNode.parent.rightChild = newNode;

        if (newNode != null)
            newNode.parent = oldNode.parent;
    }

    public long sum(Long l, Long r) {
        return sum(l, r, root);
    }

    public long sum(Long l, Long r, BTNode node) {
        long sum = 0;
        if (node == null)
            return 0;
        if (node.data >= l && node.data <= r) {
            sum += node.data;
            sum += sum(l, r, node.leftChild);
            sum += sum(l, r, node.rightChild);
        } else if (node.data >= r) {
            sum += sum(l, r, node.leftChild);
        } else if (node.data <= l) {
            sum += sum(l, r, node.rightChild);
        }

        return sum;
    }


    public static void main(String[] args) {
        try {
            System.setIn(new FileInputStream("C:\\Users\\Legion\\Desktop\\Program\\__JAVA__\\n" + //
                    "ext\\DS\\Finalproj\\SplayTree_DS_finalProject\\Test\\input25.txt"));
            System.setOut(new PrintStream(new FileOutputStream("your-output.txt")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Scanner sc = new Scanner(System.in);
        int orderNum = sc.nextInt();

        SplayTree tree = new SplayTree();
        long data;

        for (int i = 0; i <= orderNum; i++) {
            String[] order = sc.nextLine().split(" ");
            // System.out.println(order[0]);
            switch (order[0]) {
                case "add":
                    data = Long.parseLong(order[1]);
                    tree.insert(data);

                    break;
                case "del":
                    data = Long.parseLong(order[1]);
                    tree.remove(data);

                    break;

                case "find":
                    data = Long.parseLong(order[1]);
                    System.out.println(tree.search(data));

                    break;

                case "sum":
                    Long l = Long.parseLong(order[1]);
                    Long r = Long.parseLong(order[2]);
                    System.out.println(tree.sum(l, r));
                    break;

                default:
                    break;
            }
        }

        sc.close();
    }

}

class BTNode {
    long data;
    BTNode parent;
    BTNode rightChild;
    BTNode leftChild;

    public BTNode(long data) {
        this.data = data;
    }

}
