import sun.reflect.generics.tree.Tree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class SyntaxMain {
    private Scanner reader;
    private PrintWriter fout;
    private String currentToken="";


    public static void main(String[] args){
        try {
            int mod=Integer.parseInt(args[0]);
            new SyntaxMain().runLex(mod);
            new SyntaxMain().run(mod);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void runLex(int code){
        LexMain lexMain = new LexMain();
        lexMain.run(code);
    }

    private void run(int code) {
        doInit(code);
        ArrayList<TreeNode> resultTree = new ArrayList<>();
        while(reader.hasNextLine()){
            getNextToken();
            TreeNode root = startAnalysis(currentToken);
            resultTree.add(root);
        }
        TreePrinter printer = new TreePrinter(fout);
        for(TreeNode node:resultTree){
            printer.printTree(node);
        }
    }

    private void doInit(int code) {
        try {
            File inFile = new File("resource/tokenOut.txt");
            reader = new Scanner(new FileInputStream(inFile),"utf-8");
            File outFile = new File("result/SyntaxOut.txt");
            if(!outFile.exists()){
                outFile.createNewFile();
            }
            fout = new PrintWriter(new FileWriter(outFile,true));
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            fout.println(df.format(new Date())+" WriteLog: test"+code);
            fout.flush();
        }catch (Exception e){
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    private void doErrorLog(String errorMessage){
        fout.println("Grammatical Errors  Message: "+errorMessage);
        fout.flush();
        System.exit(-1);
    }

    private void getNextToken(){
        currentToken=reader.nextLine();
    }

    /**
     * startAnalysis 本函数匹配ThreadSpec，即以下
     * ThreadSpec-->thread identifier [ features featureSpec ] [ flows flowSpec ] [ properties association; ] end identifier ;
     * @param node 当前要识别的字符串
     * @return 返回生成的ThreadSpec树的根结点
     */
    private TreeNode startAnalysis(String node){
        try {
            TreeNode root = new TreeNode();
            if (node.equals("thread")) {//匹配thread
                getNextToken();
                if(currentToken.equals("identifier")){//匹配identifier
                    root.setType(currentToken);
                    getNextToken();
                    //TODO
                    //在这里遇到了问题，由于features,flow,properties均为可选项，导致分支选择过于复杂，一时间不知道
                    //该怎么解决，先留在这里等着和勇哥讨论一下
                    //下面采用三个if判断，利用全局变量currentToken来执行，将所有可能分支涵盖进去
                    if(currentToken.equals("features")) {
                        getNextToken();//已经识别features了，读入下一个单词
                        TreeNode features = featureSpec();//将当前token当做参数传递进去
                        root.addChile(features);//将features树当做root的子树添加
                        getNextToken();//feature识别完了，读入下一个字符串交给下一个判断
                    }
                    if(currentToken.equals("flows")){
                        getNextToken();//已经识别flows了，读入下一个单词
                        TreeNode flows =  flowSpec();//将当前token当做参数传递进去
                        root.addChile(flows);//将flows树当做root的子树添加
                        getNextToken();//flows识别完了，读入下一个字符串交给下一个判断
                    }
                    if(currentToken.equals("properties")){
                        getNextToken();//已经识别properties了，读入下一个单词
                        TreeNode properties = propertiesSpec();//将当前token当做参数传递进去
                        root.addChile(properties);//将properties树当做root的子树添加
                        getNextToken();//properties识别完了，读入下一个字符串交给下一个判断
                    }
                    if(currentToken.equals("end")){//匹配end
                        getNextToken();
                        if(currentToken.equals("identifier")){//匹配identifier
                            getNextToken();
                            if(!currentToken.equals(";")){//匹配';'，匹配成功会直接执行return，进入此分支为错误
                                doErrorLog("ThreadSpec 缺少\';\'");
                            }
                        }else{
                            doErrorLog("end 后面缺少identifier");
                        }
                    }else{
                        doErrorLog("缺少end");
                    }
                }else{
                    doErrorLog("Thread 后缺少identifier");
                }
            } else {
                doErrorLog("不以Thread开头");
            }
            return root;
        }catch (Exception e){
            System.out.println(e.getMessage());
            doErrorLog("异常错误");
        }
        return null;
    }

    /**
     *featureSpec 本函数匹配featureSpec，即以下
     *featureSpec-->portSpec|ParameterSpec| none ;
     * @return 返回生成的featureSpec的根结点
     */
    private TreeNode featureSpec() {
        TreeNode root = new TreeNode();//生成featureSpec树根结点
        //由于portSpec和parameterSpec有相同的部分，因此在featureSpec中识别一部分
        if(currentToken.equals("identifier")){
            root.setType(currentToken);
            getNextToken();
            if(currentToken.equals(":")) {//identifier 后面应该有":"
                getNextToken();
                TreeNode IO = new TreeNode();//生成IOType结点
                //剩下的又有点麻烦了，IOType有in|out|in out组成，要仔细分类识别
                if (currentToken.equals("out")) {//如果是单 out
                    IO.setType(currentToken);
                    root.addChile(IO);//IOType子树识别完毕，加入node结点
                    getNextToken();//读入下一个字符串
                    return judgePortOrParameter(root);//判断该走哪个分支
                } else if (currentToken.equals("in")) {
                    String type = currentToken;
                    getNextToken();
                    if (currentToken.equals("out")) {//此时IOType是in out
                        IO.setType(type + ' ' + currentToken);//将in out当做一个完整的关键字添加
                        root.addChile(IO);
                        getNextToken();
                        return judgePortOrParameter(root);
                    } else if (currentToken.equals("parameter") || currentToken.equals("data") || currentToken.equals("event")) {//此时IOType是in
                        IO.setType(type);
                        root.addChile(IO);
                        return judgePortOrParameter(root);
                    }
                } else {//不符合IOType要求，报错
                    doErrorLog("不符合IOType要求");
                }
            }else{
                doErrorLog("缺少\':\'");
            }
        }else if(currentToken.equals("none")){
            root.setType(currentToken);
            getNextToken();
            if(currentToken.equals(";")){//匹配分号，结束了
                return root;
            }else{//没有结尾分号，报错
                doErrorLog("featureSpec 后缺少\';\'");
            }
        }else{//第一个结点既不是identifier也不是none，证明不符合featureSpec的表达
            doErrorLog("不符合featureSpec表达规则");
        }
        return root;
    }

    /**
     * judgePortOrParameter 本函数用于区分在识别了IOType之后到底该继续判断PortSpec还是ParameterSpec
     * @param root featureSpec的根结点
     * @return 返回识别完成的featureSpec的根结点
     */
    private TreeNode judgePortOrParameter(TreeNode root){
        if(currentToken.equals("parameter")){//是parameter，接下来应该匹配reference和association了
            getNextToken();
            if(currentToken.equals("identifier")) {//说明有reference
                TreeNode refer = reference();
                root.addChile(refer);//将生成的reference树当做子树添加
                //getNextToken();
            }
            if(currentToken.equals("{")) {//reference后面还有association或者直接是association
                while(currentToken.equals("{")) {//循环识别association
                    getNextToken();//去掉'{'
                    TreeNode associate = association();
                    root.addChile(associate);
                    if(currentToken.equals("}")){
                        getNextToken();
                    }else{
                        doErrorLog("不和规则的association表达");
                    }
                }
                return root;
            }else if(currentToken.equals(";")) {//reference后面没有association了或者reference和association都没有，并且匹配";"
                return root;
            }else{
                doErrorLog("reference后缺少\';\'");
            }
        }else if(currentToken.equals("data") || currentToken.equals("event")){
            TreeNode port = portSpec();
            root.addChile(port);//将生成的portSpec树当做子树添加
            //getNextToken();
            if(currentToken.equals("{")){//portType后面还有association
                while(currentToken.equals("{")) {//循环识别association
                    getNextToken();//去掉'{'
                    TreeNode associate = association();
                    root.addChile(associate);
                    if(currentToken.equals("}")){
                        getNextToken();
                    }else{
                        doErrorLog("不和规则的association表达");
                    }
                }
                return root;
            }else if(currentToken.equals(";")){//portType后面没有association了并且匹配";"
                return root;
            }else{
                doErrorLog("porType后缺少\';\'");
            }
        }else{//不符合PortSpec或者parameterSpec要求，报错
            doErrorLog("不符合PortSpec或者parameterSpec要求");
        }
        return root;
    }

    /**
     * reference 本函数用于匹配reference,即以下
     * reference -->[  { identifier :: } ]  identifier
     * @return 返回生成的reference树的根结点
     */
    private TreeNode reference(){
        String str="";
        TreeNode root = new TreeNode(currentToken);
        TreeNode node = new TreeNode();
        getNextToken();
        if(currentToken.equals("::")){//先识别看看第一个::identifier有没有，有的话变为root的子树
            str=currentToken;
            getNextToken();
            if(currentToken.equals("identifier")){
                node.setType(str+currentToken);
                root.addChile(node);
                getNextToken();
            }else{
                doErrorLog("reference 缺少identifier");
            }
        }
        while(currentToken.equals("::")){//这里需要一个循环来继续识别[::identifier]直到出现错误或者下一个是别的什么东西
            str=currentToken;
            getNextToken();
            if(currentToken.equals("identifier")){
                TreeNode sonNode = new TreeNode(str+currentToken);//将::identifier当做一个结点添加
                root.addChile(sonNode);
                getNextToken();
            }else{
                doErrorLog("reference 缺少identifier");
            }
        }
        return root;
    }

    /**
     * association 此函数用于匹配association，即以下
     * association -->[ identifier :: ] identifier splitter [ constant ] access decimal | none
     * @return 返回生成的association树的根结点
     */
    private TreeNode association(){
        TreeNode root = new TreeNode();
        if(currentToken.equals("none")){//association是none
            root.setType(currentToken);
            getNextToken();
            return root;
        }else if(currentToken.equals("identifier")){
            root.setType(currentToken);
            getNextToken();
            if(currentToken.equals("::")){//结构为[identifier::]identifier
                String str=currentToken;
                getNextToken();
                if(currentToken.equals("identifier")){
                    TreeNode node=new TreeNode(str+currentToken);
                    root.addChile(node);
                    getNextToken();
                }else{
                    doErrorLog("不和规则的association表达");
                }
            }
            if(currentToken.equals("=>") || currentToken.equals("+=>")){//识别splitter
                TreeNode node = new TreeNode(currentToken);
                root.addChile(node);
                getNextToken();
            }
            if(currentToken.equals("constant")){//识别constant
                getNextToken();//不作为结点，直接pass
            }
            if(currentToken.equals("access")){//识别access decimal
                getNextToken();
                if(currentToken.equals("decimal") || currentToken.equals("+decimal") || currentToken.equals("-decimal")){
                    TreeNode node = new TreeNode(currentToken);
                    root.addChile(node);
                    getNextToken();
                    return root;
                }else{
                    doErrorLog("缺少decimal");
                }
            }else{
                doErrorLog("缺少access");
            }
        }else{
            doErrorLog("不和规则的association表达");
        }
        return root;
    }

    /**
     * portSpec 本函数用于匹配portSpec，即以下
     * portSpec --> identifier : IOType portType [ { { association } } ] ;
     * 因为表达式与ParameterSpec有部分重合，因此本函数只识别
     * portType -->data port [ reference ] | event data port [ reference ]| event port 这一部分
     * @return 返回识别完成的部分的根结点
     */
    private TreeNode portSpec(){
        TreeNode root = new TreeNode();
        String str = "";
        if(currentToken.equals("event")) {//此时portType有可能是 event port 也有可能是 event data port
            str = currentToken;
            getNextToken();//看看event后面跟的是什么
            if (currentToken.equals("port")) {//此时portType为 event port，后面不跟reference
                root.setType(str + ' ' + currentToken);
                getNextToken();
                return root;
            } else if (currentToken.equals("data")){//此时portType为 event data port，后面必须跟reference
                str= str+' '+currentToken;
                getNextToken();
                if(currentToken.equals("port")){
                    root.setType(str+' '+currentToken);
                    getNextToken();
                    if(currentToken.equals("identifier")) {//说明后面有reference
                        root.addChile(reference());//root子树为reference构成的树
                        return root;
                    }else{
                        return root;
                    }

                }else{
                    doErrorLog("非法的portType表达");
                }
            }else{
                doErrorLog("非法的portType表达");
            }
        }else if(currentToken.equals("data")){// 此时portType 只有可能是 data port
            str=currentToken;
            getNextToken();
            if(currentToken.equals("port")){
                root.setType(str+' '+currentToken);
                getNextToken();
                if(currentToken.equals("identifier")) {//说明后面有reference
                    root.addChile(reference());
                    return root;
                }else{
                    return root;
                }
            }
        }else{
            doErrorLog("非法的portType表达");
        }
        return root;
    }

    /**
     * flowSpec 本函数用于匹配flowSpec,即以下
     * flowSpec -->flowSourceSpec| flowSinkSpec| flowPathSpec| none;
     * @return 返回生成的flowSpec树的根结点
     */
    private TreeNode flowSpec() {
        TreeNode root = new TreeNode();
        if(currentToken.equals("none")){
            root.setType(currentToken);
            getNextToken();
            if(currentToken.equals(";")) {//匹配分号，结束
                return root;
            }else{
                doErrorLog("flowSpec结尾缺少\';\'");
            }
        }else if(currentToken.equals("identifier")){//这里的情况有点复杂，因为flowSourceSpec| flowSinkSpec| flowPathSpec长得很像，故放在一起识别
            TreeNode node=new TreeNode(currentToken);
            root.addChile(node);//虽然此时还没有确定root的type，但是identifier一定是root的子结点
            getNextToken();
            if(currentToken.equals(":")){//识别':'
                getNextToken();
                if(currentToken.equals("flow")){//识别flow
                    getNextToken();
                    if(currentToken.equals("source") || currentToken.equals("sink")){
                        root.setType(currentToken);//此时确定root的type
                        getNextToken();
                        if(currentToken.equals("identifier")){
                            node = new TreeNode(currentToken);
                            root.addChile(node);
                            getNextToken();
                            if(currentToken.equals("{")){//后面有association
                                while (currentToken.equals("{")){
                                    getNextToken();//去掉'{'
                                    TreeNode associate = association();
                                    root.addChile(associate);
                                    if(currentToken.equals("}")){
                                        getNextToken();
                                    }else{
                                        doErrorLog("不和规则的association表达");
                                    }
                                }
                            }
                            if(currentToken.equals(";")){//到此为止
                                return root;
                            }else{
                                doErrorLog("flowSpec 缺少 \':\'");
                            }
                        }else{
                            doErrorLog("source或者sink后面缺少identifier");
                        }
                    }else if(currentToken.equals("path")){//识别path
                        root.setType(currentToken);//此时确认root的type
                        getNextToken();
                        if(currentToken.equals("identifier")){
                            node = new TreeNode(currentToken);
                            root.addChile(node);
                            getNextToken();
                            if(currentToken.equals("->")){
                                getNextToken();
                                if(currentToken.equals("identifier")){
                                    node = new TreeNode(currentToken);
                                    root.addChile(node);
                                    getNextToken();
                                    if(currentToken.equals(";")) {//识别完了
                                        return root;
                                    }else {
                                        doErrorLog("flowSpec后面缺少\';\'");
                                    }
                                }else{
                                    doErrorLog("path后面缺少identifier");
                                }
                            }else {
                                doErrorLog("path缺少\'->\'");
                            }
                        }else{
                            doErrorLog("path后面缺少identifier");
                        }
                    }
                }else{
                    doErrorLog("flowSpec 缺少\'flow\'");
                }
            }else{
                doErrorLog("flowSpec 缺少 \':\'");
            }
        }else{
            doErrorLog("非法的flowSpec表达");
        }
        return root;
    }

    private TreeNode propertiesSpec() {
        TreeNode root = association();
        if(currentToken.equals(";")) {//识别完了
            return root;
        }else {
            doErrorLog("propertiesSpec 缺少\';\'");
        }
        return root;
    }
}
