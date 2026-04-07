package com.poly.java5.Service;


import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Repository.UserRepository;
import com.poly.java5.Utils.Utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;


import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService implements UserDetailsService {
	 @PersistenceContext
    private EntityManager manager;

    @Transactional
    public Map<String, String> register(User user) {
        Map<String, String> errorsMap = new HashMap<>();

        String jpql = "SELECT u FROM User u WHERE u.username = :username OR u.email = :email OR u.phone = :phone";
        List<User> users = manager.createQuery(jpql, User.class)
                .setParameter("username", user.getUsername().trim())
                .setParameter("email", user.getEmail().trim())
                .setParameter("phone", user.getPhone().trim())
                .getResultList();

        for (User u : users) {
            if (u.getUsername().equals(user.getUsername().trim()))
                errorsMap.put("username", "Tên đăng nhập đã tồn tại");
            if (u.getEmail().equalsIgnoreCase(user.getEmail().trim()))
                errorsMap.put("email", "Email đã tồn tại");
            if (u.getPhone().equals(user.getPhone().trim()))
                errorsMap.put("phone", "Số điện thoại đã tồn tại");
        }

        if (errorsMap.isEmpty()) {
            String hashedPassword = Utils.hashPassword(user.getPassword());
            user.setPassword(hashedPassword);
            user.setCreatedDate(LocalDateTime.now());
            user.setActive(true);
            manager.persist(user);
        }

        return errorsMap;
    }

    @Transactional
    public User login(String usernameOrEmail, String password) {
        System.out.println("=== LOGIN DEBUG ===");
        System.out.println("Username/Email: " + usernameOrEmail);
        
        String jpql = """
            SELECT u FROM User u
            WHERE (u.username = :ue OR u.email = :ue)
              AND u.active = true
        """;

        Query query = manager.createQuery(jpql, User.class);
        query.setParameter("ue", usernameOrEmail.trim());

        List<User> users = query.getResultList();
        if (users.isEmpty()) return null;

        User user = users.get(0);
        String dbPassword = user.getPassword();
        
        // Nếu user đăng nhập bằng Google (password rỗng)
        if (dbPassword == null || dbPassword.isEmpty()) {
            System.out.println("User đăng nhập bằng Google, vui lòng dùng Google Login");
            return null;
        }
        
        String hashedInputPassword = Utils.hashPassword(password);
        
        if (dbPassword.equals(hashedInputPassword)) {
            return user;
        }

        return null;
    }

    public User findById(Integer id) {
        return manager.find(User.class, id);
    }

    @Transactional
    public void save(User user) {
        System.out.println("=== SAVING USER ===");
        System.out.println("User ID: " + user.getId());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Email: " + user.getEmail());
        
        if (user.getId() == null) {
            // Chưa có ID -> insert mới
            if (user.getCreatedDate() == null) {
                user.setCreatedDate(LocalDateTime.now());
            }
            if (user.getActive() == null) {
                user.setActive(true);
            }
            if (user.getRole() == null) {
                user.setRole(com.poly.java5.Entity.UserRole.USER);
            }
            manager.persist(user);
            System.out.println("✅ Insert new user, generated ID: " + user.getId());
        } else {
            // Đã có ID -> update
            manager.merge(user);
            System.out.println("✅ Update existing user, ID: " + user.getId());
        }
    }
    
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        
        String jpql = "SELECT u FROM User u WHERE u.username = :username";
        List<User> users = manager.createQuery(jpql, User.class)
                .setParameter("username", username.trim())
                .getResultList();
        
        return users.isEmpty() ? null : users.get(0);
    }
    
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;
        
        String jpql = "SELECT u FROM User u WHERE u.email = :email";
        List<User> users = manager.createQuery(jpql, User.class)
                .setParameter("email", email.trim())
                .getResultList();
        
        System.out.println("findByEmail: " + email + " - Found: " + (users.isEmpty() ? "No" : "Yes"));
        return users.isEmpty() ? null : users.get(0);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User không tồn tại");
        }

        String role = user.getRole() != null 
                ? "ROLE_" + user.getRole().name()
                : "ROLE_USER";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .authorities(role)
                .build();
    }
    
 // =========================
    // FIND BY ID (CHO MENU)
    // =========================
   

    private final UserRepository userRepository = null;
    
    @Transactional
    
	
    
 // Thêm vào class UserService
    public List<User> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }


    // Toggle active bằng query trực tiếp (tối ưu hơn)
    @Transactional
    public void toggleActive(String username) {
        User user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Không tìm thấy user: " + username);
        }
        user.setActive(!Boolean.TRUE.equals(user.getActive()));
        userRepository.save(user);
    }
}
