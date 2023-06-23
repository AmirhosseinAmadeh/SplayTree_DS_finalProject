package SplayTree_DS_finalProject;


public class SplayTree {
    public BTNode root;

    public SplayTree(int data) {
        this.root = new BTNode(data);
    }

    public boolean search(int data) {
        if (root == null)
            throw new IllegalStateException("root cannot be null");
        if (root.data == data)
            ;
        if (root.data > data)
            ;
        if (root.data < data)
            ;
        return false;

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

    private void rotateLeft(BTNode parent) {
    }

    private void rotateRight(BTNode parent) {
    }

    public void remove(int data) {
    }

    public void sum(int start, int end) {
    }

}
