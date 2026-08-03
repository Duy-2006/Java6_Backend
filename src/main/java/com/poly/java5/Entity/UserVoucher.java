package com.poly.java5.Entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "User_Vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVoucher {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "user_id", nullable = false)
	    private User user;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "voucher_id", nullable = false)
	    private Voucher voucher;

	    @Column(name = "is_used", nullable = false)
	    private Boolean isUsed = false;

	    @Column(name = "used_date")
	    private LocalDateTime usedDate;

	    @PrePersist
	    protected void onCreate() {
	        if (usedDate == null && Boolean.TRUE.equals(isUsed)) {
	            usedDate = LocalDateTime.now();
	        }
	    }
}
