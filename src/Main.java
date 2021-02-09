
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

public class Main {
    private static FileWriter fw;

    private static final int N = 30;// 总节点个数
    private static final int M = 10;// 恶意节点个数
    // private static final int H = 20;//诚实节点个数
    private static final int I = 100;// 交互轮次
    private static final float THRESHOLD = 0.7f;// 可信群体门限
    private static int steps = 0;// 已经执行的时间步

    private static HashMap<Integer, Node> ids = new HashMap<>();// 对应
    private static HashSet<Node> set = new HashSet<>();// 节点列表
    private static HashMap<Node, int[]> fbCnt = new HashMap<>();// 反馈次数记录int[0] = 有效 int[1]=无效

    private static View v;

    public static void main(String[] args) throws Exception {
        // 文件输出流
        fw = new FileWriter("./a.txt");
        v = new View(fw);
        // 1. 创建节点集，设置真实身份
        init(N);
        // 输出所有节点情况
        v.printAllNodeInfo(set);
        // 2. 开启交互
        for (int i = 0; i < I; i++) {
            steps++;
            interact();
            v.allGlobal(set);
            v.printAverageG(set);
            // }
            fw.flush();
        }
        fw.close();
    }

    /**
     * 创建节点集
     * 
     * @param n
     */
    private static void init(int n) {
        for (int i = 1; i <= M; i++) {
            Node node = new Node(i, true);
            set.add(node);
            ids.put(i, node);// 对应关系
        }

        for (int i = M + 1; i <= n; i++) {
            Node node = new Node(i, false);
            set.add(node);
            ids.put(i, node);
        }
    }

    /**
     * 一次交互
     * 
     * @throws IOException
     */
    public static void interact() throws IOException {
        for (Node i : set) {
            // 2. 对每个邻居节点进行一次交互
            for (Node j : set) {
                if (i == j)
                    continue;
                int r = new Random().nextInt()*10;
                if(r>4) continue;//一半概率跳过
                float satisfy;
                float fbDisp;//main计算的主体节点的反馈值离散度，保存在客体节点中

                if (i.getMalicious()) {
                    // 如果i是恶意节点
                    if (j.getMalicious()) {
                        // 如果j也是恶意节点
                        satisfy = 1;
                    } else {
                        satisfy = 0;
                    }
                    fbDisp = calDirectTrustDisp(i, j, satisfy);// 由main计算离散程度并添加进j的feedback里
                    j.setFeedback(i, new float[] { satisfy, fbDisp });
                } else {
                    satisfy = createSatisfy(j);// 根据j的身份获得一个随机生成的满意度
                    float feedback = i.addInteract(j, satisfy);// 更新i对j的历史满意度记录以及反馈情况
                    fbDisp = calDirectTrustDisp(i, j, feedback);
                    j.setFeedback(i, new float[] { feedback, fbDisp });
                }
                // 动态维护机制更新i的全局可信度(奖惩措施)
                if (steps > 20) {
                    updateDMM(i, fbDisp);
                }
                //更新j的全局可信度
                updateGlobal(j);
            }
        }
        // 判断是否进入可信群体
        if(steps>20){
            isMem();
        }
        
        // fw.write("----"+steps+"-----");
        // 输出可信群体内外的成员及其身份（依据全局可信度）
        v.printTeamMember(set);
        // 输出平均全局可信度
        v.printAverageG(set);
        
    }

    private static void updateDMM(Node i, float fbDisp) throws IOException {
        if (fbDisp < 0.5) {
            // fw.write("进入惩罚机制");
            // fw.write(System.getProperty("line.separator"));
            // 离散度过大惩罚
            punish(i, fbDisp);
        } else if (fbDisp > 0.6) {
            // 奖赏
            // fw.write("进入奖赏机制");
            // fw.write(System.getProperty("line.separator"));
            reward(i, fbDisp);
        }
    }

    private static void updateGlobal(Node j){
        Map<Node, float[]> feedback = j.getFeedback();
        if (feedback.size() > 5) {
            float sumJ = 0;
            float dispJ = 0;
            for (Map.Entry<Node, float[]> e : feedback.entrySet()) {
                Node node = e.getKey();
                float[] cur = e.getValue();
                dispJ += cur[1]*node.getGlobal();
                sumJ += cur[0] * cur[1]*node.getGlobal();
            }
            float globalJ = dispJ == 0 ? 0.5f : sumJ / dispJ;
            j.setGlobal(globalJ);
        }
    }
    /**
     * 更新可信群体
     */
    private static void isMem(){
        for (Node i : set) {
            if (i.getGlobal() > THRESHOLD) {
                i.setMember(true);
            } else {
                i.setMember(false);
            }
        }
    }
    /**
     * 根据身份调用随机数生成器
     * 
     * @param j
     * @return
     */
    public static float createSatisfy(Node j) {
        // 判断j的身份
        if (j.getMalicious()) {
            // 是恶意节点
            return generateValue(true);
        } else {
            return generateValue(false);
        }
    }

    /**
     * 根据身份生成本次满意度 暂时无动态节点
     * 
     * @param flag
     * @return
     */
    private static float generateValue(boolean flag) {
        Random r = new Random();
        float res = r.nextFloat() * 0.2f + 0.01f;
        // fw.write("res:"+res);
        if (flag) {
            return res;// 0~0.2
        } else {
            return 1.0f - res;// 0.8~1.0
        }
    }

    /**
     * 惩罚
     * 
     * @param i
     * @param disp
     * @throws IOException
     */
    private static void punish(Node i, float disp) throws IOException {
        if (!fbCnt.containsKey(i)) {
            // 如果不存在记录，则存入此次记录
            fbCnt.put(i, new int[] { 0, 1 });
            return;
        }
        float res = i.getGlobal();
        int[] fc = fbCnt.get(i);
        int success = fc[0];// 反馈有效
        int fail = fc[1] + 1;// 反馈无效
        fc[1] = fc[1] + 1;
        fw.write(success + " " + fail);
        fw.write(System.getProperty("line.separator"));
        float tmp = ((success + 1) / (success + fail + 2)) * disp;
        fw.write("惩罚机制的计算结果：" + tmp);
        fw.write(System.getProperty("line.separator"));
        res = res * (1 - 0.5f * tmp);
        // res = res*0.80f;
        fbCnt.put(i, new int[] { success, fail });// 更新反馈次数统计
        i.setGlobal(res);// 更新全局可信度
    }

    /**
     * 奖赏
     * 
     * @param i
     * @param disp
     * @throws IOException
     */
    private static void reward(Node i, float disp) throws IOException {
        if(!fbCnt.containsKey(i)){
            fbCnt.put(i,new int[]{1,0});
            return;
        }
        int[] fc = fbCnt.get(i);
        int success = fc[0];
        int fail = fc[1];
        fbCnt.put(i,new int[]{success+1,fail});

        float res = i.getGlobal();
        res*=1.10;
        if(res>1){
            res = 1;
        }
        i.setGlobal(res);
        // fw.write(String.valueOf(res));
        // fw.write(System.getProperty("line.separator"));
        int[] fbI = fbCnt.get(i);
        fbI[0]++;
        fbCnt.put(i,fbI);//更新反馈次数统计
    }


    public static float calDirectTrustDisp(Node i, Node j, float feedback) throws IOException {
        //平均值
        float res = 0;
        float sum = 0;
        int cnt = 0;
        //获取j存储的反馈值
        Map<Node, float[]> map = j.getFeedback();
        for(float[] f:map.values()){
            //f[0]是反馈值
            sum+=Math.pow(Math.abs(f[0]-feedback),2);//方差
            cnt++;
        }
        if(cnt==0){
            return 0.5f;
        }
        float tmp = (float) Math.sqrt((1/cnt)*sum);
        fw.write(i.getId()+"对"+j.getId()+"的反馈值离散度方差为："+tmp);
        fw.write(System.getProperty("line.separator"));
        res = 1/(tmp+1);
        
        return res;
    }

}


