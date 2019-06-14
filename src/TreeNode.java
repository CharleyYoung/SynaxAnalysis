import java.util.ArrayList;

public class TreeNode {
    private String type;
    private ArrayList<TreeNode> child;

    public TreeNode(){
        child = new ArrayList<>();
    }
    public TreeNode(String type){
        this.type = type;
        child = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<TreeNode> getChild() {
        return child;
    }

    public void setChild(ArrayList<TreeNode> child) {
        this.child = child;
    }

    public void addChile(TreeNode node){
        child.add(node);
    }

    public String toString(){
        return type;
    }
}
