package ru.programpark.entity.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * User: oracle
 * Date: 27.05.14
 * Класс для хранения персептов для добавления и удаления
 */

public class Percepts {
    private String[] addPercepts = null;
    private String[] delPercepts = null;

    public Percepts(String filename) {
        this(filename, null);
    }

    public Percepts(String filename, Integer batchIndex) {
        List<String> percepts = new ArrayList();
        int[] counters = readPercepts(filename, percepts, batchIndex);
        addPercepts = selectPercepts(percepts, counters[0], "+");
        delPercepts = selectPercepts(percepts, counters[1], "-");
    }

    public Percepts(String[] filenames) {
        List<String> percepts = new ArrayList();
        int counterAdd = 0, counterDel = 0;
        for (int i = 0; i < filenames.length; ++i) {
            int[] counters = readPercepts(filenames[i], percepts, null);
            counterAdd += counters[0];
            counterDel += counters[1];
        }
        addPercepts = selectPercepts(percepts, counterAdd, "+");
        delPercepts = selectPercepts(percepts, counterDel, "-");
    }

    public boolean areEmpty() {
        return (addPercepts == null || addPercepts.length == 0) &&
            (delPercepts == null || delPercepts.length == 0);
    }

    private int[] readPercepts(String fileName, List<String> percepts,
                               Integer batchIndex) {
        BufferedReader reader = null;
        int[] counters = {0, 0};
        try {
            reader = new BufferedReader((fileName.startsWith("resource:"))
                ? new InputStreamReader(getClass().getResourceAsStream(fileName.substring(9)))
                : new FileReader(fileName)
            );
            String str, sstr;
            int curIndex = -1;
            boolean record = (batchIndex == null || batchIndex < 1);
            while ((str = reader.readLine()) != null) {
                if (batchIndex != null) {
                    if (str.contains("BEGIN percepts")) {
                        record = (++curIndex == batchIndex);
                        continue;
                    } else if (str.contains("END percepts")) {
                        record = false;
                        continue;
                    }
                }
                if (record && (sstr = specialForm(str)) != null) {
                    counters[0]++;
                    percepts.add("+" + sstr);
                } else if (record && str.startsWith("+")) {
                    counters[0]++;
                    percepts.add(str);
                } else if (record && str.startsWith("-")) {
                    counters[1]++;
                    percepts.add(str);
                }
            }
            if (curIndex == -1 && batchIndex != null && batchIndex == 0) {
                // Это значит, что в файле не было меток BEGIN/END percepts.
                // В этом случае считываем снова, и теперь уже всё, что там
                // есть.
                counters = readPercepts(fileName, percepts, null);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return counters;
    }

    private static Pattern STATION_NAME_RE =
        Pattern.compile("^\\s+([0-9]+)\\s+=\\s+([\\p{Lu}\\d -]+)\\s+\\([0-9]+\\)\\s*$");
    private static String STATION_NAME_TERM =
        "station_name($1, \"$2\")";

    private String specialForm(String str) {
        Matcher m = STATION_NAME_RE.matcher(str);
        if (m.matches()) return m.replaceFirst(STATION_NAME_TERM);
        return null;
    }

    private String[] selectPercepts(List<String> percepts, int counter, String opPrefix) {
        String[] arrPercepts = new String[counter];
        counter = 0;
        for (String percept : percepts) {
            if (percept.startsWith(opPrefix)) {
                arrPercepts[counter] = percept;
                ++counter;
            }
        }
        return arrPercepts;
    }

    public String[] getAddPercepts() {
        return addPercepts;
    }


    public String[] getDelPercepts() {
        return delPercepts;
    }

    public String[] getAllPercepts() {
        return mergePercepts(addPercepts, delPercepts);
    }

    public static String[] mergePercepts(String[] addPercepts, String[] delPercepts){
        ArrayList<String> perceptList = new ArrayList(Arrays.asList(addPercepts));
        perceptList.addAll(Arrays.asList(delPercepts));
        String[] res = new String[perceptList.size()];
        perceptList.toArray(res);//все перцепты вместе

        return res;
    }

    public void verifyPercepts(String[] percepts, String opPrefix) {
        for (int i = 0; i < percepts.length; i++){
            if (!percepts[i].startsWith(opPrefix))
                percepts[i] = opPrefix + percepts[i];
        }
    }

}
