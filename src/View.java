import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class View {
    FileWriter fw;

    public View(FileWriter fw) {
        this.fw = fw;
    }

    /**
     * 输出所有节点的信息
     * 
     * @param set
     * @throws IOException
     */
    public void printAllNodeInfo(HashSet<Node> set) throws IOException {
        fw.write("id\t是否为恶意节点\t全局可信度\t是否为可信群体成员");
        fw.write(System.getProperty("line.separator"));
        for(Node i: set){
            printNodeInfo(i);
        }
    }
    
    /**
     * 输出一个节点的所有信息
     * 
     * @param node
     * @throws IOException
     */
    public void printNodeInfo(Node node) throws IOException {
        briefInfo(node);
        //输出邻居节点列表
        //HashSet<Node> neighbor = node.getNeighbor();
        // System.out.print("邻居节点列表：");
        // for(Node n:neighbor){
        //     System.out.print(n.getId()+" ");
        // }

        fw.write("\n");
        fw.write(System.getProperty("line.separator"));
        fw.write("历史交互满意度记录：");
        fw.write(System.getProperty("line.separator"));
        Map<Node, List<float[]>> history = node.getHistory();
        for(Map.Entry<Node, List<float[]>> e:history.entrySet()){
            fw.write(e.getKey().getId()+":");
            for(float[] d:e.getValue()){
                fw.write("["+d[0]+","+d[1]+"] ");
                fw.write(System.getProperty("line.separator"));
            }
            fw.write("\n");
            fw.write(System.getProperty("line.separator"));
        }

        fw.write("\n");
        fw.write(System.getProperty("line.separator"));
        fw.write("各节点对该节点的反馈值记录：");
        fw.write(System.getProperty("line.separator"));
        Map<Node, float[]> feedback = node.getFeedback();
        for(Map.Entry<Node,float[]> e:feedback.entrySet()){
            float[] d = e.getValue();
            fw.write(e.getKey().getId()+"->反馈值:"+d[0]+"，反馈值离散度:"+d[1]+"|");
            fw.write(System.getProperty("line.separator"));
        }
        fw.write("\n");
        fw.write(System.getProperty("line.separator"));

        // Map<Node, float[]> indirect = node.getIndirect();
        // fw.write();
        // for(Map.Entry<Node,float[]> e:indirect.entrySet()){
        //     float[] d = e.getValue();
        //     System.out.print(e.getKey().getId()+"->推荐信任值："+d[0]+",推荐信任值离散度："+d[1]+"|");
        // }
    }
    
    /**
     * 输出可信群体成员列表
     * 
     * @param set
     * @throws IOException
     */
    public void printTeamMember(HashSet<Node> set) throws IOException {
        List<Node> mem = new ArrayList<>();//成员
        List<Node> noMem = new ArrayList<>();//非成员
        
        for(Node n:set){
            if(n.getMember()){
                mem.add(n);
            }else{
                noMem.add(n);
            }
        }
        fw.write("===成员===");
        fw.write(System.getProperty("line.separator"));
        for(Node n:mem){
            briefInfo(n);
        }
        fw.write("===非成员===");
        fw.write(System.getProperty("line.separator"));
        for(Node n:noMem){
            briefInfo(n);
        }
    }

    public void briefInfo(Node node) throws IOException {
        fw.write(node.getId()+"\t"+node.getMalicious()+"\t"+node.getGlobal()+"\t"+node.getMember());
        fw.write(System.getProperty("line.separator"));
    }

    public void printAverageG(HashSet<Node> set) throws IOException {
        float mSum = 0;
        int mCnt = 0;
        float hSum = 0;
        int hCnt = 0;

        for(Node node:set){
            if(node.getMalicious()){
                mSum+=node.getGlobal();
                mCnt++;
            }else{
                hSum+=node.getGlobal();
                hCnt++;
            }
        }

        fw.write("恶意节点平均全局可信度："+(mSum/mCnt));
        fw.write(System.getProperty("line.separator"));
        fw.write("诚实节点平均全局可信度:"+(hSum/hCnt));
        fw.write(System.getProperty("line.separator"));
    }

    public void allGlobal(HashSet<Node> set) throws IOException {
        for(Node node:set){
            fw.write(node.getId()+" "+node.getGlobal());
            fw.write(System.getProperty("line.separator"));
            fw.flush();
        }
    }
}
