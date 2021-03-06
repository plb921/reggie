package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/sendMsg")
    public R<String> sendMsg(HttpSession httpSession, @RequestBody User user){
        //获取手机号
        String phone = user.getPhone();

        if (StringUtils.isNotEmpty(phone)) {
            //生成随机4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("4位验证码：{}",code);
            //调用阿里云短信服务API
//            SMSUtils.sendMessage("签名", "", phone, code);
            //存储验证码
            httpSession.setAttribute(phone, code);

            return R.success("验证码发送成功");
        }

        return R.success("验证码发送失败");
    }

    @PostMapping("/login")
    public R<User> login(HttpSession httpSession, @RequestBody Map map){
        //获取手机号 验证码
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();

        //从session中获取保存的验证码
        Object codeSession = httpSession.getAttribute(phone);
        //进行验证码的比对（页面提交的验证码和session中的验证码）
        if (codeSession!=null && codeSession.equals(code)){
            //比对成功，查User表中是否有phone存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            if (user == null){
                //新用户，自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            httpSession.setAttribute("user", user.getId());
            return R.success(user);
        }
        return R.error("登录失败");
    }
}
