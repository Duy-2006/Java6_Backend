package com.poly.java5.Service;

import com.poly.java5.Entity.User;
import com.poly.java5.Utils.Utils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @PersistenceContext
    private EntityManager manager;

    // =========================
    // REGISTER
    // =========================
    @Transactional
    public Map<String, String> register(User user) {
        Map<String, String> errorsMap = new HashMap<>();

        String jpql = """
            SELECT u FROM User u
            WHERE u.username = :username
               OR u.email = :email
               OR u.phone = :phone
        """;

        List<User> users = manager
                .createQuery(jpql, User.class)
                .setParameter("username", user.getUsername())
                .setParameter("email", user.getEmail())
                .setParameter("phone", user.getPhone())
                .getResultList();

        for (User u : users) {
            if (u.getUsername().equals(user.getUsername())) {
                errorsMap.put("username", "Tên đăng nhập đã tồn tại");
            }
            if (u.getEmail().equals(user.getEmail())) {
                errorsMap.put("email", "Email đã tồn tại");
            }
            if (u.getPhone().equals(user.getPhone())) {
                errorsMap.put("phone", "Số điện thoại đã tồn tại");
            }
        }

        if (errorsMap.isEmpty()) {
            // 🔐 HASH PASSWORD
            user.setPassword(Utils.hashPassword(user.getPassword()));
            manager.persist(user);
        }

        return errorsMap;
    }

    // =========================
    // LOGIN
    // =========================
    @Transactional
    public User login(String usernameOrEmail, String password) {
        String jpql = """
            SELECT u FROM User u
            WHERE (u.username = :ue OR u.email = :ue)
              AND u.active = true
        """;

        Query query = manager.createQuery(jpql, User.class);
        query.setParameter("ue", usernameOrEmail);

        List<User> users = query.getResultList();
        if (users.isEmpty()) return null;

        User user = users.get(0);

        String dbPassword = user.getPassword();
        String inputHashed = Utils.hashPassword(password);

        // ===== LOG DEBUG =====
        System.out.println(">>> DB PASS    = " + dbPassword);
        System.out.println(">>> INPUT HASH = " + inputHashed);

        // CASE 1: DB đang lưu HASH
        if (dbPassword.length() >= 40) {
            if (dbPassword.equals(inputHashed)) {
                return user;
            }
        }

        // CASE 2: DB đang lưu PLAIN TEXT → tự động hash lại
        if (dbPassword.equals(password)) {
            System.out.println("⚠ PASSWORD PLAIN → AUTO HASH");
            user.setPassword(inputHashed);
            manager.merge(user);
            return user;
        }

        System.out.println("❌ WRONG PASSWORD");
        return null;
    }

    // =========================
    // TÌM USER THEO ID
    // =========================
    public User findById(Integer id) {
        return manager.find(User.class, id);
    }

    // =========================
    // CẬP NHẬT USER (dùng cho update profile sau này)
    // =========================
    @Transactional
    public User updateUser(User user) {
        return manager.merge(user);
    }

    // =========================
    // KIỂM TRA USERNAME HOẶC EMAIL ĐÃ TỒN TẠI
    // =========================
    public boolean existsByUsernameOrEmail(String username, String email) {
        String jpql = """
            SELECT COUNT(u) FROM User u 
            WHERE u.username = :username OR u.email = :email
        """;
        Long count = manager.createQuery(jpql, Long.class)
                .setParameter("username", username)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    // =========================
    // LẤY DANH SÁCH SÁCH CỦA USER (Thư viện - sau này sẽ hoàn thiện)
    // =========================
    public List<Object[]> getUserLibrary(Integer userId) {
        // TODO: Kết nối với Order, Cart, Book... sau này
        String jpql = """
            SELECT b.title, b.author, 'Sách vật lý' as type 
            FROM Book b 
            WHERE b.id IN (SELECT od.book.id FROM OrderDetail od WHERE od.order.user.id = :userId)
        """;
        return manager.createQuery(jpql, Object[].class)
                .setParameter("userId", userId)
                .getResultList();
    }
}