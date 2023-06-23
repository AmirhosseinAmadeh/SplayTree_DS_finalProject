package SplayTree_DS_finalProject;

import org.w3c.dom.Node;

public class SplayTree {
    public BTNode root;

    public SplayTree(int data) {
        this.root = new BTNode(data);
    }

    public boolean search(int data) {
        return findNode(data) != null;
    }


    private BTNode findNode(int data) {
        BTNode x = root;
        if (data == x.data)
            return x;

        while (x != null) {
            if (data < x.data)
                x = x.leftChild;
            else if (data > x.data)
                x = x.rightChild;
            else {
                splay(x);
                return x;
            }
        }

        return null;
    }


    public void insert(int data) {
        BTNode newNode = new BTNode(data);
        BTNode y = null;
        BTNode x = root;

        while (x != null) {
            y = x;
            if (data < x.data)
                x = x.leftChild;
            else if (data > x.data)
                x = x.rightChild;
            else
                return; // The node already exists, no duplicates allowed
        }

        newNode.parent = y;

        if (y == null)
            root = newNode;
        else if (data < y.data)
            y.leftChild = newNode;
        else
            y.rightChild = newNode;

        splay(newNode);
    }

    private void splay(BTNode x) {
        while (x.parent != null) {
            BTNode parent = x.parent;
            BTNode grandParent = parent.parent;

            if (grandParent == null) {
                if (x == parent.leftChild)
                    rotateRight(parent);
                else
                    rotateLeft(parent);
            } else {
                if (x == parent.leftChild) {
                    if (parent == grandParent.leftChild) {
                        rotateRight(grandParent);
                        rotateRight(parent);
                    } else {
                        rotateRight(parent);
                        rotateLeft(grandParent);
                    }
                } else {
                    if (parent == grandParent.leftChild) {
                        rotateLeft(parent);
                        rotateRight(grandParent);
                    } else {
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

    public void remove(int data) {
    }

    public void sum(int start, int end) {
    }

}
