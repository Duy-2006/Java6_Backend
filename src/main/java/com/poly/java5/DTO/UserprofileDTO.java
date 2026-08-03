package com.poly.java5.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserprofileDTO {
	private String name;
	private String email;
	private String phone;
	private String avatar;
	private String customerRank;
	private Double lifetimeValue;
	private Double discountPercent;
	private java.util.List<java.util.Map<String, Object>> recentBooks;
}
