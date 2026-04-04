package com.poly.java5.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDetail {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "id")
	    private Integer id;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "cart_id", nullable = false)
	    @ToString.Exclude
	    @EqualsAndHashCode.Exclude
	    private Cart cart;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "book_id", nullable = false)
	    @ToString.Exclude
	    @EqualsAndHashCode.Exclude
	    private Book book;
	    
	    @Column(name = "quantity")
	    private Integer quantity;
	    
	    @Column(name = "price")
	    private BigDecimal price;
	    
	    @Column(name = "selected", nullable = false)
	    private Boolean selected = false;

	    @PrePersist
	    @PreUpdate
	    public void prePersist() {
	        if (selected == null) {
	            selected = false;
	        }
	    }
	    // Tính tổng tiền cho item này
	    public BigDecimal calculateTotal() {
	        return price.multiply(BigDecimal.valueOf(quantity));
	    }

}
