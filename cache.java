import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class cache {
    String[][] cache;
    int sets;
    int asc;
    int tag;
    int index;
    int offset;
    int[][] lru;
    int[][] dirty;
    int[][] valid;
    ArrayList<String> opt;
    String[][] tag_index;
    int read = 0;
    int read_miss = 0;
    int write = 0;
    int write_miss = 0;
    int write_back = 0;
    int write_back_m = 0;
    int rp;
    int count;

    // Cache Constructor
    public cache (int block_size, int cache_size, int assoc, int replacement_policy, String trace) {
        if (cache_size != 0) {
            sets = cache_size / (block_size * assoc);
            asc = assoc;
            rp = replacement_policy;
            cache = new String[sets][assoc];
            tag_index = new String[sets][assoc];
            dirty = new int[sets][assoc];
            valid = new int[sets][assoc];
            offset = (int) (Math.log(block_size) / Math.log(2));
            index = (int) (Math.log(sets) / Math.log(2));
            tag = 32 - index - offset;
            if (rp == 0 || assoc == 1){
                lru = new int[sets][assoc];
            }else if (rp == 1) {
                int x = 0;
                int p = 1;
                float temp = 0.1f;
                while (temp != 0) {
                    temp = (float) (assoc / Math.pow(2, p));
                    if (temp > 0.5) {
                        x += Math.ceil(temp);
                        p += 1;
                    } else {
                        temp = 0;
                    }
                }
                lru = new int[sets][x];
            }else if (rp == 2){
                opt = new ArrayList<>();
                Scanner trace_copy = null;
                File file_copy = new File(trace);
                try {
                    trace_copy = new Scanner(file_copy);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                while (trace_copy.hasNextLine()) {
                    String[] address = trace_copy.nextLine().split(" ");
                    String addr = address[1];
                    int v = Integer.parseInt(addr, 16);
                    String v1 = Integer.toBinaryString(v);
                    while (v1.length() < 32) {
                        v1 = "0" + v1;
                    }
                    opt.add(v1.substring(0,(tag+index)));
                }
            }
        }
    }

    // Get tag and index from address
    public List getTagIndex(String address) {
        List ti = new ArrayList();
        int v = Integer.parseInt(address, 16);
        String v1 = Integer.toBinaryString(v);
        while (v1.length() < 32) {
            v1 = "0" + v1;
        }
        String tg = v1.substring(0, tag);
        String idx = v1.substring(tag, tag+index);
        ti.add(tg);
        ti.add(idx);
        return ti;
    }

    // Read address
    public boolean read(String address){
        boolean status = false;
        int idx = 0;
        read++;
        List ti = getTagIndex(address);
        String tg = (String) ti.get(0);
        String idxx = (String) ti.get(1);
        if (sets != 1) {
            idx = Integer.parseInt(idxx, 2);
        }

        for (int i=0; i < asc ; i++) {
            if (valid[idx][i] == 1){
                if (cache[idx][i].equals(tg)) {
                    if (rp == 0 || asc == 1) {
                        updateLRU(idx, i);
                    }
                    else if (rp == 1) {
                        updatePLRU(idx, i);
                    }
                    status = true;
                    break;
                }
            }
        }
        if (status == false) {
            read_miss++;
        }
        return status;
    }


    // Write address
    public boolean write(String address){
        write++;
        int idx = 0;
        boolean status = false;
        List ti = getTagIndex(address);
        String tg = (String) ti.get(0);
        String idxx = (String) ti.get(1);
        if (sets != 1) {
            idx = Integer.parseInt(idxx, 2);
        }

        for (int i=0; i < asc ; i++) {
            if (valid[idx][i] == 1){
                if (cache[idx][i].equals(tg)) {
                    if (rp == 0 || asc == 1) {
                        updateLRU(idx, i);
                    }
                    else if (rp == 1) {
                        updatePLRU(idx, i);
                    }
                    dirty[idx][i] = 1;
                    status = true;
                    break;
                }
            }
        }
        if (status == false) {
            write_miss++;
        }
        return status;
    }

    // When cache miss tries to allocate address into empty block or calls replacement policy
    public String allocate(String address, int counter, String op){
        boolean status = false;
        int idx = 0;
        List ti = getTagIndex(address);
        String tg = (String) ti.get(0);
        String idxx = (String) ti.get(1);
        if (sets != 1) {
            idx = Integer.parseInt(idxx, 2);
        }

        for (int i = 0; i < asc ; i++) {
            if (valid[idx][i] == 0) {
                cache[idx][i] = tg;
                tag_index[idx][i] = tg+idxx;
                valid[idx][i] = 1;
                status = true;
                if (rp == 0 || asc == 1) {
                    updateLRU(idx, i);
                }
                else if (rp == 1) {
                    updatePLRU(idx, i);
                }
                if (op.equals("w")) {
                    dirty[idx][i] = 1;
                }
                else {
                    dirty[idx][i] = 0;
                }
                break;
            }
        }
        if (status == false) {
            return allocateReplacement(address, counter, op);
        }
        return "";
    }

    // Replace cache block using replacement policy
    public String allocateReplacement(String address, int counter, String op) {
        int idx = 0;
        List ti = getTagIndex(address);
        String tg = (String) ti.get(0);
        String idxx = (String) ti.get(1);
        if (sets != 1) {
            idx = Integer.parseInt(idxx, 2);
        }
        String out = "";

        int r_idx = 0;
        if (rp == 0 || asc == 1) {
            r_idx = getLRU(idx);
        }
        else if (rp == 1) {
            r_idx = getPLRU(idx);
        }
        else if (rp == 2){
            r_idx = getOpt(idx, counter);
        }

        if (dirty[idx][r_idx] == 1){
            write_back++;
            out = tag_index[idx][r_idx];
        }
        cache[idx][r_idx] = tg;
        tag_index[idx][r_idx] = tg+idxx;
        valid[idx][r_idx] = 1;

        if (rp == 0 || asc == 1) {
            updateLRU(idx, r_idx);
        }
        else if (rp == 1) {
            updatePLRU(idx, r_idx);
        }

        if (op.equals("w")) {
            dirty[idx][r_idx] = 1;
        }else {
            dirty[idx][r_idx] = 0;
        }
        return out;
    }

    // Make invalid
    public void makeInvalid(String address){
        int out = 0;
        int idx = 0;
        List ti = getTagIndex(address);
        String tg = (String) ti.get(0);
        String idxx = (String) ti.get(1);
        if (sets != 1) {
            idx = Integer.parseInt(idxx, 2);
        }

        for (int i=0; i<asc; i++) {
            if (cache[idx][i].equals(tg)) {
                    valid[idx][i] = 0;
                    out = dirty[idx][i];
                    break;
            }
        }
        if (out == 1) {
            write_back_m++;
        }
    }

    // get LRU block
    public int getLRU(int set) {
        int min_idx = 0;
        for (int i=0 ; i < asc ; i++) {
            if (lru[set][i] < lru[set][min_idx]) {
                min_idx = i;
            }
        }
        return min_idx;
    }

    // Update LRU
    public void updateLRU(int set, int assoc) {
        lru[set][assoc] = count;
        count++;
    }

    // get Pseudo LRU block
    public int getPLRU(int set) {
        String x = "";
        int i = 1;
        while(i-1 < lru[0].length) {
            x += lru[set][i-1];
            if (lru[set][i-1] == 0) {
                i = (2*i) + 1;
            } else {
                i = 2*i;
            }
        }
        String[] xx = x.split("");
        String y = "";
        for (int j=0; j<x.length(); j++) {
            if (xx[j].equals("0")){
                y += "1";
            }
            else {
                y += "0";
            }
        }
        return Integer.parseInt(y,2);
    }

    // Update Pseudo LRU
    public void updatePLRU(int set, int idx) {
        String x = Integer.toBinaryString(idx);
        int l = (int) Math.ceil( Math.log(asc) / Math.log(2) );
        while (x.length() < l) {
            x = "0" + x;
        }
        String[] y = x.split("");
        int f = 1;
        for (int i=0; i<x.length(); i++) {
            lru[set][f-1] = Integer.parseInt(y[i]);
            if (y[i].equals("0")) {
                f = (2*f);
            }
            else {
                f = (2*f) + 1;
            }
        }
    }


    // Optimal Replacement Policy
    public int getOpt(int set, int counter){
        int[] temp = new int[asc];
        int count = 0;
        for (int j=0; j<asc; j++){
            for (int i=counter-1; i<opt.size(); i++) {
                if (tag_index[set][j].equals(opt.get(i))) {
                    temp[j] = i;
                    count++;
                    break;
                    }
                }
            if (count == j) {
                return j;
            }
        }
        int max_idx = 0;
        for (int k=0; k<asc; k++) {
            if (temp[k] > temp[max_idx]) {
                max_idx = k;
            }
        }
        return max_idx;
    }
}
