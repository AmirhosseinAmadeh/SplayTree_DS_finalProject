package SplayTree_DS_finalProject;
public class SplayTree<T> {
    public BTNode<T> root;

    public SplayTree(T data) {
        this.root = new BTNode<T>(data);
    }

}
