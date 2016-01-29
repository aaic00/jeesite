/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.thinkgem.jeesite.modules.sys.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.shiro.session.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thinkgem.jeesite.common.config.Global;
import com.thinkgem.jeesite.common.persistence.Page;
import com.thinkgem.jeesite.common.security.Digests;
import com.thinkgem.jeesite.common.security.shiro.session.SessionDAO;
import com.thinkgem.jeesite.common.service.BaseService;
import com.thinkgem.jeesite.common.service.ServiceException;
import com.thinkgem.jeesite.common.utils.CacheUtils;
import com.thinkgem.jeesite.common.utils.Encodes;
import com.thinkgem.jeesite.common.utils.StringUtils;
import com.thinkgem.jeesite.common.web.Servlets;
import com.thinkgem.jeesite.modules.sys.dao.MenuDao;
import com.thinkgem.jeesite.modules.sys.dao.RoleDao;
import com.thinkgem.jeesite.modules.sys.dao.UserDao;
import com.thinkgem.jeesite.modules.sys.entity.Menu;
import com.thinkgem.jeesite.modules.sys.entity.Office;
import com.thinkgem.jeesite.modules.sys.entity.Role;
import com.thinkgem.jeesite.modules.sys.entity.User;
import com.thinkgem.jeesite.modules.sys.security.SystemAuthorizingRealm;
import com.thinkgem.jeesite.modules.sys.utils.LogUtils;
import com.thinkgem.jeesite.modules.sys.utils.UserUtils;

/**
 * 系统管理，安全相关实体的管理类,包括用户、角色、菜单.
 * @author ThinkGem
 * @version 2013-12-05
 */
@Service
@Transactional(readOnly = true)
public class SystemService extends BaseService implements InitializingBean {

  public static final String HASH_ALGORITHM = "SHA-1";
  public static final int HASH_INTERATIONS = 1024;
  public static final int SALT_SIZE = 8;

  @Autowired
  private UserDao userDao;
  @Autowired
  private RoleDao roleDao;
  @Autowired
  private MenuDao menuDao;
  @Autowired
  private SessionDAO sessionDao;
  @Autowired
  private SystemAuthorizingRealm systemRealm;

  public SessionDAO getSessionDao() {
    return this.sessionDao;
  }

  //-- User Service --//

  /**
   * 获取用户
   * @param id
   * @return
   */
  public User getUser(final String id) {
    return UserUtils.get(id);
  }

  /**
   * 根据登录名获取用户
   * @param loginName
   * @return
   */
  public User getUserByLoginName(final String loginName) {
    return UserUtils.getByLoginName(loginName);
  }

  public Page<User> findUser(final Page<User> page, final User user) {
    // 生成数据权限过滤条件（dsf为dataScopeFilter的简写，在xml中使用 ${sqlMap.dsf}调用权限SQL）
    user.getSqlMap().put("dsf", BaseService.dataScopeFilter(user.getCurrentUser(), "o", "a"));
    // 设置分页参数
    user.setPage(page);
    // 执行分页查询
    page.setList(this.userDao.findList(user));
    return page;
  }

  /**
   * 无分页查询人员列表
   * @param user
   * @return
   */
  public List<User> findUser(final User user){
    // 生成数据权限过滤条件（dsf为dataScopeFilter的简写，在xml中使用 ${sqlMap.dsf}调用权限SQL）
    user.getSqlMap().put("dsf", BaseService.dataScopeFilter(user.getCurrentUser(), "o", "a"));
    final List<User> list = this.userDao.findList(user);
    return list;
  }

  /**
   * 通过部门ID获取用户列表，仅返回用户id和name（树查询用户时用）
   * @param user
   * @return
   */
  @SuppressWarnings("unchecked")
  public List<User> findUserByOfficeId(final String officeId) {
    List<User> list = (List<User>)CacheUtils.get(UserUtils.USER_CACHE, UserUtils.USER_CACHE_LIST_BY_OFFICE_ID_ + officeId);
    if (list == null){
      final User user = new User();
      user.setOffice(new Office(officeId));
      list = this.userDao.findUserByOfficeId(user);
      CacheUtils.put(UserUtils.USER_CACHE, UserUtils.USER_CACHE_LIST_BY_OFFICE_ID_ + officeId, list);
    }
    return list;
  }

  @Transactional(readOnly = false)
  public void saveUser(final User user) {
    if (org.apache.commons.lang3.StringUtils.isBlank(user.getId())){
      user.preInsert();
      this.userDao.insert(user);
    }else{
      // 清除原用户机构用户缓存
      final User oldUser = this.userDao.get(user.getId());
      if (oldUser.getOffice() != null && oldUser.getOffice().getId() != null){
        CacheUtils.remove(UserUtils.USER_CACHE, UserUtils.USER_CACHE_LIST_BY_OFFICE_ID_ + oldUser.getOffice().getId());
      }
      // 更新用户数据
      user.preUpdate();
      this.userDao.update(user);
    }
    if (org.apache.commons.lang3.StringUtils.isNotBlank(user.getId())){
      // 更新用户与角色关联
      this.userDao.deleteUserRole(user);
      if (user.getRoleList() != null && user.getRoleList().size() > 0){
        this.userDao.insertUserRole(user);
      }else{
        throw new ServiceException(user.getLoginName() + "没有设置角色！");
      }
      // 清除用户缓存
      UserUtils.clearCache(user);
      //			// 清除权限缓存
      //			systemRealm.clearAllCachedAuthorizationInfo();
    }
  }

  @Transactional(readOnly = false)
  public void updateUserInfo(final User user) {
    user.preUpdate();
    this.userDao.updateUserInfo(user);
    // 清除用户缓存
    UserUtils.clearCache(user);
    //		// 清除权限缓存
    //		systemRealm.clearAllCachedAuthorizationInfo();
  }

  @Transactional(readOnly = false)
  public void deleteUser(final User user) {
    this.userDao.delete(user);
    // 清除用户缓存
    UserUtils.clearCache(user);
    //		// 清除权限缓存
    //		systemRealm.clearAllCachedAuthorizationInfo();
  }

  @Transactional(readOnly = false)
  public void updatePasswordById(final String id, final String loginName, final String newPassword) {
    final User user = new User(id);
    user.setPassword(SystemService.entryptPassword(newPassword));
    this.userDao.updatePasswordById(user);
    // 清除用户缓存
    user.setLoginName(loginName);
    UserUtils.clearCache(user);
    //		// 清除权限缓存
    //		systemRealm.clearAllCachedAuthorizationInfo();
  }

  @Transactional(readOnly = false)
  public void updateUserLoginInfo(final User user) {
    // 保存上次登录信息
    user.setOldLoginIp(user.getLoginIp());
    user.setOldLoginDate(user.getLoginDate());
    // 更新本次登录信息
    user.setLoginIp(StringUtils.getRemoteAddr(Servlets.getRequest()));
    user.setLoginDate(new Date());
    this.userDao.updateLoginInfo(user);
  }

  /**
   * 生成安全的密码，生成随机的16位salt并经过1024次 sha-1 hash
   */
  public static String entryptPassword(final String plainPassword) {
    final String plain = Encodes.unescapeHtml(plainPassword);
    final byte[] salt = Digests.generateSalt(SystemService.SALT_SIZE);
    final byte[] hashPassword = Digests.sha1(plain.getBytes(), salt, SystemService.HASH_INTERATIONS);
    return Encodes.encodeHex(salt)+Encodes.encodeHex(hashPassword);
  }

  /**
   * 验证密码
   * @param plainPassword 明文密码
   * @param password 密文密码
   * @return 验证成功返回true
   */
  public static boolean validatePassword(final String plainPassword, final String password) {
    final String plain = Encodes.unescapeHtml(plainPassword);
    final byte[] salt = Encodes.decodeHex(password.substring(0,16));
    final byte[] hashPassword = Digests.sha1(plain.getBytes(), salt, SystemService.HASH_INTERATIONS);
    return password.equals(Encodes.encodeHex(salt)+Encodes.encodeHex(hashPassword));
  }

  /**
   * 获得活动会话
   * @return
   */
  public Collection<Session> getActiveSessions(){
    return this.sessionDao.getActiveSessions(false);
  }

  //-- Role Service --//

  public Role getRole(final String id) {
    return this.roleDao.get(id);
  }

  public Role getRoleByName(final String name) {
    final Role r = new Role();
    r.setName(name);
    return this.roleDao.getByName(r);
  }

  public Role getRoleByEnname(final String enname) {
    final Role r = new Role();
    r.setEnname(enname);
    return this.roleDao.getByEnname(r);
  }

  public List<Role> findRole(final Role role){
    return this.roleDao.findList(role);
  }

  public List<Role> findAllRole(){
    return UserUtils.getRoleList();
  }

  @Transactional(readOnly = false)
  public void saveRole(final Role role) {
    if (org.apache.commons.lang3.StringUtils.isBlank(role.getId())){
      role.preInsert();
      this.roleDao.insert(role);
    }else{
      role.preUpdate();
      this.roleDao.update(role);
    }
    // 更新角色与菜单关联
    this.roleDao.deleteRoleMenu(role);
    if (role.getMenuList().size() > 0){
      this.roleDao.insertRoleMenu(role);
    }
    // 更新角色与部门关联
    this.roleDao.deleteRoleOffice(role);
    if (role.getOfficeList().size() > 0){
      this.roleDao.insertRoleOffice(role);
    }
    // 清除用户角色缓存
    UserUtils.removeCache(UserUtils.CACHE_ROLE_LIST);
    //		// 清除权限缓存
    //		systemRealm.clearAllCachedAuthorizationInfo();
  }

  @Transactional(readOnly = false)
  public void deleteRole(final Role role) {
    this.roleDao.delete(role);
    // 清除用户角色缓存
    UserUtils.removeCache(UserUtils.CACHE_ROLE_LIST);
    //		// 清除权限缓存
    //		systemRealm.clearAllCachedAuthorizationInfo();
  }

  @Transactional(readOnly = false)
  public Boolean outUserInRole(final Role role, final User user) {
    final List<Role> roles = user.getRoleList();
    for (final Role e : roles){
      if (e.getId().equals(role.getId())){
        roles.remove(e);
        this.saveUser(user);
        return true;
      }
    }
    return false;
  }

  @Transactional(readOnly = false)
  public User assignUserToRole(final Role role, final User user) {
    if (user == null){
      return null;
    }
    final List<String> roleIds = user.getRoleIdList();
    if (roleIds.contains(role.getId())) {
      return null;
    }
    user.getRoleList().add(role);
    this.saveUser(user);
    return user;
  }

  //-- Menu Service --//

  public Menu getMenu(final String id) {
    return this.menuDao.get(id);
  }

  public List<Menu> findAllMenu(){
    return UserUtils.getMenuList();
  }

  @Transactional(readOnly = false)
  public void saveMenu(final Menu menu) {

    // 获取父节点实体
    menu.setParent(this.getMenu(menu.getParent().getId()));

    // 获取修改前的parentIds，用于更新子节点的parentIds
    final String oldParentIds = menu.getParentIds();

    // 设置新的父节点串
    menu.setParentIds(menu.getParent().getParentIds()+menu.getParent().getId()+",");

    // 保存或更新实体
    if (org.apache.commons.lang3.StringUtils.isBlank(menu.getId())){
      menu.preInsert();
      this.menuDao.insert(menu);
    }else{
      menu.preUpdate();
      this.menuDao.update(menu);
    }

    // 更新子节点 parentIds
    final Menu m = new Menu();
    m.setParentIds("%,"+menu.getId()+",%");
    final List<Menu> list = this.menuDao.findByParentIdsLike(m);
    for (final Menu e : list){
      e.setParentIds(e.getParentIds().replace(oldParentIds, menu.getParentIds()));
      this.menuDao.updateParentIds(e);
    }
    // 清除用户菜单缓存
    UserUtils.removeCache(UserUtils.CACHE_MENU_LIST);
    //		// 清除权限缓存
    //		systemRealm.clearAllCachedAuthorizationInfo();
    // 清除日志相关缓存
    CacheUtils.remove(LogUtils.CACHE_MENU_NAME_PATH_MAP);
  }

  @Transactional(readOnly = false)
  public void updateMenuSort(final Menu menu) {
    this.menuDao.updateSort(menu);
    // 清除用户菜单缓存
    UserUtils.removeCache(UserUtils.CACHE_MENU_LIST);
    //		// 清除权限缓存
    //		systemRealm.clearAllCachedAuthorizationInfo();
    // 清除日志相关缓存
    CacheUtils.remove(LogUtils.CACHE_MENU_NAME_PATH_MAP);
  }

  @Transactional(readOnly = false)
  public void deleteMenu(final Menu menu) {
    this.menuDao.delete(menu);
    // 清除用户菜单缓存
    UserUtils.removeCache(UserUtils.CACHE_MENU_LIST);
    //		// 清除权限缓存
    //		systemRealm.clearAllCachedAuthorizationInfo();
    // 清除日志相关缓存
    CacheUtils.remove(LogUtils.CACHE_MENU_NAME_PATH_MAP);
  }

  /**
   * 获取Key加载信息
   */
  public static boolean printKeyLoadMessage(){
    final StringBuilder sb = new StringBuilder();
    sb.append("\r\n======================================================================\r\n");
    sb.append("\r\n    欢迎使用 "+Global.getConfig("productName")+"  - Powered By http://jeesite.com\r\n");
    sb.append("\r\n======================================================================\r\n");
    System.out.println(sb.toString());
    return true;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
  }

}
