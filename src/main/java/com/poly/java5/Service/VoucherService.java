package com.poly.java5.Service;
import com.poly.java5.Entity.Voucher;
import com.poly.java5.Repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class VoucherService {
	 @Autowired
	    private VoucherRepository voucherRepository;

	    // ── ADMIN: CRUD ───────────────────────────────────────────

	    public List<Voucher> findAll() {
	        return voucherRepository.findAllByOrderByIdDesc();
	    }

	    public Voucher findById(Integer id) {
	        return voucherRepository.findById(id).orElse(null);
	    }

	    @Transactional
	    public Voucher create(Map<String, Object> body) {
	        String code = body.get("code").toString().toUpperCase().trim();

	        if (voucherRepository.existsByCode(code)) {
	            throw new RuntimeException("Mã voucher '" + code + "' đã tồn tại.");
	        }

	        Voucher v = buildFromBody(new Voucher(), body);
	        v.setCode(code);
	        v.setUsedCount(0);
	        return voucherRepository.save(v);
	    }

	    @Transactional
	    public Voucher update(Integer id, Map<String, Object> body) {
	        Voucher v = voucherRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher ID: " + id));
	        return voucherRepository.save(buildFromBody(v, body));
	    }

	    @Transactional
	    public void delete(Integer id) {
	        if (!voucherRepository.existsById(id)) {
	            throw new RuntimeException("Không tìm thấy voucher ID: " + id);
	        }
	        voucherRepository.deleteById(id);
	    }

	    // ── USER: Áp dụng voucher ────────────────────────────────

	    // Validate và trả về số tiền giảm
	    public Map<String, Object> applyVoucher(String code, Double orderAmount) {
	        Voucher v = voucherRepository
	                .findValidVoucher(code.toUpperCase(), LocalDate.now(), orderAmount)
	                .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ hoặc đã hết hạn."));

	        Double discount = v.calculateDiscount(orderAmount);

	        return Map.of(
	            "voucherId",    v.getId(),
	            "code",         v.getCode(),
	            "discountType", v.getDiscountType(),
	            "discount",     discount,
	            "finalAmount",  Math.max(0, orderAmount - discount)
	        );
	    }

	    // Tăng usedCount sau khi đặt hàng thành công
	    @Transactional
	    public void incrementUsedCount(Integer voucherId) {
	        Voucher v = voucherRepository.findById(voucherId)
	                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher."));
	        v.setUsedCount(v.getUsedCount() + 1);
	        voucherRepository.save(v);
	    }

	    // Lấy danh sách voucher đang active (hiển thị cho user)
	    public List<Voucher> findActiveVouchers() {
	        return voucherRepository.findActiveVouchers(LocalDate.now());
	    }

	    // ── Helper: map body → entity ────────────────────────────
	    private Voucher buildFromBody(Voucher v, Map<String, Object> body) {
	        if (body.containsKey("discountType"))
	            v.setDiscountType(body.get("discountType").toString());

	        if (body.containsKey("discountValue"))
	            v.setDiscountValue(Double.parseDouble(body.get("discountValue").toString()));

	        if (body.containsKey("minOrderValue"))
	            v.setMinOrderValue(Double.parseDouble(body.get("minOrderValue").toString()));

	        if (body.containsKey("maxDiscount")) {
	            Object md = body.get("maxDiscount");
	            v.setMaxDiscount(md != null && !md.toString().isBlank()
	                    ? Double.parseDouble(md.toString()) : null);
	        }

	        if (body.containsKey("usageLimit"))
	            v.setUsageLimit(Integer.parseInt(body.get("usageLimit").toString()));

	        if (body.containsKey("startDate"))
	            v.setStartDate(LocalDate.parse(body.get("startDate").toString()));

	        if (body.containsKey("endDate"))
	            v.setEndDate(LocalDate.parse(body.get("endDate").toString()));

	        if (body.containsKey("active"))
	            v.setActive(Boolean.parseBoolean(body.get("active").toString()));

	        return v;
	    }
}
