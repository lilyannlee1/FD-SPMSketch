import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerDB_Mros {
    Mros UCID_Mros = new Mros(10,16,0.38,8);
    Map<Integer, List<Integer>> allInfo = new HashMap<>();
    private int support = 0;
    public int getSupport(){
        Integer support = 0 ;//求和公式，求所有不为空的curMros的个数,所以，在mros中得有一个判断mros是否为空的函数
        support = allInfo.size();
        return support;
    }

}