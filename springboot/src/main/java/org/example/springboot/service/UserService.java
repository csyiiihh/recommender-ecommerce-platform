package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.entity.*;
import org.example.springboot.enumClass.AccountStatus;
import org.example.springboot.mapper.*;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.MenusUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @org.springframework.beans.factory.annotation.Autowired private AccessService access;
    @org.springframework.beans.factory.annotation.Autowired private org.example.springboot.service.email.VerificationCodeService codes;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    @Resource
    private UserMapper userMapper;
    @Resource
    private MenuMapper menuMapper;




    @Resource
    private AddressMapper addressMapper;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private ProductMapper productMapper;
@Resource
private ReviewMapper reviewMapper;
@Resource
private CartMapper cartMapper;



@Resource
private FavoriteMapper favoriteMapper;

    @Resource
    private PasswordEncoder bCryptPasswordEncoder;

    @Resource
    private StockInMapper stockInMapper;


    public User getByEmail(String email){
        LambdaQueryWrapper<User> studentLambdaQueryWrapper = new LambdaQueryWrapper<User>().eq(User::getEmail, email);
        return userMapper.selectOne(studentLambdaQueryWrapper);
    }

    @org.springframework.transaction.annotation.Transactional
    public Result<?> register(User user) {
        if (user.getPassword() == null || user.getPassword().length() < 6 || user.getPassword().length() > 72) return Result.error("400", "密码长度须为6到72个字符");
        if (user.getUsername() == null || (user.getUsername().isBlank() || user.getUsername().length() < 3 || user.getUsername().length() > 20) || user.getEmail() == null || !user.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) return Result.error("400", "用户名或邮箱格式错误");
        if (!codes.verifyCode("register", user.getEmail(), user.getCode())) return Result.error("400", "验证码错误或过期");
        user.setRole("USER"); user.setBusinessLicense(null);
        int result = createUser(user);
        return result > 0 ? Result.success() : Result.error("400", "用户名或邮箱已存在");
    }
    public boolean resetPassword(String email, String password, String code) {
        if (!codes.verifyCode("reset", email, code)) return false;
        User user = getByEmail(email); if(user == null) return false;
        User update = new User(); update.setId(user.getId()); update.setPassword(bCryptPasswordEncoder.encode(password));
        return userMapper.updateById(update) > 0;
    }
    public Result<?> login(User user) {
        try {
            User compare = getByUsername(user.getUsername());
            if (compare == null) {
                LOGGER.warn("登录失败，用户名：{} 不存在", user.getUsername());
                return ResultUtils.error(ResultCode.USER_NOT_FOUND, "用户不存在");
            }
            
            if (compare.getStatus().equals(AccountStatus.DISABLED.getValue())) {
                LOGGER.warn("登录失败，用户名：{}，账号被禁用", user.getUsername());
                return ResultUtils.error(ResultCode.ACCOUNT_DISABLED, "账号被禁用");
            }
            
            if (bCryptPasswordEncoder.matches(user.getPassword(), compare.getPassword())) {
                List<Menu> roleMenuList = menuMapper.selectList(null);
                String token = JwtTokenUtils.genToken(String.valueOf(compare.getId()), compare.getPassword());
                compare.setMenuList(MenusUtils.allocMenus(roleMenuList, compare.getRole()));
                compare.setToken(token);
                LOGGER.info("用户登录成功，用户ID：{}，用户名：{}", compare.getId(), compare.getUsername());
                return ResultUtils.success(compare);
            }
            
            LOGGER.warn("登录失败，用户名：{}，密码错误", user.getUsername());
            return ResultUtils.error(ResultCode.LOGIN_FAILED, "用户名或密码错误");
        } catch (Exception e) {
            LOGGER.error("登录异常，用户名：{}，错误：{}", user.getUsername(), e.getMessage(), e);
            throw new BusinessException(ResultCode.LOGIN_FAILED.getCode(), "登录失败：" + e.getMessage());
        }
    }
    public List<User> getUserByRole(String role) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(User::getRole, role);
        return userMapper.selectList(queryWrapper);
    }
    public int createUser(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank() || user.getEmail() == null || user.getEmail().isBlank()) throw new BusinessException("用户名和邮箱不能为空");
        user.setId(null);
        if (!java.util.Set.of("USER", "MERCHANT", "ADMIN").contains(user.getRole() == null ? "USER" : user.getRole())) throw new BusinessException("无效角色");
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", user.getUsername())) != null) {
            return -1;
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, user.getEmail())) != null) {
            return -2;
        }
        if (user.getPassword() == null || user.getPassword().length() < 6 || user.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new BusinessException("密码须至少6个字符且不超过72字节");
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setRole(StringUtils.isNotBlank(user.getRole()) ? user.getRole() : "USER");
        user.setStatus(AccountStatus.ENABLED.getValue());
        return userMapper.insert(user);
    }
    @org.springframework.transaction.annotation.Transactional
    public Integer deleteUserById(int id) {
        access.admin();
        if (!checkStockRelation(id)) return -2;
        if (!checkUserProducts(id)) return -1;
        if (orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getUserId, id)) > 0) throw new BusinessException("存在历史订单，请停用账号而非删除");
        deleteUserRelations(id);
        return userMapper.deleteById(id);
    }
    @org.springframework.transaction.annotation.Transactional
    public boolean updateUser(Long id, User user) {
        User old = userMapper.selectById(id);
        if (old == null) return false;
        User safe = new User(); safe.setId(id); safe.setName(user.getName()); safe.setBusinessLicense(user.getBusinessLicense());
        if(user.getEmail() != null && !java.util.Objects.equals(user.getEmail(), old.getEmail())) throw new BusinessException("邮箱变更需要单独验证，当前暂不支持");
        // Email changes require a separate verified-email workflow.
        if (access.isAdmin()) { if(user.getRole() != null && !java.util.Set.of("USER", "MERCHANT", "ADMIN").contains(user.getRole())) throw new BusinessException("无效角色"); safe.setRole(user.getRole()); safe.setStatus(user.getStatus()); safe.setBusinessLicense(user.getBusinessLicense()); }
        return userMapper.updateById(safe) > 0;
    }
    public User getByUsername(String  username){
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername,username));
    }


    public boolean updatePassword(int id, UserPasswordUpdate userPasswordUpdate) {
        String password = userPasswordUpdate.getNewPassword();
        if(password == null || password.length() < 6 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new BusinessException("新密码长度无效");
        User oldUser = userMapper.selectById(id);
        if (oldUser != null &&bCryptPasswordEncoder.matches(userPasswordUpdate.getOldPassword(),oldUser.getPassword())) {
            oldUser.setPassword(bCryptPasswordEncoder.encode(userPasswordUpdate.getNewPassword()));
            return userMapper.updateById(oldUser) > 0;
        }

        return false;
    }
    @org.springframework.transaction.annotation.Transactional
    public Integer deleteBatch(List<Integer> ids) {
        access.admin();
        for (Integer id : ids) {
            if (!checkStockRelation(id)) return -2;
            if (!checkUserProducts(id)) return -1;
            if (orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getUserId, id)) > 0) throw new BusinessException("存在历史订单，请停用账号而非删除");
        }
        for (Integer id : ids) deleteUserRelations(id);
        return userMapper.deleteByIds(ids);
    }

    public List<User> getAllUsers() {
        return userMapper.selectList(new QueryWrapper<>());
    }
    public User getUserById(int id) {
        User user = userMapper.selectById(id);
        if (user != null && user.getStatus().equals(AccountStatus.ENABLED.getValue())) {
            return user;
        }
        return null;
    }
    public Page<User> getUsersByPage(String username, String name, String role, String status, Integer currentPage, Integer size) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like(User::getUsername, username);
        }

        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like(User::getName, name);
        }
        if (StringUtils.isNotBlank(role)) {
            queryWrapper.eq(User::getRole, role);
        }
        if (StringUtils.isNotBlank(status)) {
            queryWrapper.eq(User::getStatus, status);
        }
        Page<User> page = new Page<>(currentPage, size);
        return userMapper.selectPage(page, queryWrapper);

    }
    private boolean checkUserProducts(int id){
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, id));
        for (Product product : products) {
            if(product!=null){
                return false;

            }
        }
        return true;
    }

    private void deleteUserRelations(int id){
   
                List<Address> addresses = addressMapper.selectList(new LambdaQueryWrapper<Address>().eq(Address::getUserId, id));
        for (Address address : addresses) {
            if(address!=null){
                addressMapper.deleteById(address);
            }
        }

        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getUserId, id));
        for (Order order : orders) {
            if(order!=null){
                orderMapper.deleteById(order);
            }
        }

        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>().eq(Review::getUserId, id));
        for (Review review : reviews) {
            if(review!=null){
                reviewMapper.deleteById(review);
            }
        }
        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, id));
        for (Cart cart : carts) {
            if(cart!=null){
                cartMapper.deleteById(cart);
            }
        }

        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, id));
        for (Favorite favorite : favorites) {
            if(favorite!=null){
                favoriteMapper.deleteById(favorite);
            }
        }

    }
  private   Boolean checkStockRelation(int id){
      List<StockIn> stockIns = stockInMapper.selectList(new LambdaQueryWrapper<StockIn>().eq(StockIn::getOperatorId, id));
      for (StockIn stockIn : stockIns) {
          if(stockIn!=null){
                return false;
          }
      }
      return true;
    }


}
