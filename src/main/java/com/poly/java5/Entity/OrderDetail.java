package com.poly.java5.Entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "OrderDetails")
public class OrderDetail {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "id")
	    private Integer id;

	 @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "order_id", nullable = false)
	    @ToString.Exclude
	    @EqualsAndHashCode.Exclude
	    private Order order;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "book_id", nullable = false)
	    @ToString.Exclude
	    @EqualsAndHashCode.Exclude
	    private Book book;
	    
	    @Column(name = "quantity", nullable = false)
	    private Integer quantity;
	    
	    @Column(name = "price", nullable = false, precision = 10, scale = 2)
	    private BigDecimal price;
	    
	    // Tính subtotal cho item này
	    public BigDecimal calculateSubtotal() {
	        return price.multiply(BigDecimal.valueOf(quantity));
	    }
	    // 🔥 QUAN TRỌNG NHẤT
	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof OrderDetail)) return false;
	        OrderDetail that = (OrderDetail) o;
	        return id != null && id.equals(that.id);
	    }

	    @Override
	    public int hashCode() {
	        return getClass().hashCode();
	    }
  
   
}