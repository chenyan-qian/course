package com.course.controller;

import com.course.entity.User;
import com.course.service.UserService;
import com.course.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 登录
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> map = new HashMap<>();
        User one = userService.lambdaQuery()
                .eq(User::getUsername, user.getUsername())
                .one();

        if (one == null || !one.getPassword().equals(user.getPassword())) {
            map.put("code", 401);
            map.put("msg", "账号或密码错误");
            return map;
        }

        String token = JwtUtil.generateToken(one.getId(), one.getUsername());
        // 不返回密码
        one.setPassword(null);
        map.put("code", 200);
        map.put("token", token);
        map.put("user", one);
        return map;
    }

    // 注册
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> map = new HashMap<>();
        // 检查用户名是否已存在
        long count = userService.lambdaQuery()
                .eq(User::getUsername, user.getUsername())
                .count();
        if (count > 0) {
            map.put("code", 400);
            map.put("msg", "用户名已存在");
            return map;
        }
        boolean ok = userService.save(user);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "注册成功" : "注册失败");
        return map;
    }

    // 修改密码（需登录）
    @PutMapping("/password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body,
                                               HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null || newPassword.isEmpty()) {
            map.put("code", 400);
            map.put("msg", "旧密码和新密码不能为空");
            return map;
        }

        User user = userService.getById(userId);
        if (!user.getPassword().equals(oldPassword)) {
            map.put("code", 400);
            map.put("msg", "旧密码不正确");
            return map;
        }

        user.setPassword(newPassword);
        boolean ok = userService.updateById(user);
        map.put("code", ok ? 200 : 500);
        map.put("msg", ok ? "密码修改成功" : "修改失败");
        return map;
    }
}
