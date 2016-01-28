/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件操作工具类
 * 实现文件的创建、删除、复制、压缩、解压以及目录的创建、删除、复制、压缩解压等功能
 * @author ThinkGem
 * @version 2013-06-21
 */
public class FileUtils extends org.apache.commons.io.FileUtils {

  private static Logger log = LoggerFactory.getLogger(FileUtils.class);

  /**
   * 复制单个文件，如果目标文件存在，则不覆盖
   * @param srcFileName 待复制的文件名
   * @param descFileName 目标文件名
   * @return 如果复制成功，则返回true，否则返回false
   */
  public static boolean copyFile(final String srcFileName, final String descFileName) {
    return FileUtils.copyFileCover(srcFileName, descFileName, false);
  }

  /**
   * 复制单个文件
   * @param srcFileName 待复制的文件名
   * @param descFileName 目标文件名
   * @param coverlay 如果目标文件已存在，是否覆盖
   * @return 如果复制成功，则返回true，否则返回false
   */
  public static boolean copyFileCover(final String srcFileName,
      final String descFileName, final boolean coverlay) {
    final File srcFile = new File(srcFileName);
    // 判断源文件是否存在
    if (!srcFile.exists()) {
      FileUtils.log.debug("复制文件失败，源文件 " + srcFileName + " 不存在!");
      return false;
    }
    // 判断源文件是否是合法的文件
    else if (!srcFile.isFile()) {
      FileUtils.log.debug("复制文件失败，" + srcFileName + " 不是一个文件!");
      return false;
    }
    final File descFile = new File(descFileName);
    // 判断目标文件是否存在
    if (descFile.exists()) {
      // 如果目标文件存在，并且允许覆盖
      if (coverlay) {
        FileUtils.log.debug("目标文件已存在，准备删除!");
        if (!FileUtils.delFile(descFileName)) {
          FileUtils.log.debug("删除目标文件 " + descFileName + " 失败!");
          return false;
        }
      } else {
        FileUtils.log.debug("复制文件失败，目标文件 " + descFileName + " 已存在!");
        return false;
      }
    } else {
      if (!descFile.getParentFile().exists()) {
        // 如果目标文件所在的目录不存在，则创建目录
        FileUtils.log.debug("目标文件所在的目录不存在，创建目录!");
        // 创建目标文件所在的目录
        if (!descFile.getParentFile().mkdirs()) {
          FileUtils.log.debug("创建目标文件所在的目录失败!");
          return false;
        }
      }
    }

    // 准备复制文件
    // 读取的位数
    int readByte = 0;
    InputStream ins = null;
    OutputStream outs = null;
    try {
      // 打开源文件
      ins = new FileInputStream(srcFile);
      // 打开目标文件的输出流
      outs = new FileOutputStream(descFile);
      final byte[] buf = new byte[1024];
      // 一次读取1024个字节，当readByte为-1时表示文件已经读取完毕
      while ((readByte = ins.read(buf)) != -1) {
        // 将读取的字节流写入到输出流
        outs.write(buf, 0, readByte);
      }
      FileUtils.log.debug("复制单个文件 " + srcFileName + " 到" + descFileName
          + "成功!");
      return true;
    } catch (final Exception e) {
      FileUtils.log.debug("复制文件失败：" + e.getMessage());
      return false;
    } finally {
      // 关闭输入输出流，首先关闭输出流，然后再关闭输入流
      if (outs != null) {
        try {
          outs.close();
        } catch (final IOException oute) {
          oute.printStackTrace();
        }
      }
      if (ins != null) {
        try {
          ins.close();
        } catch (final IOException ine) {
          ine.printStackTrace();
        }
      }
    }
  }

  /**
   * 复制整个目录的内容，如果目标目录存在，则不覆盖
   * @param srcDirName 源目录名
   * @param descDirName 目标目录名
   * @return 如果复制成功返回true，否则返回false
   */
  public static boolean copyDirectory(final String srcDirName, final String descDirName) {
    return FileUtils.copyDirectoryCover(srcDirName, descDirName,
        false);
  }

  /**
   * 复制整个目录的内容
   * @param srcDirName 源目录名
   * @param descDirName 目标目录名
   * @param coverlay 如果目标目录存在，是否覆盖
   * @return 如果复制成功返回true，否则返回false
   */
  public static boolean copyDirectoryCover(final String srcDirName,
      final String descDirName, final boolean coverlay) {
    final File srcDir = new File(srcDirName);
    // 判断源目录是否存在
    if (!srcDir.exists()) {
      FileUtils.log.debug("复制目录失败，源目录 " + srcDirName + " 不存在!");
      return false;
    }
    // 判断源目录是否是目录
    else if (!srcDir.isDirectory()) {
      FileUtils.log.debug("复制目录失败，" + srcDirName + " 不是一个目录!");
      return false;
    }
    // 如果目标文件夹名不以文件分隔符结尾，自动添加文件分隔符
    String descDirNames = descDirName;
    if (!descDirNames.endsWith(File.separator)) {
      descDirNames = descDirNames + File.separator;
    }
    final File descDir = new File(descDirNames);
    // 如果目标文件夹存在
    if (descDir.exists()) {
      if (coverlay) {
        // 允许覆盖目标目录
        FileUtils.log.debug("目标目录已存在，准备删除!");
        if (!FileUtils.delFile(descDirNames)) {
          FileUtils.log.debug("删除目录 " + descDirNames + " 失败!");
          return false;
        }
      } else {
        FileUtils.log.debug("目标目录复制失败，目标目录 " + descDirNames + " 已存在!");
        return false;
      }
    } else {
      // 创建目标目录
      FileUtils.log.debug("目标目录不存在，准备创建!");
      if (!descDir.mkdirs()) {
        FileUtils.log.debug("创建目标目录失败!");
        return false;
      }

    }

    boolean flag = true;
    // 列出源目录下的所有文件名和子目录名
    final File[] files = srcDir.listFiles();
    for (int i = 0; i < files.length; i++) {
      // 如果是一个单个文件，则直接复制
      if (files[i].isFile()) {
        flag = FileUtils.copyFile(files[i].getAbsolutePath(),
            descDirName + files[i].getName());
        // 如果拷贝文件失败，则退出循环
        if (!flag) {
          break;
        }
      }
      // 如果是子目录，则继续复制目录
      if (files[i].isDirectory()) {
        flag = FileUtils.copyDirectory(files[i]
            .getAbsolutePath(), descDirName + files[i].getName());
        // 如果拷贝目录失败，则退出循环
        if (!flag) {
          break;
        }
      }
    }

    if (!flag) {
      FileUtils.log.debug("复制目录 " + srcDirName + " 到 " + descDirName + " 失败!");
      return false;
    }
    FileUtils.log.debug("复制目录 " + srcDirName + " 到 " + descDirName + " 成功!");
    return true;

  }

  /**
   *
   * 删除文件，可以删除单个文件或文件夹
   *
   * @param fileName 被删除的文件名
   * @return 如果删除成功，则返回true，否是返回false
   */
  public static boolean delFile(final String fileName) {
    final File file = new File(fileName);
    if (!file.exists()) {
      FileUtils.log.debug(fileName + " 文件不存在!");
      return true;
    } else {
      if (file.isFile()) {
        return FileUtils.deleteFile(fileName);
      } else {
        return FileUtils.deleteDirectory(fileName);
      }
    }
  }

  /**
   *
   * 删除单个文件
   *
   * @param fileName 被删除的文件名
   * @return 如果删除成功，则返回true，否则返回false
   */
  public static boolean deleteFile(final String fileName) {
    final File file = new File(fileName);
    if (file.exists() && file.isFile()) {
      if (file.delete()) {
        FileUtils.log.debug("删除文件 " + fileName + " 成功!");
        return true;
      } else {
        FileUtils.log.debug("删除文件 " + fileName + " 失败!");
        return false;
      }
    } else {
      FileUtils.log.debug(fileName + " 文件不存在!");
      return true;
    }
  }

  /**
   *
   * 删除目录及目录下的文件
   *
   * @param dirName 被删除的目录所在的文件路径
   * @return 如果目录删除成功，则返回true，否则返回false
   */
  public static boolean deleteDirectory(final String dirName) {
    String dirNames = dirName;
    if (!dirNames.endsWith(File.separator)) {
      dirNames = dirNames + File.separator;
    }
    final File dirFile = new File(dirNames);
    if (!dirFile.exists() || !dirFile.isDirectory()) {
      FileUtils.log.debug(dirNames + " 目录不存在!");
      return true;
    }
    boolean flag = true;
    // 列出全部文件及子目录
    final File[] files = dirFile.listFiles();
    for (int i = 0; i < files.length; i++) {
      // 删除子文件
      if (files[i].isFile()) {
        flag = FileUtils.deleteFile(files[i].getAbsolutePath());
        // 如果删除文件失败，则退出循环
        if (!flag) {
          break;
        }
      }
      // 删除子目录
      else if (files[i].isDirectory()) {
        flag = FileUtils.deleteDirectory(files[i]
            .getAbsolutePath());
        // 如果删除子目录失败，则退出循环
        if (!flag) {
          break;
        }
      }
    }

    if (!flag) {
      FileUtils.log.debug("删除目录失败!");
      return false;
    }
    // 删除当前目录
    if (dirFile.delete()) {
      FileUtils.log.debug("删除目录 " + dirName + " 成功!");
      return true;
    } else {
      FileUtils.log.debug("删除目录 " + dirName + " 失败!");
      return false;
    }

  }

  /**
   * 创建单个文件
   * @param descFileName 文件名，包含路径
   * @return 如果创建成功，则返回true，否则返回false
   */
  public static boolean createFile(final String descFileName) {
    final File file = new File(descFileName);
    if (file.exists()) {
      FileUtils.log.debug("文件 " + descFileName + " 已存在!");
      return false;
    }
    if (descFileName.endsWith(File.separator)) {
      FileUtils.log.debug(descFileName + " 为目录，不能创建目录!");
      return false;
    }
    if (!file.getParentFile().exists()) {
      // 如果文件所在的目录不存在，则创建目录
      if (!file.getParentFile().mkdirs()) {
        FileUtils.log.debug("创建文件所在的目录失败!");
        return false;
      }
    }

    // 创建文件
    try {
      if (file.createNewFile()) {
        FileUtils.log.debug(descFileName + " 文件创建成功!");
        return true;
      } else {
        FileUtils.log.debug(descFileName + " 文件创建失败!");
        return false;
      }
    } catch (final Exception e) {
      e.printStackTrace();
      FileUtils.log.debug(descFileName + " 文件创建失败!");
      return false;
    }

  }

  /**
   * 创建目录
   * @param descDirName 目录名,包含路径
   * @return 如果创建成功，则返回true，否则返回false
   */
  public static boolean createDirectory(final String descDirName) {
    String descDirNames = descDirName;
    if (!descDirNames.endsWith(File.separator)) {
      descDirNames = descDirNames + File.separator;
    }
    final File descDir = new File(descDirNames);
    if (descDir.exists()) {
      FileUtils.log.debug("目录 " + descDirNames + " 已存在!");
      return false;
    }
    // 创建目录
    if (descDir.mkdirs()) {
      FileUtils.log.debug("目录 " + descDirNames + " 创建成功!");
      return true;
    } else {
      FileUtils.log.debug("目录 " + descDirNames + " 创建失败!");
      return false;
    }

  }

  /**
   * 写入文件
   * @param file 要写入的文件
   */
  public static void writeToFile(final String fileName, final String content, final boolean append) {
    try {
      org.apache.commons.io.FileUtils.write(new File(fileName), content, "utf-8", append);
      FileUtils.log.debug("文件 " + fileName + " 写入成功!");
    } catch (final IOException e) {
      FileUtils.log.debug("文件 " + fileName + " 写入失败! " + e.getMessage());
    }
  }

  /**
   * 写入文件
   * @param file 要写入的文件
   */
  public static void writeToFile(final String fileName, final String content, final String encoding, final boolean append) {
    try {
      org.apache.commons.io.FileUtils.write(new File(fileName), content, encoding, append);
      FileUtils.log.debug("文件 " + fileName + " 写入成功!");
    } catch (final IOException e) {
      FileUtils.log.debug("文件 " + fileName + " 写入失败! " + e.getMessage());
    }
  }

  /**
   * 修复路径，将 \\ 或 / 等替换为 File.separator
   * @param path
   * @return
   */
  public static String path(final String path){
    String p = org.apache.commons.lang3.StringUtils.replace(path, "\\", "/");
    p = org.apache.commons.lang3.StringUtils.join(org.apache.commons.lang3.StringUtils.split(p, "/"), "/");
    if (!org.apache.commons.lang3.StringUtils.startsWithAny(p, "/") && org.apache.commons.lang3.StringUtils.startsWithAny(path, "\\", "/")){
      p += "/";
    }
    if (!org.apache.commons.lang3.StringUtils.endsWithAny(p, "/") && org.apache.commons.lang3.StringUtils.endsWithAny(path, "\\", "/")){
      p = p + "/";
    }
    if (path != null && path.startsWith("/")){
      p = "/" + p; // linux下路径
    }
    return p;
  }

}
