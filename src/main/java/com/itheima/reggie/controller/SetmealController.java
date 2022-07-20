package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐
     */
    @CacheEvict(value = "setmealCache", key = "#setmealDto.categoryId + '_' + #setmealDto.status")
    @PostMapping
    public R<String> addSetmeal(@RequestBody SetmealDto setmealDto){
        setmealService.addSetmeal(setmealDto);
        return R.success("新增套餐成功");
    }

    /**
     * 按页查询套餐
     */
    @GetMapping("/page")
    public R<Page<SetmealDto>> page(int page, int pageSize, String name){
        Page<SetmealDto> setmealDtoPage = new Page<>();
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name!=null, Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(setmealPage, queryWrapper);
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");
        List<Setmeal> setmealrecords = setmealPage.getRecords();
        List<SetmealDto> setmealDtoRecords = setmealrecords.stream().map((item)->{
            SetmealDto setmealDto = new SetmealDto();
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            BeanUtils.copyProperties(item, setmealDto);
            setmealDto.setCategoryName(category.getName());
            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPage.setRecords(setmealDtoRecords);

        return R.success(setmealDtoPage);
    }

    /**
     * 根据id查询套餐以及包含的菜品信息
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id){
        SetmealDto setmealDto = new SetmealDto();

        Setmeal setmeal = setmealService.getById(id);
        BeanUtils.copyProperties(setmeal, setmealDto);

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(setmealDishes);

        return R.success(setmealDto);
    }

    /**
     * 根据套餐类别id查询套餐列表
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status")
    public R<List<Setmeal>> getByCategoryId(Setmeal setmeal){

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus()!=null, Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> setmealList = setmealService.list(queryWrapper);

//        List<SetmealDto> setmealDtoList = setmealList.stream().map((item)->{
//            LambdaQueryWrapper<SetmealDish> setmealDishQueryWrapper = new LambdaQueryWrapper<>();
//            setmealDishQueryWrapper.eq(SetmealDish::getSetmealId, item.getId());
//            List<SetmealDish> dishes = setmealDishService.list(setmealDishQueryWrapper);
//
//           SetmealDto setmealDto = new SetmealDto();
//           setmealDto.setSetmealDishes(dishes);
//           BeanUtils.copyProperties(item, setmealDto);
//           return setmealDto;
//        }).collect(Collectors.toList());
        return R.success(setmealList);
    }

    /**
     * 根据id删除套餐
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true)  // 删除setmealCache下全部套餐
    public R<String> deleteSetmeal(@RequestParam List<Long> ids){
        setmealService.removeSetmeal(ids);
        return R.success("删除套餐成功");
    }


}
