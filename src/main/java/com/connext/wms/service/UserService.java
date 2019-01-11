package com.connext.wms.service;

import com.connext.wms.entity.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface UserService {
    //注册
    void register(User user);
    //注册验证
    public int checkRegister(@Param("telephone") String telephone);
    //登陆
    public User login(Map<String, String> map);
    //删除用户
    void delete(Integer id);
    //修改用户信息
    void updateByPrimaryKey(User user);
    //用户列表
    List<User> queryAll();
    //修改权限
    void modifyRole(User user);
    //忘记密码
    void forget(User user);
}
