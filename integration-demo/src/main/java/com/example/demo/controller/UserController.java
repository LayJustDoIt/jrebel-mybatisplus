package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/user/selectById/{id}")
    public User selectById(@PathVariable Long id) {
        return userService.selectById(id);
    }

    @GetMapping("/user/selectList")
    public List<User> selectList() {
        return userService.selectList();
    }

    @GetMapping("/user/selectPage")
    public IPage<User> selectPage(@RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "2") int pageSize) {
        return userService.selectPage(pageNum, pageSize);
    }
}
