package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     */
    @PostMapping
    public R<String> addDish(@RequestBody DishDto dishDto){
        dishService.saveWithFlavor(dishDto);
        //删除redis中的缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页
     */
    @GetMapping("/page")
    public R<Page<DishDto>> getPage(int page, int pageSize, String name){

        Page<Dish> pageinfo = new Page(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name!=null, Dish::getName, name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        dishService.page(pageinfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageinfo, dishDtoPage, "records");  //注入page中除records外的信息

        List<Dish> dishRecords = pageinfo.getRecords();
        List<DishDto> dishDtoRecords = dishRecords.stream().map((item)->{  //生成DishDtoPage的records
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);  //注入Dish

            Long categoryId = item.getCategoryId();  //获取当前item中的菜品id
            Category category = categoryService.getById(categoryId);
            if (category!=null){
                String categoryName = category.getName();  //获取菜品id对应的菜品名称
                dishDto.setCategoryName(categoryName);  //注入菜品名称到dishDto中
            }
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(dishDtoRecords);
        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品
     */
    @GetMapping("/{id}")
    public R<DishDto> getDishById(@PathVariable Long id){
        DishDto dishDto = new DishDto();
        Dish dish = dishService.getById(id);
        BeanUtils.copyProperties(dish, dishDto);

        //填入口味列表
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> dishFlavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(dishFlavors);
        return R.success(dishDto);
    }

    /**
     * 根据类别id查询菜品
     */
    @GetMapping("/list")
    public R<List<DishDto>> getDishByCategoryId(Dish dish){
        List<DishDto> dishDtoList = null;
        String key = "dish_" +  dish.getCategoryId() + "_" + dish.getStatus(); //redis中以类别id作为key缓存菜品列表

        //先从Redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        //如果存在，直接返回缓存中的数据
        if (dishDtoList != null){
            return R.success(dishDtoList);
        }
        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到Redis
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId, dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus, dish.getStatus());
        List<Dish> dishList = dishService.list(queryWrapper);

        dishDtoList = dishList.stream().map((item)->{
            //查询菜品对应的口味
            LambdaQueryWrapper<DishFlavor> flavorQueryWrapper = new LambdaQueryWrapper<>();
            flavorQueryWrapper.eq(DishFlavor::getDishId, item.getId());
            List<DishFlavor> flavors = dishFlavorService.list(flavorQueryWrapper);

            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            dishDto.setFlavors(flavors);
            return dishDto;
        }).collect(Collectors.toList());
        //缓存到Redis
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

    /**
     * 修改菜品
     */
    @PutMapping
    public R<String> upadteDish(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);
        //删除redis中的缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("菜品修改成功");
    }

    /**
     * 菜品停售/启售
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable int status, @RequestParam List<Long> ids){

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId, ids);

        List<Dish> dishes = dishService.list(queryWrapper);
        dishes = dishes.stream().map((item)->{
            item.setStatus(status);
            return item;
        }).collect(Collectors.toList());

        dishService.updateBatchById(dishes);

        return R.success("修改菜品状态成功");
    }

}