package deviceinfo;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.annotation.TargetApi;
import android.app.ActivityManager;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BA.*;
import anywheresoftware.b4a.BA.ActivityObject;


@Author("Github: Fb Device Year Class, Wrapped by: Jamie John")
@Version(1.0f)
@ShortName("deviceinfo")
@ActivityObject
public class DeviceInfo {
	private BA ba;
	
    // Year definitions
    private static final int CLASS_UNKNOWN = -1;
    private static final int CLASS_2008 = 2008;
    private static final int CLASS_2009 = 2009;
    private static final int CLASS_2010 = 2010;
    private static final int CLASS_2011 = 2011;
    private static final int CLASS_2012 = 2012;
    private static final int CLASS_2013 = 2013;
    private static final int CLASS_2014 = 2014;
    private static final int CLASS_2015 = 2015;
    private static final int CLASS_2016 = 2016;
    private static final int CLASS_2017 = 2017;

    private static final String CLASS_LOW = "LOW";
    private static final String CLASS_MEDIUM = "MEDIUM";
    private static final String CLASS_HIGH = "HIGH";

    private static final long MB = 1024 * 1024;
    private static final int MHZ_IN_KHZ = 1000;

    private volatile static Integer mYearCategory;
    private volatile static String mClassCategory;

/**
     * The default return value of any method in this class when an
     * error occurs or when processing fails (Currently set to -1). Use this to check if
     * the information about the device in question was successfully obtained.
     */
    static final int DEVICEINFO_UNKNOWN = -1;
    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (!Character.isDigit(path.charAt(i))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    /**
     * Reads the number of CPU cores from {@code /sys/devices/system/cpu/}.
     *
     * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
     */
    public static int getNumberOfCPUCores() {
        int cores;
        try {
            cores = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
        } catch (SecurityException | NullPointerException e) {
            cores = DEVICEINFO_UNKNOWN;
        }
        return cores;
    }
	 
    /**
     * Method for reading the clock speed of a CPU core on the device. Will read from either
     * {@code /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq} or {@code /proc/cpuinfo}.
     *
     * @return Clock speed of a core on the device, or -1 in the event of an error.
     */
    public static int getCPUMaxFreqKHz() {
        int maxFreq = DEVICEINFO_UNKNOWN;
        try {
            for (int i = 0; i < getNumberOfCPUCores(); i++) {
                String filename = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
                File cpuInfoMaxFreqFile = new File(filename);
                if (cpuInfoMaxFreqFile.exists()) {
                    byte[] buffer = new byte[128];
                    FileInputStream stream = new FileInputStream(cpuInfoMaxFreqFile);
                    try {
                        stream.read(buffer);
                        int endIndex = 0;
                        //Trim the first number out of the byte buffer.
                        while (Character.isDigit(buffer[endIndex]) && endIndex < buffer.length) {
                            endIndex++;
                        }
                        String str = new String(buffer, 0, endIndex);
                        Integer freqBound = Integer.parseInt(str);
                        if (freqBound > maxFreq) {
                            maxFreq = freqBound;
                        }
                    } catch (NumberFormatException e) {
                        //Fall through and use /proc/cpuinfo.
                    } finally {
                        stream.close();
                    }
                }
            }
            if (maxFreq == DEVICEINFO_UNKNOWN) {
                FileInputStream stream = new FileInputStream("/proc/cpuinfo");
                try {
                    int freqBound = parseFileForValue("cpu MHz", stream);
                    freqBound *= 1000; //MHz -> kHz
                    if (freqBound > maxFreq) maxFreq = freqBound;
                } finally {
                    stream.close();
                }
            }
        } catch (IOException e) {
            maxFreq = DEVICEINFO_UNKNOWN; //Fall through and return unknown.
        }
        return maxFreq;
    }

    /**
     * Calculates the total RAM of the device through Android API or /proc/meminfo.
     *
     * @param c - Context object for current running activity.
     * @return Total RAM that the device has, or DEVICEINFO_UNKNOWN = -1 in the event of an error.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static long getTotalMemory(Context c) {
        // memInfo.totalMem not supported in pre-Jelly Bean APIs.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            am.getMemoryInfo(memInfo);
            return memInfo.totalMem;
        } else {
            long totalMem = DEVICEINFO_UNKNOWN;
            try {
                FileInputStream stream = new FileInputStream("/proc/meminfo");
                try {
                    totalMem = parseFileForValue("MemTotal", stream);
                    totalMem *= 1024;
                } finally {
                    stream.close();
                }
            } catch (IOException ignored) {
            }
            return totalMem;
        }
    }
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static long getMemThreshold(Context c) {
        // memInfo.totalMem not supported in pre-Jelly Bean APIs.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            am.getMemoryInfo(memInfo);
            return memInfo.threshold;
        } else {
            long totalMem = DEVICEINFO_UNKNOWN;
            try {
                FileInputStream stream = new FileInputStream("/proc/meminfo");
                try {
                    totalMem = parseFileForValue("MemTotal", stream);
                    totalMem *= 1024;
                } finally {
                    stream.close();
                }
            } catch (IOException ignored) {
            }
            return totalMem;
        }
    }
    /**
     * Helper method for reading values from system files, using a minimised buffer.
     *
     * @param textToMatch - Text in the system files to read for.
     * @param stream      - FileInputStream of the system file being read from.
     * @return A numerical value following textToMatch in specified the system file.
     * -1 in the event of a failure.
     */
    private static int parseFileForValue(String textToMatch, FileInputStream stream) {
        byte[] buffer = new byte[1024];
        try {
            int length = stream.read(buffer);
            for (int i = 0; i < length; i++) {
                if (buffer[i] == '\n' || i == 0) {
                    if (buffer[i] == '\n') i++;
                    for (int j = i; j < length; j++) {
                        int textIndex = j - i;
                        //Text doesn't match query at some point.
                        if (buffer[j] != textToMatch.charAt(textIndex)) {
                            break;
                        }
                        //Text matches query here.
                        if (textIndex == textToMatch.length() - 1) {
                            return extractValue(buffer, j);
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            //Ignore any exceptions and fall through to return unknown value.
        }
        return DEVICEINFO_UNKNOWN;
    }

    /**
     * Helper method used by {@link #parseFileForValue(String, FileInputStream) parseFileForValue}. Parses
     * the next available number after the match in the file being read and returns it as an integer.
     *
     * @param index - The index in the buffer array to begin looking.
     * @return The next number on that line in the buffer, returned as an int. Returns
     * DEVICEINFO_UNKNOWN = -1 in the event that no more numbers exist on the same line.
     */
    private static int extractValue(byte[] buffer, int index) {
        while (index < buffer.length && buffer[index] != '\n') {
            if (Character.isDigit(buffer[index])) {
                int start = index;
                index++;
                while (index < buffer.length && Character.isDigit(buffer[index])) {
                    index++;
                }
                String str = new String(buffer, start, index - start);
                return Integer.parseInt(str);
            }
            index++;
        }
        return DEVICEINFO_UNKNOWN;
    }

    public static String getScreenSize(Context context) {
        int screenSize = context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        String result;
        switch (screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                result = "XLarge";
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                result = "Large";
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                result = "Normal";
                break;
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                result = "Small";
                break;
            default:
                result = "neither XLarge, large, normal or small";
        }
        //return "Screen size: " + result;
		return result;
    }

    public static String getDeviceDensity(Context context) {
        String result = "";
        float scale = context.getResources().getDisplayMetrics().density;
        if (scale < 1.0f) {
            result += "ldpi";
        } else if (scale < 1.5f) {
            result += "mdpi";
        } else if (scale < 2.0f) {
            result += "hdpi";
        } else if (scale < 3.0f) {
            result += "xhdpi";
        } else if (scale < 4.0f) {
            result += "xxhdpi";
        } else {
            result += "xxxhdpi";
        }
        return result;
    }

    public static String getAndroidVersionName() {

        switch (Build.VERSION.SDK_INT) {

            case Build.VERSION_CODES.BASE:
            case Build.VERSION_CODES.BASE_1_1:
                return "Android version: Base " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.CUPCAKE:
                return "Android version: Cupcake " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.DONUT:
                return "Android version: Donut " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.ECLAIR:
            case Build.VERSION_CODES.ECLAIR_0_1:
            case Build.VERSION_CODES.ECLAIR_MR1:
                return "Android version: Eclair " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.FROYO:
                return "Android version: Froyo " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.GINGERBREAD:
            case Build.VERSION_CODES.GINGERBREAD_MR1:
                return "Android version: Gingerbread " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.HONEYCOMB:
            case Build.VERSION_CODES.HONEYCOMB_MR1:
            case Build.VERSION_CODES.HONEYCOMB_MR2:
                return "Android version: Honeycomb " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
                return "Android version: Ice Cream Sandwich " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.JELLY_BEAN:
            case Build.VERSION_CODES.JELLY_BEAN_MR1:
            case Build.VERSION_CODES.JELLY_BEAN_MR2:
                return "Android version: Jelly Bean " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.KITKAT:
                return "Android version: KitKat " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.LOLLIPOP:
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                return "Android version: Lollipop " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.M:
                return "Android version: Marshmallow " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.N:
            case Build.VERSION_CODES.N_MR1:
                return "Android version: Nougat " + Build.VERSION.RELEASE;
            case Build.VERSION_CODES.CUR_DEVELOPMENT:
                return "Android version: Development " + Build.VERSION.RELEASE;
        }
        return "Android version unknown SDK number: " + Build.VERSION.SDK_INT + " " + Build.VERSION.RELEASE;
    }
 public static String getAndroidVersionNumber() {

        switch (Build.VERSION.SDK_INT) {

            case Build.VERSION_CODES.BASE:
            case Build.VERSION_CODES.BASE_1_1:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.CUPCAKE:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.DONUT:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.ECLAIR:
            case Build.VERSION_CODES.ECLAIR_0_1:
            case Build.VERSION_CODES.ECLAIR_MR1:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.FROYO:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.GINGERBREAD:
            case Build.VERSION_CODES.GINGERBREAD_MR1:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.HONEYCOMB:
            case Build.VERSION_CODES.HONEYCOMB_MR1:
            case Build.VERSION_CODES.HONEYCOMB_MR2:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH:
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.JELLY_BEAN:
            case Build.VERSION_CODES.JELLY_BEAN_MR1:
            case Build.VERSION_CODES.JELLY_BEAN_MR2:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.KITKAT:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.LOLLIPOP:
            case Build.VERSION_CODES.LOLLIPOP_MR1:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.M:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.N:
            case Build.VERSION_CODES.N_MR1:
                return Build.VERSION.RELEASE;
            case Build.VERSION_CODES.CUR_DEVELOPMENT:
                return Build.VERSION.RELEASE;
        }
        return "Android version unknown SDK number: " + Build.VERSION.SDK_INT + " " + Build.VERSION.RELEASE;
    }
	//Object activityClass
    public static String getScreenResolution(BA ba) {
        int measuredWidth;
        int measuredHeight;
        WindowManager w = ba.activity.getWindowManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            w.getDefaultDisplay().getSize(size);
            measuredWidth = size.x;
            measuredHeight = size.y;
        } else {
            Display d = w.getDefaultDisplay();
            measuredWidth = d.getWidth();
            measuredHeight = d.getHeight();
        }

        return "(" + measuredWidth + "x" + measuredHeight + ")px";
    }
//

    public static String getScreenResolutionInDP(BA ba) {
        int measuredWidth;
        int measuredHeight;
        WindowManager w = ba.activity.getWindowManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            w.getDefaultDisplay().getSize(size);
            measuredWidth = size.x;
            measuredHeight = size.y;
        } else {
            Display d = w.getDefaultDisplay();
            measuredWidth = d.getWidth();
            measuredHeight = d.getHeight();
        }

        return "(" + convertPixelsToDp(measuredWidth, ba.activity) + "x" + convertPixelsToDp(measuredHeight, ba.activity) + ")dp";
    }

    private static int convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (px / (metrics.densityDpi / 160f));
    }
    /**
     * Entry Point of DeviceTestingCategory. Extracts DeviceYearClass variable with memoizing.
     * Example usage:
     * <p/>
     * <pre>
     *   int yearClass = YearClass.getYearClass(context);
     * </pre>
     */
	 public String DetectYearClass(Context c) {
		 String m = "";
		int s = DeviceInfo.getYear(c);
        if (s == CLASS_2008) m = CLASS_LOW;
		if (s == CLASS_2009) m = CLASS_LOW;
		if (s == CLASS_2010) m = CLASS_LOW;
		if (s == CLASS_2010) m = CLASS_LOW;
		if (s == CLASS_2011) m = CLASS_LOW;
		if (s == CLASS_2012) m = CLASS_MEDIUM;
		if (s == CLASS_2013) m = CLASS_MEDIUM;
		if (s == CLASS_2014) m = CLASS_MEDIUM;
		if (s == CLASS_2015) m = CLASS_HIGH;
		if (s == CLASS_2016) m = CLASS_HIGH;
		if (s == CLASS_2017) m = CLASS_HIGH;
		return m;
    }
    public static int getYear(Context c) {
        if (mYearCategory == null) {
            synchronized (DeviceInfo.class) {
                if (mYearCategory == null) {
                    mYearCategory = categorizeByYear(c);
                }
            }
        }
        return mYearCategory;
    }

    private static void conditionallyAdd(ArrayList<Integer> list, int value) {
        if (value != CLASS_UNKNOWN) {
            list.add(value);
        }
    }

    /**
     * Calculates the "best-in-class year" of the device. This represents the top-end or flagship
     * devices of that year, not the actual release year of the phone. For example, the Galaxy Duos
     * S was released in 2012, but its specs are very similar to the Galaxy S that was released in
     * 2010 as a then top-of-the-line phone, so it is a 2010 device.
     *
     * @return The year when this device would have been considered top-of-the-line.
     */
    private static int categorizeByYear(Context c) {
        /* Log.v(TAG, "getClockSpeedYear(): " + getClockSpeedYear());
        Log.v(TAG, "getNumCoresYear(): " + getNumCoresYear());
        Log.v(TAG, "getRamYear(): " + getRamYear(c)); */

        ArrayList<Integer> componentYears = new ArrayList<>();
        conditionallyAdd(componentYears, getClockSpeedYear());
        conditionallyAdd(componentYears, getRamYear(c));

        if (componentYears.isEmpty()) {
            // GKB: Fallback to using number of cores only if nothing else is available.
            conditionallyAdd(componentYears, getNumCoresYear());
        }

        // GKB: Simplified derivation of overall device year by taking average of individual years.
        int avgYear = 0;
        for (Integer year : componentYears) {
            if (year > CLASS_UNKNOWN) {
                avgYear += year;
            }
        }

        return (avgYear > 0) ?
                (avgYear / componentYears.size()) : // GKB: Implies floor() function via division operation.
                CLASS_UNKNOWN;
    }

    /**
     * Calculates the year class by the number of processor cores the phone has.
     *
     * @return the year in which top-of-the-line phones had the same number of processors as this phone.
     */
    private static int getNumCoresYear() {
        int cores = DeviceInfo.getNumberOfCPUCores();
        if (cores < 1) return CLASS_UNKNOWN;
        if (cores == 1) return CLASS_2008;
        if (cores <= 3) return CLASS_2015;
        if (cores <= 4) return CLASS_2016;
        return CLASS_2017; // E.g. Octa core devices
    }

    /**
     * Calculates the year class by the clock speed of the cores in the phone.
     *
     * @return the year in which top-of-the-line phones had the same clock speed.
     */
    private static int getClockSpeedYear() {
        long clockSpeedKHz = DeviceInfo.getCPUMaxFreqKHz();
        if (clockSpeedKHz == DeviceInfo.DEVICEINFO_UNKNOWN) return CLASS_UNKNOWN;

        // GKB: Clock speed dropped when core count was upped to 8 so factor this into the calc.
        int cores = DeviceInfo.getNumberOfCPUCores();
        if (cores < 8) {
            // These cut-offs include 20MHz of "slop" because my "1.5GHz" Galaxy S3 reports
            // its clock speed as 1512000. So we add a little slop to keep things nominally correct.
            if (clockSpeedKHz <= 528 * MHZ_IN_KHZ) return CLASS_2008;
            if (clockSpeedKHz <= 620 * MHZ_IN_KHZ) return CLASS_2009;
            if (clockSpeedKHz <= 1020 * MHZ_IN_KHZ) return CLASS_2010;
            if (clockSpeedKHz <= 1220 * MHZ_IN_KHZ) return CLASS_2011;
            if (clockSpeedKHz <= 1520 * MHZ_IN_KHZ) return CLASS_2012;
            if (clockSpeedKHz <= 2020 * MHZ_IN_KHZ) return CLASS_2014;
            if (clockSpeedKHz <= 2200 * MHZ_IN_KHZ) return CLASS_2016;
            return CLASS_2017;
        } else {
            if (clockSpeedKHz <= 1520 * MHZ_IN_KHZ) return CLASS_2015;
            return CLASS_2016;
        }
    }

    public static String getClockSpeedValue() {
        long clockSpeedKHz = DeviceInfo.getCPUMaxFreqKHz();
        return "Clock speed: " + clockSpeedKHz / MHZ_IN_KHZ + " Mhz";
    }

    //TODO add 64 bits check
    public static boolean is64bitsCPU() {
        return Build.SUPPORTED_64_BIT_ABIS.length > 0;
    }

    /**
     * Calculates the year class by the amount of RAM the phone has.
     *
     * @return the year in which top-of-the-line phones had the same amount of RAM as this phone.
     */
    private static int getRamYear(Context c) {
        long totalRam = DeviceInfo.getTotalMemory(c);
        if (totalRam <= 0) return CLASS_UNKNOWN;
        if (totalRam <= 192 * MB) return CLASS_2008;
        if (totalRam <= 290 * MB) return CLASS_2009;
        if (totalRam <= 512 * MB) return CLASS_2010;
        if (totalRam <= 1024 * MB) return CLASS_2011;
        if (totalRam <= 1536 * MB) return CLASS_2012;
        if (totalRam <= 2048 * MB) return CLASS_2015;
        if (totalRam <= 4096 * MB) return CLASS_2016;
        return CLASS_2017;
    }

    public static String getRamValue(Context c) {
        long totalRam = DeviceInfo.getTotalMemory(c);
       return "Total Ram: " + totalRam / MB + " Mb";
	   
    }
	
}
