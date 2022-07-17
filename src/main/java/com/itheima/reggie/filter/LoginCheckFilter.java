package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 过滤器：检查用户是否已登录
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {

    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCH = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1获取本次请求的URI
        String requestURI = request.getRequestURI();

        //2 判断本次请求是否需要处理
        //不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout" ,
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };
        if(matchURI(urls, requestURI)){
            filterChain.doFilter(request, response);
            return;
        }

        //3 判断后台是否登录，若已登录则放行
        if (request.getSession().getAttribute("employee") != null){
            BaseContext.setCurrentId((Long) request.getSession().getAttribute("employee"));
            filterChain.doFilter(request, response);
            return;
        }

        //4 判断前台是否登录，若已登录则放行
        if (request.getSession().getAttribute("user") != null){
            BaseContext.setCurrentId((Long) request.getSession().getAttribute("user"));
            filterChain.doFilter(request, response);
            return;
        }

        //未登录，拦截，对应 /backend/js/request.json
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
    }

    public boolean matchURI(String[] urls, String requestURI){
        for(String url : urls){
            if(PATH_MATCH.match(url, requestURI)){ return true;}
        }
        return false;
    }
}
