package engine;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class BackupManager {

    // שמירת המערכת כולה לקובץ
    public static void saveMarket(Market market, String filePath) throws Exception {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath + ".dat"))) {
            out.writeObject(market);
        }
    }

    // טעינת מערכת מקובץ גיבוי
    public static Market loadMarket(String filePath) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath + ".dat"))) {
            return (Market) in.readObject(); // קורא את האובייקט חזרה לזיכרון
        }
    }
}