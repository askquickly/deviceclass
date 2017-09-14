package org.askquickly.utils
import android.os.Environment
import android.os.StatFs
object DiskUtils {
  private val MEGA_BYTE:Long = 1048576
  fun totalSpace(external:Boolean):Int {
    val statFs = getStats(external)
    val total = ((statFs.getBlockCount() as Long) * (statFs.getBlockSize() as Long)) / MEGA_BYTE
    return total.toInt()
  }
  fun freeSpace(external:Boolean):Int {
    val statFs = getStats(external)
    val availableBlocks = statFs.getAvailableBlocks()
    val blockSize = statFs.getBlockSize()
    val freeBytes = availableBlocks * blockSize
    return (freeBytes / MEGA_BYTE).toInt()
  }
  fun busySpace(external:Boolean):Int {
    val statFs = getStats(external)
    val total = (statFs.getBlockCount() * statFs.getBlockSize())
    val free = (statFs.getAvailableBlocks() * statFs.getBlockSize())
    return ((total - free) / MEGA_BYTE).toInt()
  }
  private fun getStats(external:Boolean):StatFs {
    val path:String
    if (external)
    {
      path = Environment.getExternalStorageDirectory().getAbsolutePath()
    }
    else
    {
      path = Environment.getRootDirectory().getAbsolutePath()
    }
    return StatFs(path)
  }
}
