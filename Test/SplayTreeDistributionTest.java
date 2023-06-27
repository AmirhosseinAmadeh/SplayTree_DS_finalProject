package SplayTree_DS_finalProject.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import SplayTree_DS_finalProject.SplayTree;
import SplayTree_DS_finalProject.BTNode;

public class SplayTreeDistributionTest {
    public static void main(String[] args) {
        // Build tree with uniform distribution
        SplayTree uniformTree = buildTreeWithUniformDistribution();
        System.out.println("Uniform Distribution:");
        System.out.println("Tree height: " + calculateTreeHeight(uniformTree));

        // Build tree with normal distribution
        SplayTree normalTree = buildTreeWithNormalDistribution();
        System.out.println("\nNormal Distribution:");
        System.out.println("Tree height: " + calculateTreeHeight(normalTree));
    }

    private static SplayTree buildTreeWithUniformDistribution() {
        SplayTree splayTree = new SplayTree();
        Random random = new Random();
        Set<Integer> randomSet = new HashSet<Integer>();

        // Insert 100 elements with uniform distribution
        for (int i = 0; i < 100; i++) {
            int value = random.nextInt(1000); // Generate random value between 0 and 999
            splayTree.insert(value);
            randomSet.add(value);
            //splayTree.search(randomSet.toArray()[0]);
        }

        return splayTree;
    }

    private static SplayTree buildTreeWithNormalDistribution() {
        SplayTree splayTree = new SplayTree();
        Random random = new Random();

        // Insert 100 elements with normal distribution
        for (int i = 0; i < 100; i++) {
            double value = random.nextGaussian() * 100 + 500; // Generate random value with mean 500 and standard deviation 100
            int intValue = (int) Math.round(value); // Convert to integer
            splayTree.insert(intValue);
        }

        return splayTree;
    }

    private static int calculateTreeHeight(SplayTree splayTree) {
        return calculateHeight(splayTree.getRoot());
    }

    private static int calculateHeight(BTNode node) {
        if (node == null)
            return 0;

        int leftHeight = calculateHeight(node.leftChild);
        int rightHeight = calculateHeight(node.rightChild);

        return 1 + Math.max(leftHeight, rightHeight);
    }
}

