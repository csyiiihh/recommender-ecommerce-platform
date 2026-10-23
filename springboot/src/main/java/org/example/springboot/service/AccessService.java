package org.example.springboot.service;

import org.example.springboot.entity.*;
import org.example.springboot.mapper.*;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Objects;

@Service
public class AccessService {
    @Autowired private OrderMapper orders;
    @Autowired private ProductMapper products;
    @Autowired private AddressMapper addresses;
    @Autowired private CartMapper carts;
    @Autowired private FavoriteMapper favorites;
    @Autowired private ReviewMapper reviews;
    @Autowired private LogisticsMapper logistics;
    @Autowired private GroupBuyActivityMapper activities;
    @Autowired private CustomerServiceSessionMapper sessions;
    public User current() {
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) throw new AccessDeniedException("请先登录");
        return user;
    }
    public boolean isAdmin() { return "ADMIN".equals(current().getRole()); }
    public boolean isMerchant() { return "MERCHANT".equals(current().getRole()); }
    public void admin() { if (!isAdmin()) denied(); }
    public void staff() { if (!isAdmin() && !isMerchant()) denied(); }
    public void owner(Long userId) { if (!isAdmin() && !Objects.equals(current().getId(), userId)) denied(); }
    public Long scope(Long userId) { if (isAdmin()) return userId; owner(userId == null ? current().getId() : userId); return current().getId(); }
    public void product(Long id) { staff(); Product p = products.selectById(id); if (p == null) denied(); owner(p.getMerchantId()); }
    public void order(Long id, boolean sellerAllowed) {
        Order o = orders.selectById(id); if (o == null) denied();
        if (isAdmin() || Objects.equals(o.getUserId(), current().getId())) return;
        if (sellerAllowed && isMerchant()) { product(o.getProductId()); return; }
        denied();
    }
    public void sellerOrder(Long id) { staff(); Order o = orders.selectById(id); if(o == null) denied(); product(o.getProductId()); }
    public void address(Long id) { Address a = addresses.selectById(id); if(a == null) denied(); owner(a.getUserId()); }
    public Long addressOwner(Long id) { address(id); return addresses.selectById(id).getUserId(); }
    public void receiveLogistics(Long id) { Logistics l = logistics.selectById(id); if(l == null) denied(); order(l.getOrderId(), false); }
    public void cart(Long id) { Cart c = carts.selectById(id); if(c == null) denied(); owner(c.getUserId()); }
    public void favorite(Long id) { Favorite f = favorites.selectById(id); if(f == null) denied(); owner(f.getUserId()); }
    public void review(Long id) { Review r = reviews.selectById(id); if(r == null) denied(); owner(r.getUserId()); }
    public void logistics(Long id, boolean seller) { Logistics l = logistics.selectById(id); if(l == null) denied(); if(seller) sellerOrder(l.getOrderId()); else order(l.getOrderId(), true); }
    public void activity(Long id) { staff(); GroupBuyActivity a = activities.selectById(id); if(a == null) denied(); owner(a.getMerchantId()); }
    public void session(Long id) {
        CustomerServiceSession s = sessions.selectById(id); if(s == null) denied();
        if(!isAdmin() && !Objects.equals(s.getUserId(),current().getId()) && !Objects.equals(s.getServiceId(), current().getId())) denied();
    }
    private void denied() { throw new AccessDeniedException("无权访问该资源"); }
}
