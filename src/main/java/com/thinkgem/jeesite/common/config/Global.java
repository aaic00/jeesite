/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.common.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.DefaultResourceLoader;

import com.google.common.collect.Maps;
import com.thinkgem.jeesite.common.utils.PropertiesLoader;

/**
 * 全局配置类
 * @author ThinkGem
 * @version 2014-06-25
 */
public class Global {

  /**
   * 当前对象实例
   */
  private static Global global = new Global();

  /**
   * 保存全局属性值
   */
  private static Map<String, String> map = Maps.newHashMap();

  /**
   * 属性文件加载对象
   */
  private static PropertiesLoader loader = new PropertiesLoader("jeesite.properties");

  /**
   * 显示/隐藏
   */
  public static final String SHOW = "1";
  public static final String HIDE = "0";

  /**
   * 是/否
   */
  public static final String YES = "1";
  public static final String NO = "0";

  /**
   * 对/错
   */
  public static final String TRUE = "true";
  public static final String FALSE = "false";

  /**
   * 上传文件基础虚拟路径
   */
  public static final String USERFILES_BASE_URL = "/userfiles/";

  /**
   * 获取当前对象实例
   */
  public static Global getInstance() {
    return Global.global;
  }

  /**
   * 获取配置
   * @see ${fns:getConfig('adminPath')}
   */
  public static String getConfig(final String key) {
    String value = Global.map.get(key);
    if (value == null){
      value = Global.loader.getProperty(key);
      Global.map.put(key, value != null ? value : org.apache.commons.lang3.StringUtils.EMPTY);
    }
    return value;
  }

  /**
   * 获取管理端根路径
   */
  public static String getAdminPath() {
    return Global.getConfig("adminPath");
  }

  /**
   * 获取前端根路径
   */
  public static String getFrontPath() {
    return Global.getConfig("frontPath");
  }

  /**
   * 获取URL后缀
   */
  public static String getUrlSuffix() {
    return Global.getConfig("urlSuffix");
  }

  /**
   * 是否是演示模式，演示模式下不能修改用户、角色、密码、菜单、授权
   */
  public static Boolean isDemoMode() {
    final String dm = Global.getConfig("demoMode");
    return "true".equals(dm) || "1".equals(dm);
  }

  /**
   * 在修改系统用户和角色时是否同步到Activiti
   */
  public static Boolean isSynActivitiIndetity() {
    final String dm = Global.getConfig("activiti.isSynActivitiIndetity");
    return "true".equals(dm) || "1".equals(dm);
  }

  /**
   * 页面获取常量
   * @see ${fns:getConst('YES')}
   */
  public static Object getConst(final String field) {
    try {
      return Global.class.getField(field).get(null);
    } catch (final Exception e) {
      // 异常代表无配置，这里什么也不做
    }
    return null;
  }

  /**
   * 获取上传文件的根目录
   * @return
   */
  public static String getUserfilesBaseDir() {
    String dir = Global.getConfig("userfiles.basedir");

    if(!dir.endsWith("/")) {
      dir += "/";
    }
    //		System.out.println("userfiles.basedir: " + dir);
    return dir;
  }

  /**
   * 获取工程路径
   * @return
   */
  public static String getProjectPath(){
    // 如果配置了工程路径，则直接返回，否则自动获取。
    String projectPath = Global.getConfig("projectPath");
    if (org.apache.commons.lang3.StringUtils.isNotBlank(projectPath)){
      return projectPath;
    }
    try {
      File file = new DefaultResourceLoader().getResource("").getFile();
      if (file != null){
        while(true){
          final File f = new File(file.getPath() + File.separator + "src" + File.separator + "main");
          if (f == null || f.exists()){
            break;
          }
          if (file.getParentFile() != null){
            file = file.getParentFile();
          }else{
            break;
          }
        }
        projectPath = file.toString();
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
    return projectPath;
  }

}
