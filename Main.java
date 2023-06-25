package SplayTree_DS_finalProject;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // try {
        // System.setIn(new
        // FileInputStream("C:\\Users\\Legion\\Desktop\\Program\\__JAVA__\\n" + //
        // "ext\\DS\\Finalproj\\SplayTree_DS_finalProject\\Test\\input25.txt"));
        // System.setOut(new PrintStream(new FileOutputStream("your-output.txt")));
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        Scanner sc = new Scanner(System.in);
        int orderNum = sc.nextInt();

        //SplayTree tree1 = new SplayTree();
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
