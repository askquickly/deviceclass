package org.askquickly.utils;
// import B4A*
import android.os.Environment;
import android.os.StatFs;

public class DiskUtils {
  private static final long MEGA_BYTE = 1048576;

  public static int totalSpace(boolean external)
  {
    StatFs statFs = getStats(external);
    long total = (((long) statFs.getBlockCount()) * ((long) statFs.getBlockSize())) / MEGA_BYTE;
    return (int) total;
  }

  public static int freeSpace(boolean external)
  {
    StatFs statFs = getStats(external);
    long availableBlocks = statFs.getAvailableBlocks();
    long blockSize = statFs.getBlockSize();
    long freeBytes = availableBlocks * blockSize;

    return (int) (freeBytes / MEGA_BYTE);
  }

  public static int busySpace(boolean external)
  {
    StatFs statFs = getStats(external);
    long total = (statFs.getBlockCount() * statFs.getBlockSize());
    long free  = (statFs.getAvailableBlocks() * statFs.getBlockSize());

    return (int) ((total - free) / MEGA_BYTE);
  }

  private static StatFs getStats(boolean external){
    String path;

    if (external){
      path = Environment.getExternalStorageDirectory().getAbsolutePath();
    }
    else{
      path = Environment.getRootDirectory().getAbsolutePath();
    }

    return new StatFs(path);
  }
}
