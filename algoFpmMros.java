

/**
 * 首先，本模块将TestFile读入，并写到数组中
 * 其次，将其建立成垂直数据库，单先不使用MROS结构
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

import utils.MemoryLogger;


public class algoFpmMros {
    /*
    全局变量
     */
    private static long startTime;//统计时间
    private static long endTime;//统计时间
    static int patternCount =0;//模式计数
    static int add_FreCount = 0;
    static int fully_FreCount=0;
    static int withPurnCount= 0;//计算剪枝后的扩展数量
    static int extension = 0;


    /*垂直数据库（one_verDBList）：存储所有的一模式及其一模式的位置 key：存一模式的itemName value：存一模式的VerDB_Mros*/
    static Map<Integer,VerDB_Mros> one_verDBList = new HashMap<>();
    static Map<Integer,VerDB_Mros> addItemMap = new HashMap<>();
    /* 垂直数据库（k_MDB）：存储所有的频繁k模式及其VerMap  key：存k模式的itemNamevalue：存k模式的Mros*/
    static Map<List<Integer>,Mros> k_MDBmap = new HashMap<>();

    /* 垂直数据库（semi_MDBList）：存储所有可能为频繁k模式及其位置 key：存k模式的itemName value：存k模式的Mros*/
    static Map<List<Integer>,Mros> semi_MDBmap = new HashMap<>();
    /* 可能频繁的模式（maybe_MDBList）：存储所有可能随着数据库的增量，会成为频繁k模式及其Mros结构 key：存k模式的itemName value：存k模式的Mros*/
    static Map<List<Integer>,Mros> maybe_MDBmap = new HashMap<>();


    /*最小支持度*/
    static double minSup =0;//previous Minsupport
    static int Maxsid = 0;//统计序列长度
    /*删减后的Mros结构*/
    static Mros M_ucid_de = new Mros(10,16,0.38,8);

    /*创建写文件参数*/
    BufferedWriter writer = null;//创建bufferedWriter类对象
    static BufferedWriter deWriter = null;

    /*保存最第一次挖掘出的所有频繁1-模式*/
    static Set<Integer> freItemList = new HashSet<>();
    static Set<List<Integer>> addFreList = new HashSet<>();
    /*保存最第一次挖掘出的半频繁1-模式*/
    static Set<Integer> semiItemList = new HashSet<>();

    /*保存最第一次挖掘出的所有不频繁1-模式*/
    static Set<Integer> unFreList =new HashSet<>();
    static double delta = 0;//deDB与原来的CDB的比例
    /*保存最第一次挖掘出的所有频繁2-模式*/
    static List<List<Integer>> two_freItemList = new ArrayList<>();


    /*
     * itemCadMap：候选集存储（需要存储可能频繁的模式）
     * key--可能频繁的1-pattern
     * value--每个可能模式的可能候选者。
     */
    static Map<Integer,List<Integer>> itemCadMap = new HashMap<>();
    static Map<Integer, List<Integer>> itemCad_temp = new HashMap<>();


    //创建一个列表，其中每一条为数组，且数组长度不定，使用范类
    public void ReadFileToVerDB_Mros(String input,String outputFilePath,double minSupRe,double delta) throws IOException {
        //MemoryLogger.getInstance().reset();//清除已记录的内存分配和释放信息，以便重新开始内存分析或确保在某个特定时刻的内存使用情况得到准确的记录
        writer =  new BufferedWriter(new FileWriter(outputFilePath));//创建一个写对象
        //startTime = System.currentTimeMillis();
        MemoryLogger.getInstance().reset();
        int pidsum =0;//求pid的和
        double variance=0;
        /**
         * 执行步骤STEP 1:
         * （1）逐行读入数据
         * （2）生成所有的1-pattern垂直数据库表
         * （3）生成所有的频繁1-pattern垂直数据库表并存入内存
         * 其中，包含，项目名称，项目中的UCID，及其当前位置和上一个位置信息，及其交集（支持度）大小，
         * 使用两种数据结构，将数据分为两部分存储，频繁的存储一部分，不频繁的存储为一部分；
         * 其中，不频繁的只存储其MROS结构，当计算发现其频繁后，再将其加载进入内存，进行挖掘。
         */
        try {
            //读文件
            FileInputStream fin = new FileInputStream(new File(input));
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            String thisLine;

            int sid = 0;//序列号sid初始化
            int pid = 0;//局部位置信息号pid初始化
            double subvar=0;
            //读文件中的每一行，直到结束
            while((thisLine = reader.readLine()) != null){
                //当这行内容为注释，空时，结束
                if(thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@'){
                    continue;
                }
                for(String token : thisLine.split(" ")){
                    if(token.equals("-1")){
                        pid++;
                    } else if (token.equals("-2")){
                        sid++;
                        Maxsid = sid;
                        pidsum+=pid;
                        subvar=(pid-51.997)*(pid-51.997);
                        variance+=subvar;
                        pid = 0;//换行后，需要重置
                        M_ucid_de.add(sid);
                    } else {
                        Integer itemName = Integer.parseInt(token);//使用itemName来接受上述token的值

                        //若不存在
                        if (!one_verDBList.containsKey(itemName)) {
                            /*如果不存在，重新创建*/
                            VerDB_Mros itemInformation = new VerDB_Mros();
                            itemInformation.UCID_Mros.add(sid);//sid存入mros结构

                            /*放入sid,Pid的列表*/
                            List<Integer> posList = new ArrayList<>();
                            posList.add(pid);
                            itemInformation.allInfo.put(sid,posList);

                            /*构建one_verDBList*/
                            one_verDBList.put(itemName,itemInformation);
                        } else {
                            /*若itemName存在于one_verDBList中*/
                            VerDB_Mros verDB_r = one_verDBList.get(itemName);
                            if (verDB_r.allInfo.keySet().contains(sid)){
                                /*若sid存在于one_verDBList中posMap中*/
                                one_verDBList.get(itemName).allInfo.get(sid).add(pid);
                            }else {
                                /*若sid不存在于one_verDBList中posMap中*/
                                one_verDBList.get(itemName).UCID_Mros.add(sid);

                                List<Integer> posList = new ArrayList<>();
                                posList.add(pid);
                                one_verDBList.get(itemName).allInfo.put(sid,posList);
                            }
                        }
                    }
                }
            }
            //关闭写文件
            reader.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("variance: "+variance);

        /*
         * 求最小支持度的阈值
         */
        minSup = minSupRe * Maxsid;
        if (minSup == 0) {
            minSup = 1;
        }

        /**
         * STEP 2: 挑选频繁1-模式
         * freItemList：数据结构--数组，存储内容--所有频繁一模式
         * prefixVerList：数据结构--hash表
         *      key--频繁1-模式
         *      value--频繁一模式的垂直数据结构（Mros和Poslist）
         */

        /*iter是指数据库one_verDBList中的迭代器，是一个地址序列；iter.hashNext是判断边界值
		iter.next()获取本身的值，然后向后移一步；iter.remove()删除本次值，移向下一步
		* */
        Iterator<Entry<Integer, VerDB_Mros>> iter = one_verDBList.entrySet().iterator();
        while (iter.hasNext()){
            //使用一个参数来接受迭代器当前的值
            Map.Entry<Integer, VerDB_Mros> entry = (Map.Entry<Integer,VerDB_Mros>) iter.next();
            if(entry.getValue().getSupport() >= minSup){
                freItemList.add(entry.getKey());
                List<Integer> p = new ArrayList<>();
                p.add(entry.getKey());
                k_MDBmap.put(p,entry.getValue().UCID_Mros);
                patternCount++;
                //unFreVerList.put(entry.getKey(),entry.getValue());
                //iter.remove(); //使用iter迭代函数，将不频繁的1-pattern都移除，此时，one_verDBList中只剩频繁的一模式
            } else if (entry.getValue().getSupport() >= minSup*delta) {
                List<Integer> p = new ArrayList<>();
                p.add(entry.getKey());
                semi_MDBmap.put(p,entry.getValue().UCID_Mros);
                semiItemList.add(entry.getKey());
            } else {
                unFreList.add(entry.getKey());
            }
        }


        /**
         * 生成器候选集，并且生成二模式
         * candList:列表，存储频繁项集的候选者
         * itemCadMap:存储全局变量哈希表，
         *      key--频繁一模式
         *      value--每个的候选者
         */
        Map<List<Integer>,VerDB_Mros> k_verDB_temp = new HashMap<>();
        Map<List<Integer>,VerDB_Mros> semi_verDB_temp = new HashMap<>();
        List<List<Integer>> semi_twolist = new ArrayList<>();
        Set<Integer> union =new HashSet<>();
        union.addAll(freItemList);
        union.addAll(semiItemList);
        for (Integer integer1 : union){
            VerDB_Mros verDBMros_1 =one_verDBList.get(integer1);
            List<Integer> candlist =new ArrayList<>();
            for (Integer integer2 : union){
                List<Integer> itemList = new ArrayList<>();
                itemList.add(integer1);
                itemList.add(integer2);
                VerDB_Mros verDBMros_2 =one_verDBList.get(integer2);
                VerDB_Mros verDBMros_xy = ExtendP(verDBMros_1,verDBMros_2,minSup);
                if (verDBMros_xy.allInfo.size()>=minSup){
                    two_freItemList.add(itemList);
                    k_verDB_temp.put(itemList,verDBMros_xy);
                    k_MDBmap.put(itemList,verDBMros_xy.UCID_Mros);
                    candlist.add(integer2);
                }else if (verDBMros_xy.allInfo.size()>=minSup*delta){
                    semi_twolist.add(itemList);
                    semi_verDB_temp.put(itemList,verDBMros_xy);
                    semi_MDBmap.put(itemList,verDBMros_xy.UCID_Mros);
                    candlist.add(integer2);
                }
                itemCadMap.put(integer1,candlist);
            }
        }


        /**
         * 频繁的二模式模式增长
         */
        for (List<Integer> two_integers : two_freItemList){
            Integer last = two_integers.get(two_integers.size()-1);
            List<Integer> canList_1 = itemCadMap.get(last);
            VerDB_Mros verDBMros_1 = k_verDB_temp.get(two_integers);
            if (canList_1 != null){
                GrowP(two_integers,verDBMros_1,canList_1,minSup,delta);
            }
            //savePattern(prefixVerList,minSup);
        }

        for (List<Integer> semiPtwo : semi_twolist){
            Integer last = semiPtwo.get(semiPtwo.size()-1);
            List<Integer> canList_1 = itemCadMap.get(last);
            VerDB_Mros verDBMros_1 = semi_verDB_temp.get(semiPtwo);
            if (canList_1!=null){
                GrowP(semiPtwo,verDBMros_1,canList_1,minSup,delta);
            }
        }
        //MemoryLogger.getInstance().checkMemory();
        //endTime = System.currentTimeMillis();
        //printStatistics();
        //System.out.println("max past memory:"+MemoryLogger.getInstance().getMaxMemory());
    }





    /**
     * GrowP 用来生成所有的频繁模式
     * @param fre_x
     * @param VerDB_x
     * @param candidateList
     * @param minSup
     * @throws IOException
     */

    public static void GrowP(List<Integer> fre_x,VerDB_Mros VerDB_x,List<Integer> candidateList,double minSup,double delta) throws IOException{
        Mros Mros_x = VerDB_x.UCID_Mros;
        Map<List<Integer>,VerDB_Mros> freVerList_new =new HashMap<>();
        for (Integer y : candidateList) {
            Mros Mros_y = one_verDBList.get(y).UCID_Mros;
            double xANDy = Mros_x.intersectionSizeEstimate(Mros_x,Mros_y);
            if (xANDy >= minSup*delta){
                withPurnCount++;
                List<Integer> item_new = new ArrayList<>();
                item_new.addAll(fre_x);//深拷贝、浅拷贝有区别
                VerDB_Mros verDB_xy = ExtendP(VerDB_x,one_verDBList.get(y),minSup);
                if (verDB_xy.allInfo.size()>=minSup){
                    /*挖掘频繁的k模式*/
                    item_new.add(y);
                    freVerList_new.put(item_new,verDB_xy);
                    k_MDBmap.put(item_new,verDB_xy.UCID_Mros);

                } else if (verDB_xy.allInfo.size()>=minSup*delta) {
                    /*挖掘可能频繁的k模式*/
                    item_new.add(y);
                    freVerList_new.put(item_new,verDB_xy);
                    semi_MDBmap.put(item_new,verDB_xy.UCID_Mros);
                }
                //savePattern(freVerList,minSup);
            }
        }
        for (Entry<List<Integer>,VerDB_Mros> entry : freVerList_new.entrySet()){
            Integer last = entry.getKey().get(entry.getKey().size()-1);
            List<Integer> candList_new = itemCadMap.get(last);
            if (candList_new!=null) {
                GrowP(entry.getKey(), entry.getValue(), candList_new, minSup, delta);
            }
        }
    }


    /**
     * ExtendP:生成扩展后模式的垂直数据库，即生成<x,y>模式的垂直数据库
     * @param VerDB_x 等待扩展x的垂直数据库
     * @param VerDB_y 在最后加入的垂直数据库
     * @param minSup 最小支持度
     * @throws IOException
     */
    private static VerDB_Mros ExtendP(VerDB_Mros VerDB_x,VerDB_Mros VerDB_y,double minSup) throws IOException {

        Map<Integer, List<Integer>> posMap_x = VerDB_x.allInfo;
        Map<Integer, List<Integer>> posMap_y = VerDB_y.allInfo;

        VerDB_Mros verDB_temp = new VerDB_Mros();


        for (Integer ucid : posMap_x.keySet()) {
            if (posMap_y.keySet().contains(ucid)) {
                /*如果ucid存在于y的VerDB中*/
                Integer first_x = posMap_x.get(ucid).get(0);
                Integer first_y = posMap_y.get(ucid).get(0);
                Integer y_size = posMap_y.get(ucid).size();

                Integer last_y = posMap_y.get(ucid).get(y_size - 1);

                /*构造positionList存储局部位置信息*/
                List<Integer> posList_temp = new ArrayList<>();
                if (first_x < first_y) {
                    posList_temp.addAll(posMap_y.get(ucid));//将y的positionList直接赋值给<x,y>模式中ucid的position信息表
                } else if (first_x < last_y) {
                    List<Integer> posList_y = posMap_y.get(ucid);
                    for (Integer pid : posList_y) {
                        if (pid > first_x) {
                            posList_temp.add(pid);
                        }
                    }
                }
                if (!posList_temp.isEmpty()) {
                    verDB_temp.UCID_Mros.add(ucid);//ucid放入Mros数据库中
                    verDB_temp.allInfo.put(ucid, posList_temp);
                }
            }
        }
        return verDB_temp;
    }
    public static void deMFP(String deDB,String deOutPut, double minSupRe,double delta) throws IOException{
        MemoryLogger.getInstance().reset();
        startTime = System.currentTimeMillis();
        Set<Integer> deItems = new HashSet<>();
        int deMaxsid =0;
        deWriter =  new BufferedWriter(new FileWriter(deOutPut));//创建一个写对象

        /**
         * 执行步骤STEP 1:
         * （1）逐行读入数据
         * （2）生成所有的1-pattern垂直数据库表
         * （3）生成所有的频繁1-pattern垂直数据库表并存入内存
         * 其中，包含，项目名称，项目中的UCID，及其当前位置和上一个位置信息，及其交集（支持度）大小，
         * 使用两种数据结构，将数据分为两部分存储，频繁的存储一部分，不频繁的存储为一部分；
         * 其中，不频繁的只存储其MROS结构，当计算发现其频繁后，再将其加载进入内存，进行挖掘。
         */
        try {
            //读文件
            FileInputStream fin = new FileInputStream(new File(deDB));
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            String thisLine;

            int sid = 0;//序列号sid初始化
            int pid = 0;//局部位置信息号pid初始化
            //读文件中的每一行，直到结束
            Map<Integer,Set<Integer>> T_temp = new HashMap<>();
            while((thisLine = reader.readLine()) != null){
                //当这行内容为注释，空时，结束
                if(thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@'){
                    continue;
                }
                for(String token : thisLine.split(" ")){
                    if(token.equals("-1")){
                        pid++;
                    } else if (token.equals("-2")){
                        sid++;
                        deMaxsid = sid;
                        pid = 0;//换行后，需要重置
                        M_ucid_de.remove(sid);
                    } else {
                        Integer itemName = Integer.parseInt(token);//使用itemName来接受上述token的值
                        //第一次在itemname中出现的sid删除掉
                        if (T_temp.containsKey(itemName)){
                            if (T_temp.get(itemName).contains(sid)){
                                continue;
                            }else {
                                if (k_MDBmap.containsKey(itemName)){
                                    k_MDBmap.get(itemName).remove(sid);
                                } else if (semi_MDBmap.containsKey(itemName)) {
                                    semi_MDBmap.get(itemName).remove(sid);
                                }
                            }
                        }else{
                            Set<Integer> sidSet=new HashSet<>();
                            sidSet.add(sid);
                            T_temp.put(itemName,sidSet);
                            if (k_MDBmap.containsKey(itemName)){
                                k_MDBmap.get(itemName).remove(sid);
                            } else if (semi_MDBmap.containsKey(itemName)) {
                                semi_MDBmap.get(itemName).remove(sid);
                            }
                        }
                    }
                }
            }

            //关闭写文件
            reader.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        /*更新减量后的最小支持度*/
        double newminSup = Math.ceil((Maxsid-deMaxsid)*minSupRe);
        int count =0;

        /*删除上次挖掘的频繁模式中的不频繁模式*/
        List<List<Integer>>PList = new ArrayList<>();
        Iterator<Entry<List<Integer>, Mros>> iter_1 = k_MDBmap.entrySet().iterator();
        while (iter_1.hasNext()){
            //使用一个参数来接受迭代器当前的值
            Map.Entry<List<Integer>, Mros> entry =(Entry<List<Integer>,Mros>) iter_1.next();
            List<Integer> itemList = entry.getKey();
            if (itemList.size()==1){
                if (entry.getValue().getItemNum()>=newminSup){
                    PList.add(itemList);
                    count++;
                }
            }else {
                Mros M_x=entry.getValue();
                double sup = M_x.intersectionSizeEstimate(M_x,M_ucid_de);
                if(sup < newminSup){
                    iter_1.remove(); //使用iter_1迭代函数，将不频繁的1-pattern都移除，此时，one_verDBList中只剩频繁的一模式
                }else {
                    count++;
                    PList.add(entry.getKey());
                }
            }

        }


        Iterator<Entry<List<Integer>, Mros>> iter_2 = semi_MDBmap.entrySet().iterator();
        while (iter_2.hasNext()){
            //使用一个参数来接受迭代器当前的值
            Map.Entry<List<Integer>, Mros> entry =(Entry<List<Integer>,Mros>) iter_2.next();
            Mros M_x=entry.getValue();
            List<Integer> itemList = entry.getKey();
            if (itemList.size()==1){
                if (entry.getValue().getItemNum()>=newminSup){
                    PList.add(itemList);
                    count++;
                }
            }else {
                double sup = M_x.intersectionSizeEstimate(M_x,M_ucid_de);
                if(sup < newminSup){
                    //System.out.println("unfrep: "+ entry.getKey()+"Sup: "+sup);
                    iter_2.remove(); //使用iter迭代函数，将不频繁的1-pattern都移除，此时，one_verDBList中只剩频繁的一模式
                }else {
                    count++;
                    PList.add(entry.getKey());
                    //System.out.println("newfreP: "+ entry.getKey()+"Sup: "+sup);
                }
            }
        }

        MemoryLogger.getInstance().checkMemory();
        endTime = System.currentTimeMillis();
        //saveDePattern(newfreList);
        saveDePattern(PList);
        System.out.println("New MinSup: "+ newminSup);
        System.out.println("New count: "+count);
        System.out.println("New totalTime: "+(endTime-startTime));
        System.out.println("New Max memory (mb) : " + MemoryLogger.getInstance().getMaxMemory());
    }


    public static void addMFP(String addDB,String addOutPut, double minSupRe,double delta) throws IOException{
        MemoryLogger.getInstance().reset();
        int addMaxsid =0;
        deWriter =  new BufferedWriter(new FileWriter(addOutPut));//创建一个写对象
        Set<Integer> addItems = new HashSet<>();
        Set<Integer> newfrelist =new HashSet<>();
        startTime = System.currentTimeMillis();


        /**
         * 执行步骤STEP 1:
         * （1）逐行读入数据
         */
        try {
            //读文件
            FileInputStream fin = new FileInputStream(new File(addDB));
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            String thisLine;

            int sid = Maxsid;//增量序列号sid
            int pid = 0;//局部位置信息号pid初始化
            //读文件中的每一行，直到结束
            while((thisLine = reader.readLine()) != null){
                //当这行内容为注释，空时，结束
                if(thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@'){
                    continue;
                }
                for(String token : thisLine.split(" ")){
                    if(token.equals("-1")){
                        pid++;
                    } else if (token.equals("-2")){
                        sid++;
                        addMaxsid = sid;
                        pid = 0;//换行后，需要重置
                    } else {
                        Integer itemName = Integer.parseInt(token);//使用itemName来接受上述token的值

                        if (addItemMap.containsKey(itemName)){
                            if (addItemMap.get(itemName).allInfo.containsKey(sid)){
                                addItemMap.get(itemName).allInfo.get(sid).add(pid);
                            }else {
                                List<Integer> posList = new ArrayList<>();
                                posList.add(pid);
                                addItemMap.get(itemName).UCID_Mros.add(sid);
                                addItemMap.get(itemName).allInfo.put(sid,posList);
                            }
                        }else {
                            VerDB_Mros verDB = new VerDB_Mros();
                            verDB.UCID_Mros.add(sid);
                            List<Integer> posList = new ArrayList<>();
                            posList.add(pid);
                            verDB.allInfo.put(sid,posList);
                            addItemMap.put(itemName,verDB);
                        }
                        /*存到之前的一模式数据库中*/
                        if (one_verDBList.containsKey(itemName)){
                            if (one_verDBList.get(itemName).allInfo.containsKey(sid)){
                                one_verDBList.get(itemName).allInfo.get(sid).add(pid);
                            }else {
                                List<Integer> posList = new ArrayList<>();
                                posList.add(pid);
                                one_verDBList.get(itemName).UCID_Mros.add(sid);
                                one_verDBList.get(itemName).allInfo.put(sid,posList);
                            }

                        }else {
                            VerDB_Mros verDB = new VerDB_Mros();
                            verDB.UCID_Mros.add(sid);
                            List<Integer> posList = new ArrayList<>();
                            posList.add(pid);
                            verDB.allInfo.put(sid,posList);
                            one_verDBList.put(itemName,verDB);

                        }
                        addItems.add(itemName);
                    }
                }
            }
            //关闭写文件
            reader.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        double newsup = Math.ceil(minSupRe*addMaxsid);
        Set<Integer> freItemList_temp = new HashSet<>();
        for (Integer x : freItemList){
            if (one_verDBList.get(x).allInfo.size()>=newsup){
                add_FreCount++;
                freItemList_temp.add(x);
            }
        }
        newfrelist.addAll(addItems);
        newfrelist.removeAll(freItemList_temp);
        for (Integer x : newfrelist){
            if (one_verDBList.get(x).allInfo.size()>=newsup){
                add_FreCount++;
                freItemList_temp.add(x);
            }
        }

        double threshold = newsup*(1-delta);


        /*挖掘频繁的和可能频繁的二模式*/
        Set<List<Integer>> two_maylist = new HashSet<>();
        Map<List<Integer>, VerDB_Mros> maybeverDB_temp = new HashMap<>();
        for (Integer x : addItems){
            List<Integer> canlist_temp = new ArrayList<>();
            VerDB_Mros verDB_x = addItemMap.get(x);
            for (Integer y : addItems){
                List<Integer> itemList = new ArrayList<>();
                itemList.add(x);
                itemList.add(y);
                VerDB_Mros verDB_y = addItemMap.get(y);
                if (k_MDBmap.containsKey(itemList)){
                    /*当原来就是频繁的k模式的时候*/

                    VerDB_Mros verDB_xy = ExtendP(verDB_x,verDB_y,minSupRe*addMaxsid);
                    for (Integer ucid : verDB_xy.allInfo.keySet()){
                        k_MDBmap.get(itemList).add(ucid);
                    }
                    if (k_MDBmap.get(itemList).getItemNum()>=newsup){
                        add_FreCount++;
                        canlist_temp.add(y);
                        two_maylist.add(itemList);
                        addFreList.add(itemList);
                        maybeverDB_temp.put(itemList,verDB_xy);
                    }
//                    else if (k_MDBmap.get(itemList).getItemNum()>=newsup*delta){
//                        canlist_temp.add(y);
//                        two_maylist.add(itemList);
//                        maybeverDB_temp.put(itemList,verDB_xy);
//                        k_MDBmap.remove(itemList);
//                        semi_MDBmap.put(itemList,verDB_xy.UCID_Mros);
//                    }
                } else if (semi_MDBmap.containsKey(itemList)) {
                    VerDB_Mros verDB_xy = ExtendP(verDB_x,verDB_y,minSupRe*addMaxsid);
                    /*当原来是版频繁模式的时候*/
                    for (Integer ucid : verDB_xy.allInfo.keySet()){
                        semi_MDBmap.get(itemList).add(ucid);
                    }
                    if (semi_MDBmap.get(itemList).getItemNum()>=newsup){
                        add_FreCount++;
                        canlist_temp.add(y);
                        two_maylist.add(itemList);
                        addFreList.add(itemList);
                        maybeverDB_temp.put(itemList,verDB_xy);
                        k_MDBmap.put(itemList,semi_MDBmap.get(itemList));
                    }
//                    else if (semi_MDBmap.get(itemList).getItemNum()>=newsup*delta){
//                        canlist_temp.add(y);
//                        two_maylist.add(itemList);
//                        maybeverDB_temp.put(itemList,verDB_xy);
//                        semi_MDBmap.put(itemList,verDB_xy.UCID_Mros);
//                    }
                }
//                else if (verDB_xy.allInfo.size()>=threshold){
//                    /*当原来的数据库中没有时候*/
//                    canlist_temp.add(y);
//                    two_maylist.add(itemList);
//                    maybeverDB_temp.put(itemList,verDB_xy);
//                    maybe_MDBmap.put(itemList,verDB_xy.UCID_Mros);
//                }
            }
            itemCad_temp.put(x,canlist_temp);
        }

        for (List<Integer> two_integers : two_maylist){
            Integer last = two_integers.get(two_integers.size()-1);
            List<Integer> canList_1 = itemCad_temp.get(last);
            VerDB_Mros verDBMros_1 = maybeverDB_temp.get(two_integers);
            if (canList_1 != null){
                GrowP_add(two_integers,verDBMros_1,canList_1,newsup,delta);
            }
            //savePattern(prefixVerList,minSup);
        }

        Set<List<Integer>> freList = k_MDBmap.keySet();
        freList.removeAll(addFreList);
        for (List<Integer> x : freList){
            double sup_x =k_MDBmap.get(x).getItemNum();
            if (sup_x>=newsup){
                addFreList.add(x);
            }
//            else if (sup_x>=newsup*delta){
////                k_MDBmap.remove(x);
//                semi_MDBmap.put(x,k_MDBmap.get(x));
//            }
        }
        MemoryLogger.getInstance().checkMemory();
        endTime = System.currentTimeMillis();
        saveDePattern(freItemList);
        saveAddPattern(addFreList);
        System.out.println("New ADD MinSup: "+ newsup);
        System.out.println("New fre count: "+add_FreCount);
        System.out.println("New ADD totalTime: "+(endTime-startTime)+" ms");
        System.out.println("New ADD Max memory (mb) : " + MemoryLogger.getInstance().getMaxMemory());
    }

    public static void GrowP_add(List<Integer> fre_x,VerDB_Mros VerDB_x,List<Integer> candidateList,double newminSup,double delta) throws IOException{
        Mros Mros_x = VerDB_x.UCID_Mros;
        Map<List<Integer>,VerDB_Mros> freVerList_new =new HashMap<>();
        for (Integer y : candidateList) {
            List<Integer> item_new = new ArrayList<>();
            item_new.addAll(fre_x);//深拷贝、浅拷贝有区别
            item_new.add(y);
            if (k_MDBmap.containsKey(item_new)){
                VerDB_Mros verDB_xy = ExtendP(VerDB_x,addItemMap.get(y),newminSup);
                for (Integer ucid : verDB_xy.allInfo.keySet()){
                    k_MDBmap.get(item_new).add(ucid);
                }
                if (k_MDBmap.get(item_new).getItemNum()>=newminSup){
                    //fully_FreCount++;
                    //addFreList.add(item_new);
                    freVerList_new.put(item_new,verDB_xy);
                }
//                else if (k_MDBmap.get(item_new).getItemNum()>=newminSup*(1-delta)){
//                    k_MDBmap.remove(item_new);
//                    semi_MDBmap.put(item_new,verDB_xy.UCID_Mros);
//                    freVerList_new.put(item_new,verDB_xy);
//                }
            } else if (semi_MDBmap.containsKey(item_new)) {
                VerDB_Mros verDB_xy = ExtendP(VerDB_x,addItemMap.get(y),newminSup);
                for (Integer ucid : verDB_xy.allInfo.keySet()){
                    semi_MDBmap.get(item_new).add(ucid);
                }

                if (semi_MDBmap.get(item_new).getItemNum()>=newminSup){
                    //fully_FreCount++;
                    //addFreList.add(item_new);
                    //k_MDBmap.put(item_new,semi_MDBmap.get(item_new));
                    freVerList_new.put(item_new,verDB_xy);
                }
//                else if (semi_MDBmap.get(item_new).getItemNum()>=newminSup*(1-delta)){
//                    semi_MDBmap.put(item_new,verDB_xy.UCID_Mros);
//                    freVerList_new.put(item_new,verDB_xy);
//                }
            }
//            Mros Mros_y = addItemMap.get(y).UCID_Mros;
//            double xANDy = Mros_x.intersectionSizeEstimate(Mros_x,Mros_y);
//            if (xANDy >=newminSup){
//                VerDB_Mros verDB_xy = ExtendP(VerDB_x,addItemMap.get(y),newminSup);
//                if (verDB_xy.allInfo.size()>=newminSup) {
//                    freVerList_new.put(item_new,verDB_xy);
//                    maybe_MDBmap.put(item_new,verDB_xy.UCID_Mros);
//                }
//            }
        }
        for (Entry<List<Integer>,VerDB_Mros> entry : freVerList_new.entrySet()){
            Integer last = entry.getKey().get(entry.getKey().size()-1);
            List<Integer> candList_new = itemCad_temp.get(last);
            if (candList_new!=null) {
                GrowP_add(entry.getKey(), entry.getValue(), candList_new, newminSup, delta);
            }
        }
    }


    public static void FullyMFP(String addDB,String deDB,String fullyOutPut, double minSupRe,double delta) throws IOException{
        MemoryLogger.getInstance().reset();
        int addMaxsid =0;
        deWriter =  new BufferedWriter(new FileWriter(fullyOutPut));//创建一个写对象
        Set<Integer> addItems = new HashSet<>();
        Set<Integer> newfrelist =new HashSet<>();
        startTime = System.currentTimeMillis();


        /**
         * 增量数据库
         */
        try {
            //读文件
            FileInputStream fin = new FileInputStream(new File(addDB));
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            String thisLine;

            int sid = Maxsid;//增量序列号sid
            int pid = 0;//局部位置信息号pid初始化
            //读文件中的每一行，直到结束
            while((thisLine = reader.readLine()) != null){
                //当这行内容为注释，空时，结束
                if(thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@'){
                    continue;
                }
                for(String token : thisLine.split(" ")){
                    if(token.equals("-1")){
                        pid++;
                    } else if (token.equals("-2")){
                        sid++;
                        Maxsid = sid;
                        pid = 0;//换行后，需要重置
                        M_ucid_de.add(sid);
                    } else {
                        Integer itemName = Integer.parseInt(token);//使用itemName来接受上述token的值

                        if (addItemMap.containsKey(itemName)){
                            if (addItemMap.get(itemName).allInfo.containsKey(sid)){
                                addItemMap.get(itemName).allInfo.get(sid).add(pid);
                            }else {
                                List<Integer> posList = new ArrayList<>();
                                posList.add(pid);
                                addItemMap.get(itemName).UCID_Mros.add(sid);
                                addItemMap.get(itemName).allInfo.put(sid,posList);
                            }
                        }else {
                            VerDB_Mros verDB = new VerDB_Mros();
                            verDB.UCID_Mros.add(sid);
                            List<Integer> posList = new ArrayList<>();
                            posList.add(pid);
                            verDB.allInfo.put(sid,posList);
                            addItemMap.put(itemName,verDB);
                        }
                        /*存到之前的一模式数据库中*/
                        if (one_verDBList.containsKey(itemName)){
                            if (one_verDBList.get(itemName).allInfo.containsKey(sid)){
                                one_verDBList.get(itemName).allInfo.get(sid).add(pid);
                            }else {
                                List<Integer> posList = new ArrayList<>();
                                posList.add(pid);
                                one_verDBList.get(itemName).UCID_Mros.add(sid);
                                one_verDBList.get(itemName).allInfo.put(sid,posList);
                            }

                        }else {
                            VerDB_Mros verDB = new VerDB_Mros();
                            verDB.UCID_Mros.add(sid);
                            List<Integer> posList = new ArrayList<>();
                            posList.add(pid);
                            verDB.allInfo.put(sid,posList);
                            one_verDBList.put(itemName,verDB);

                        }
                        addItems.add(itemName);
                    }
                }
            }
            //关闭写文件
            reader.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        int deMaxsid =0;

        /**
         * 执行步骤STEP 1:
         * （1）逐行读入数据
         * （2）生成所有的1-pattern垂直数据库表
         * （3）生成所有的频繁1-pattern垂直数据库表并存入内存
         * 其中，包含，项目名称，项目中的UCID，及其当前位置和上一个位置信息，及其交集（支持度）大小，
         * 使用两种数据结构，将数据分为两部分存储，频繁的存储一部分，不频繁的存储为一部分；
         * 其中，不频繁的只存储其MROS结构，当计算发现其频繁后，再将其加载进入内存，进行挖掘。
         */
        try {
            //读文件
            FileInputStream fin = new FileInputStream(new File(deDB));
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            String thisLine;

            int sid = 0;//序列号sid初始化
            int pid = 0;//局部位置信息号pid初始化
            //读文件中的每一行，直到结束
            Map<Integer,Set<Integer>> T_temp = new HashMap<>();
            while((thisLine = reader.readLine()) != null){
                //当这行内容为注释，空时，结束
                if(thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@'){
                    continue;
                }
                for(String token : thisLine.split(" ")){
                    if(token.equals("-1")){
                        pid++;
                    } else if (token.equals("-2")){
                        sid++;
                        deMaxsid = sid;
                        pid = 0;//换行后，需要重置
                        M_ucid_de.remove(sid);
                    } else {
                        Integer itemName = Integer.parseInt(token);//使用itemName来接受上述token的值
                        //第一次在itemname中出现的sid删除掉
                        if (T_temp.containsKey(itemName)){
                            if (T_temp.get(itemName).contains(sid)){
                                continue;
                            }else {
                                if (k_MDBmap.containsKey(itemName)){
                                    k_MDBmap.get(itemName).remove(sid);
                                } else if (semi_MDBmap.containsKey(itemName)) {
                                    semi_MDBmap.get(itemName).remove(sid);
                                }
                            }
                        }else{
                            Set<Integer> sidSet=new HashSet<>();
                            sidSet.add(sid);
                            T_temp.put(itemName,sidSet);
                            if (k_MDBmap.containsKey(itemName)){
                                k_MDBmap.get(itemName).remove(sid);
                            } else if (semi_MDBmap.containsKey(itemName)) {
                                semi_MDBmap.get(itemName).remove(sid);
                            }
                        }
                    }
                }
            }

            //关闭写文件
            reader.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        /*更新减量后的最小支持度*/
        double newminSup = Math.ceil((Maxsid-deMaxsid)*minSupRe);
        int count =0;
        System.out.println(M_ucid_de.getItemNum());

        Set<Integer> freItemList_temp = new HashSet<>();
        newfrelist.addAll(addItems);
        newfrelist.addAll(freItemList);
        newfrelist.addAll(semiItemList);
        for (Integer x : newfrelist){
            if (one_verDBList.get(x).allInfo.size()>=newminSup){
                freItemList_temp.add(x);
            }
        }

        double threshold = newminSup*(1-delta);


        /*挖掘频繁的和可能频繁的二模式*/
        Set<List<Integer>> two_maylist = new HashSet<>();
        Map<List<Integer>, VerDB_Mros> maybeverDB_temp = new HashMap<>();
        for (Integer x : addItems){
            List<Integer> canlist_temp = new ArrayList<>();
            VerDB_Mros verDB_x = addItemMap.get(x);
            for (Integer y : addItems){
                List<Integer> itemList = new ArrayList<>();
                itemList.add(x);
                itemList.add(y);
                VerDB_Mros verDB_y = addItemMap.get(y);
                if (k_MDBmap.containsKey(itemList)){
                    /*当原来就是频繁的k模式的时候*/

                    VerDB_Mros verDB_xy = ExtendP(verDB_x,verDB_y,minSupRe*addMaxsid);
                    for (Integer ucid : verDB_xy.allInfo.keySet()){
                        k_MDBmap.get(itemList).add(ucid);
                    }
                    Mros M_x = k_MDBmap.get(itemList);
                    double sup_1 = M_x.intersectionSizeEstimate(M_x,M_ucid_de);
                    if (sup_1 >= newminSup){
                        canlist_temp.add(y);
                        two_maylist.add(itemList);
                        //addFreList.add(itemList);
                        maybeverDB_temp.put(itemList,verDB_xy);
                    }
//                    else if (k_MDBmap.get(itemList).getItemNum()>=newsup*delta){
//                        canlist_temp.add(y);
//                        two_maylist.add(itemList);
//                        maybeverDB_temp.put(itemList,verDB_xy);
//                        k_MDBmap.remove(itemList);
//                        semi_MDBmap.put(itemList,verDB_xy.UCID_Mros);
//                    }
                } else if (semi_MDBmap.containsKey(itemList)) {
                    VerDB_Mros verDB_xy = ExtendP(verDB_x,verDB_y,minSupRe*addMaxsid);
                    /*当原来是版频繁模式的时候*/
                    for (Integer ucid : verDB_xy.allInfo.keySet()){
                        semi_MDBmap.get(itemList).add(ucid);
                    }
                    Mros M_x = semi_MDBmap.get(itemList);
                    double sup_1 = M_x.intersectionSizeEstimate(M_x,M_ucid_de);
                    if (sup_1 >= newminSup){
                        canlist_temp.add(y);
                        two_maylist.add(itemList);
                        //addFreList.add(itemList);
                        maybeverDB_temp.put(itemList,verDB_xy);
                        k_MDBmap.put(itemList,semi_MDBmap.get(itemList));
                    }
//                    else if (semi_MDBmap.get(itemList).getItemNum()>=newsup*delta){
//                        canlist_temp.add(y);
//                        two_maylist.add(itemList);
//                        maybeverDB_temp.put(itemList,verDB_xy);
//                        semi_MDBmap.put(itemList,verDB_xy.UCID_Mros);
//                    }
                }
//                else if (verDB_xy.allInfo.size()>=threshold){
//                    /*当原来的数据库中没有时候*/
//                    canlist_temp.add(y);
//                    two_maylist.add(itemList);
//                    maybeverDB_temp.put(itemList,verDB_xy);
//                    maybe_MDBmap.put(itemList,verDB_xy.UCID_Mros);
//                }
            }
            itemCad_temp.put(x,canlist_temp);
        }

        for (List<Integer> two_integers : two_maylist){
            Integer last = two_integers.get(two_integers.size()-1);
            List<Integer> canList_1 = itemCad_temp.get(last);
            VerDB_Mros verDBMros_1 = maybeverDB_temp.get(two_integers);
            if (canList_1 != null){
                GrowP_add(two_integers,verDBMros_1,canList_1,newminSup,delta);
            }
            //savePattern(prefixVerList,minSup);
        }

        Set<List<Integer>> freList = k_MDBmap.keySet();
        System.out.println("k_map: "+freList.size());
        Set<List<Integer>> fullyFreList = new HashSet<>();
        for (List<Integer> x : freList){
            Mros M_x = k_MDBmap.get(x);
            double sup_x = M_x.intersectionSizeEstimate(M_x,M_ucid_de);
            if (sup_x>=newminSup){
                fully_FreCount++;
                fullyFreList.add(x);
            }
//            else if (sup_x>=newsup*delta){
////                k_MDBmap.remove(x);
//                semi_MDBmap.put(x,k_MDBmap.get(x));
//            }
        }
        Set<List<Integer>> freList_1 = semi_MDBmap.keySet();
        System.out.println("semi_map: "+freList_1.size());
        for (List<Integer> x : freList_1){
            Mros M_x = semi_MDBmap.get(x);
            double sup_x = M_x.intersectionSizeEstimate(M_x,M_ucid_de);
            if (sup_x>=newminSup){
                fully_FreCount++;
                fullyFreList.add(x);
            }
//            else if (sup_x>=newsup*delta){
////                k_MDBmap.remove(x);
//                semi_MDBmap.put(x,k_MDBmap.get(x));
//            }
        }

        MemoryLogger.getInstance().checkMemory();
        endTime = System.currentTimeMillis();
        //saveDePattern(freItemList);
        saveAddPattern(fullyFreList);
        System.out.println("New fully MinSup: "+ newminSup);
        System.out.println("New fre count: "+fully_FreCount);
        System.out.println("New fully totalTime: "+(endTime-startTime)+" ms");
        System.out.println("New fully Max memory (mb) : " + MemoryLogger.getInstance().getMaxMemory());
    }



    /**
     * 将挖掘出的频繁模式输出至结果文件
     */
    private void savePattern (HashMap<List,VerDB_Mros> one_verDBListTemp, double minSup) throws IOException {
        StringBuilder r = new StringBuilder("");
        for(Entry<List,VerDB_Mros> entry: one_verDBListTemp.entrySet()){
            /*
             * 输出频繁k-模式的项目名称
             */
			r.append('(');
            List<Integer> itemNames = new ArrayList<>();
            itemNames = entry.getKey();
            r.append("itemName: ");
            for(Integer itemName : itemNames){
                String string = itemName.toString();
                r.append(string);
                r.append(" -1 ");
            }
            r.append(')');
            r.append("\n");

            /*
             * 输出频繁k-模式的支持度
             */
            r.append("#SUP: ");//支持度提示
            r.append(entry.getValue().getSupport());//支持度
            r.append("\n");
            patternCount++; // 添加模式总数
        }
        r.append(patternCount);
        r.append("\n");
        writer.write(r.toString());
        writer.newLine();
        writer.flush();
    }

    /**
     * 将挖掘出的频繁模式输出至结果文件
     */
    private static void saveDePattern(Set<Integer> Fre) throws IOException {
        StringBuilder dr = new StringBuilder("");
        for(Integer p : Fre){
            /*
             * 输出频繁1-模式的项目名称
             */
            dr.append(p);
            dr.append(" -2");
            /*
             * 输出频繁1-模式的支持度
             */
//            dr.append("#SUP: ");//支持度提示
//            dr.append(one_verDBList.get(p).allInfo.size());//支持度
            dr.append("\n");
        }
        deWriter.write(dr.toString());
        deWriter.newLine();
        deWriter.flush();
    }
    private static void saveDePattern(List<List<Integer>> deFreList) throws IOException {
        StringBuilder dr = new StringBuilder("");
        for(List<Integer> pList : deFreList){
            for (Integer p : pList){
                /*
                 * 输出频繁1-模式的项目名称
                 */
                dr.append(p);
                dr.append(" -1 ");

            }
            dr.append(" -2");
            /*
             * 输出频繁1-模式的支持度
             */
//            dr.append("#SUP: ");//支持度提示
//            dr.append(one_verDBList.get(p).allInfo.size());//支持度
            dr.append("\n");
        }
        deWriter.write(dr.toString());
        deWriter.newLine();
        deWriter.flush();
    }

    private static void saveAddPattern(Set<List<Integer>> addFreList) throws IOException {
        StringBuilder dr = new StringBuilder("");
        for(List<Integer> pList : addFreList){
            for (Integer p : pList){
                /*
                 * 输出频繁1-模式的项目名称
                 */
                dr.append(p);
                dr.append(" -1 ");

            }
            dr.append(" -2");
            /*
             * 输出频繁1-模式的支持度
             */
//            dr.append("#SUP: ");//支持度提示
//            dr.append(one_verDBList.get(p).allInfo.size());//支持度
            dr.append("\n");
        }
        deWriter.write(dr.toString());
        deWriter.newLine();
        deWriter.flush();
    }

    public void printStatistics() {
        StringBuilder r = new StringBuilder(200);
        r.append("=============  De_CSPM v0.23/08/28 - STATISTICS =============\n Total time ~ ");
        r.append(endTime - startTime);
        r.append(" ms\n");
        r.append(" Frequent sequences count : " + patternCount);
        r.append('\n');
        r.append("the number of extension : "+ extension);

        r.append('\n');
        r.append(" the number of purning : "+withPurnCount);
        r.append(" Max memory (mb) : " );
        r.append(MemoryLogger.getInstance().getMaxMemory());
//        r.append(patternCount);
        r.append('\n');
        r.append("minsup " + minSup);
        r.append('\n');
        r.append("=========================================================\n");
        System.out.println(r.toString());
    }



}
