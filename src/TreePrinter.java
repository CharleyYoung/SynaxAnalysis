import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

public class TreePrinter {
    private PrintWriter writer;

    public TreePrinter(PrintWriter writer){
        this.writer=writer;
    }

    public void printTree(TreeNode root){
        printNode(root,0);
        writer.flush();
    }

    private void printNode(TreeNode root,int level){
        if(root==null){
            writer.println(levelSpace(level)+"NULL");
        }
        writer.println(levelSpace(level)+root);
        ArrayList<TreeNode> child = root.getChild();
        if(child.size()>0){
            for(int i=0;i<child.size();i++){
                printNode(child.get(i),level+1);
            }
        }
    }

    private String levelSpace(int level){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0;i<level;i++) {
            stringBuilder.append("|    ");
        }
        return stringBuilder.toString();
    }
}
