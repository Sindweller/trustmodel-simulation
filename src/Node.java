import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 边缘设备节点的编号、全局可信度、身份、是否在群体里、历史满意度记录、推荐信任值记录
 */
public class Node {
    int id;
    float global;//全局可信度
    int flag;//1恶意节点 0正常节点 -1动态节点 目前判定的身份
    boolean malicious;//真正的身份
    boolean member = false;//是否在可信群体里
    Map<Node, List<float[]>> history;//历史交互满意度，客体节点->交互满意度记录
    Map<Node, float[]> indirect;//对各节点的推荐信任值记录 float[0]是值 float[1]是离散度

    Map<Node, float[]> feedback = new HashMap<>();//其他节点对本节点反馈的直接信任值，包括值和系统认为的离散度
    //构造方法
    public Node(int id, boolean malicious){
        this.id = id;
        this.malicious = malicious;
        history = new HashMap<>();
        indirect = new HashMap<>();
        this.global = 0.5f;//中立状态
    }

    /**
     * 一次交互过程，计算满意度离散值
     * @param satisfy
     */
    public float addInteract(Node j, float satisfy){
        //无记录
        if(!history.containsKey(j)){
            List<float[]> cur = new ArrayList<>();
            cur.add(new float[]{satisfy,0.5f});
            history.put(j,cur);
            return satisfy;
        }else{
            float ret;
            List<float[]> cur = history.get(j);//满意度 离散度
            //小于2条记录也直接放进去
            if(cur.size()<=2){
                ret = calFeedBack(cur,satisfy);
                cur.add(new float[]{satisfy,0.5f});
            }else{
                //计算离散度
                int n = cur.size();//记录个数
                float sum = 0;
                for(float[] i:cur){
                    //对list中每一项float[0]进行计算
                    sum+=Math.pow(Math.abs(i[0]-satisfy),2);
                    
                }
                float disp;
                if(n!=0){
                    //计算满意度离散度
                    disp = (float) (1 / (Math.sqrt((1 / n) * sum) + 1));
                }else{
                    disp = 0.5f;
                }
                //计算feedback再更新历史记录
                ret = calFeedBack(cur,satisfy);
                cur.add(new float[]{satisfy,disp});
                //更新历史满意度+离散度记录
                history.put(j,cur);
            }
            return ret;

        }
        
    }
    private float calFeedBack(List<float[]> list, float satisfy){
        float sum = 0;
        float dispC = 0;
        float res = satisfy;
        for(float[] f:list){
            sum+=f[0]*f[1];
            dispC+=f[1];
        }
        if(dispC!=0){
            res = (float) (0.5 * satisfy + 0.5 * (sum / dispC));
        }
        return res;
    }

    /**
     * 设置全局可信度（计算由可信中心main完成）
     * @param global
     */
    public void setGlobal(float global){
        this.global = global;
    }

    public boolean getMalicious(){
        return this.malicious;
    }

    public float calDirectTrust(float satisfy, List<float[]> his){
        float sum = 0;
        for(float[] i:his){
            sum*=i[0];
        }
        float res = (float) Math.pow(sum, 1 / his.size());// 乘法平均值
        res = (float) (0.5 * res + 0.5 * satisfy);// 历史和本次各占一半
        return res;
    }

    public void setMember(boolean member){
        this.member = member;
    }

    public float getGlobal(){
        return this.global;
    }

    public int getId(){
        return this.id;
    }

    public boolean getMember(){
        return this.member;
    }

    public Map<Node,float[]> getFeedback(){
        return this.feedback;
    }

    public void setFeedback(Node i, float[] fb){
        this.feedback.put(i,fb);
    }

    public Map<Node, List<float[]>> getHistory(){
        return this.history;
    }

    public Map<Node,float[]> getIndirect(){
        return this.indirect;
    }
}
