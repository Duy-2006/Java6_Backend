package com.poly.java5.Entity;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "user_address")
public class UserAddress implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "receiver_name", columnDefinition = "NVARCHAR(255)")
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "province_id", nullable = false)
    private Integer provinceId;

    @Column(name = "province_name", columnDefinition = "NVARCHAR(100)")
    private String provinceName;

    @Column(name = "district_id", nullable = false)
    private Integer districtId;

    @Column(name = "ward_code", length = 50)
    private String wardCode;

    @Column(name = "ward_name", columnDefinition = "NVARCHAR(100)")
    private String wardName;

    @Column(name = "street", columnDefinition = "NVARCHAR(255)")
    private String street;

    @Column(name = "is_default")
    private Boolean isDefault;
}
