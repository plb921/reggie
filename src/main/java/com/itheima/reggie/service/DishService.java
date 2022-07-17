package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

public interface DishService extends IService<Dish> {

    /**
     * 新增菜品，同时向菜品口味表插入数据
     */
    void saveWithFlavor(DishDto dishDto);

    /**
     * 修改菜品，同时向菜品口味表插入数据
     */
    void updateWithFlavor(DishDto dishDto);
}
