package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.Address;
import org.example.springboot.mapper.AddressMapper;
import org.example.springboot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressService.class);

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private UserMapper userMapper;

    public Result<?> createAddress(Address address) {
        try {
            int result = addressMapper.insert(address);
            if (result > 0) {
                LOGGER.info("创建地址成功，地址ID：{}，用户ID：{}", address.getId(), address.getUserId());
                return ResultUtils.success(address);
            }
            LOGGER.warn("创建地址失败，数据库插入返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建地址失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("创建地址异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "创建地址失败：" + e.getMessage());
        }
    }

    public Result<?> updateAddress(Long id, Address address) {
        // 检查地址是否存在
        Address existingAddress = addressMapper.selectById(id);
        if (existingAddress == null) {
            LOGGER.warn("更新地址失败，地址ID：{} 不存在", id);
            return ResultUtils.dataNotFound("地址不存在");
        }
        
        address.setId(id);
        try {
            int result = addressMapper.updateById(address);
            if (result > 0) {
                LOGGER.info("更新地址成功，地址ID：{}", id);
                return ResultUtils.success(address);
            }
            LOGGER.warn("更新地址失败，地址ID：{}，数据库更新返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新地址失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("更新地址异常，地址ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "更新地址失败：" + e.getMessage());
        }
    }

    public Result<?> deleteAddress(Long id) {
        // 检查地址是否存在
        Address address = addressMapper.selectById(id);
        if (address == null) {
            LOGGER.warn("删除地址失败，地址ID：{} 不存在", id);
            return ResultUtils.dataNotFound("地址不存在");
        }
        
        try {
            int result = addressMapper.deleteById(id);
            if (result > 0) {
                LOGGER.info("删除地址成功，地址ID：{}", id);
                return ResultUtils.success();
            }
            LOGGER.warn("删除地址失败，地址ID：{}，数据库删除返回 0", id);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除地址失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("删除地址异常，地址ID：{}，错误：{}", id, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除地址失败：" + e.getMessage());
        }
    }

    public Result<?> getAddressById(Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            LOGGER.warn("查询地址失败，地址ID：{} 不存在", id);
            return ResultUtils.dataNotFound("地址不存在");
        }
        
        // 填充用户信息
        address.setUser(userMapper.selectById(address.getUserId()));
        return ResultUtils.success(address);
    }

    public Result<?> getAddressesByUserId(Long userId) {
        try {
            LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Address::getUserId, userId);
            List<Address> addresses = addressMapper.selectList(queryWrapper);
            
            if (addresses != null && !addresses.isEmpty()) {
                // 填充用户信息
                addresses.forEach(address -> 
                    address.setUser(userMapper.selectById(address.getUserId()))
                );
                LOGGER.debug("查询用户地址，用户ID：{}，地址数：{}", userId, addresses.size());
                return ResultUtils.success(addresses);
            }
            LOGGER.info("查询用户地址，用户ID：{}，结果为空", userId);
            return ResultUtils.success(addresses); // 返回空列表而不是错误
        } catch (Exception e) {
            LOGGER.error("查询用户地址异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询地址列表失败");
        }
    }

    public Result<?> getAddressesByPage(Long userId, Integer currentPage, Integer size) {
        try {
            LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
            if (userId != null) {
                queryWrapper.eq(Address::getUserId, userId);
            }

            Page<Address> page = new Page<>(currentPage, size);
            Page<Address> result = addressMapper.selectPage(page, queryWrapper);

            // 填充用户信息
            result.getRecords().forEach(address -> 
                address.setUser(userMapper.selectById(address.getUserId()))
            );

            LOGGER.debug("分页查询地址，当前页：{}，每页大小：{}，总数：{}", currentPage, size, result.getTotal());
            return ResultUtils.success(result);
        } catch (Exception e) {
            LOGGER.error("分页查询地址异常：{}", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "查询地址列表失败");
        }
    }

    public Result<?> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.warn("批量删除地址失败，ID列表为空");
            return ResultUtils.paramError("ID列表不能为空");
        }
        
        try {
            // 检查每个地址是否存在
            for (Long id : ids) {
                Address address = addressMapper.selectById(id);
                if (address == null) {
                    LOGGER.warn("批量删除地址，地址ID：{} 不存在", id);
                    return ResultUtils.dataNotFound("地址ID：" + id + " 不存在");
                }
            }
            
            LambdaQueryWrapper<Address> wrapper = Wrappers.lambdaQuery();
            wrapper.in(Address::getId, ids);
            int result = addressMapper.delete(wrapper);
            
            if (result > 0) {
                LOGGER.info("批量删除地址成功，删除数量：{}，ID列表：{}", result, ids);
                return ResultUtils.success();
            }
            LOGGER.warn("批量删除地址失败，数据库删除返回 0");
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除地址失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("批量删除地址异常，ID列表：{}，错误：{}", ids, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "批量删除地址失败：" + e.getMessage());
        }
    }
} 