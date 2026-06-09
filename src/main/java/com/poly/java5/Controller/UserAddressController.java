package com.poly.java5.Controller;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserAddress;
import com.poly.java5.Repository.UserAddressRepository;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/profile/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @GetMapping("")
    public ResponseEntity<?> getUserAddresses() {
        try {
            User user = AuthUtil.getAuthenticatedUser(userService);
            if (user == null) return ResponseEntity.status(401).body("Unauthorized");

            List<UserAddress> addresses = userAddressRepository.findByUserId(user.getId());
            
            // Map to DTO to avoid Jackson serialization issues with Hibernate Proxy and User object
            List<Map<String, Object>> result = addresses.stream().map(a -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", a.getId());
                map.put("receiverName", a.getReceiverName());
                map.put("receiverPhone", a.getReceiverPhone());
                map.put("provinceId", a.getProvinceId());
                map.put("provinceName", a.getProvinceName());
                map.put("districtId", a.getDistrictId());
                map.put("wardCode", a.getWardCode());
                map.put("wardName", a.getWardName());
                map.put("street", a.getStreet());
                map.put("isDefault", a.getIsDefault());
                return map;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
    }

    @PostMapping("")
    public ResponseEntity<?> addAddress(@RequestBody UserAddress userAddress) {
        try {
            User user = AuthUtil.getAuthenticatedUser(userService);
            if (user == null) return ResponseEntity.status(401).body("Unauthorized");

            // Nếu đặt làm mặc định, hủy mặc định các địa chỉ cũ
            if (Boolean.TRUE.equals(userAddress.getIsDefault())) {
                List<UserAddress> oldAddresses = userAddressRepository.findByUserId(user.getId());
                for (UserAddress old : oldAddresses) {
                    if (Boolean.TRUE.equals(old.getIsDefault())) {
                        old.setIsDefault(false);
                        userAddressRepository.save(old);
                    }
                }
            } else {
                // Nếu chưa có địa chỉ nào thì tự động đặt làm mặc định
                List<UserAddress> existings = userAddressRepository.findByUserId(user.getId());
                if (existings.isEmpty()) {
                    userAddress.setIsDefault(true);
                }
            }

            userAddress.setUser(user);
            UserAddress saved = userAddressRepository.save(userAddress);
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", saved.getId());
            map.put("receiverName", saved.getReceiverName());
            map.put("receiverPhone", saved.getReceiverPhone());
            map.put("provinceId", saved.getProvinceId());
            map.put("provinceName", saved.getProvinceName());
            map.put("districtId", saved.getDistrictId());
            map.put("wardCode", saved.getWardCode());
            map.put("wardName", saved.getWardName());
            map.put("street", saved.getStreet());
            map.put("isDefault", saved.getIsDefault());
            
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving address: " + e.getMessage());
        }
    }
}
