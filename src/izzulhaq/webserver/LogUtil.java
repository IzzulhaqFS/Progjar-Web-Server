package izzulhaq.webserver;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class LogUtil {
    private static final String FILE_LOG = "web_server_logs.txt";
    private static List<String> logs = new LinkedList<String>();
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyy HH:mm:ss");

    public static void write(String log) {
        write(log, true);
    }

    public static void write(String log, boolean print) {
        String message = simpleDateFormat.format(new Date()) + " " + log;
        logs.add(message);

        if (print) {
            System.out.println(message);
        }
    }

    public static void save(boolean append) {
        try {
            if (logs != null && logs.size() > 0) {
                FileWriter fileWriterLog = new FileWriter(FILE_LOG, append);
                BufferedWriter bwLog = new BufferedWriter(fileWriterLog);

                for (String str : logs) {
                    bwLog.write(str);
                    bwLog.newLine();
                }

                bwLog.close();
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("Unable to open file '" + FILE_LOG + "'");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void clear() {
        logs.clear();
    }
}
