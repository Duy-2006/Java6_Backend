package com.poly.java5.Entity; // Bạn kiểm tra lại chữ 'entity' viết thường hay 'Entity' viết hoa để khớp với package của dự án nhé

import jakarta.persistence.*;

@Entity
@Table(name = "publishers")
public class Publisher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;
    
    private String address;
    private String phone;
    private Boolean active = true;

    // 1. BẮT BUỘC PHẢI CÓ: Constructor không tham số cho Hibernate
    public Publisher() {
    }

    // 2. Constructor đầy đủ tham số
    public Publisher(Integer id, String name, String address, String phone, Boolean active) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.active = active;
    }

    // 3. Các hàm Getter và Setter thuần
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @ManyToMany(mappedBy = "publishers")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<Book> books;
}