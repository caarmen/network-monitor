package sun.awt.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.util.Comparator;

/**
 * Created by calvarez on 25/04/15.
 */
public class DataTransferer {
    private static DataTransferer me = null;
    public static Class byteArrayClass;
    public static Class charArrayClass;

    public static boolean isFlavorCharsetTextType(DataFlavor dataFlavor) {
        return false;
    }

    public static String getTextCharset(DataFlavor dataFlavor) {
        return "";
    }

    public static DataTransferer getInstance() {
        if(me == null)
            me = new DataTransferer();
        return me;
    }

    public String getDefaultUnicodeEncoding() {
        return "UTF-8";
    }

    public static boolean doesSubtypeSupportCharset(DataFlavor dataFlavor) {
        return false;
    }

    public static String canonicalName(String charset) {
        return null;
    }

    public static boolean isFlavorNoncharsetTextType(DataFlavor dataFlavor) {
        return false;
    }

    public static boolean isRemote(Class representationClass) {
        return false;
    }

    public static class DataFlavorComparator implements Comparator {
        @Override
        public int compare(Object o, Object t1) {
            return 0;
        }
    }
}
