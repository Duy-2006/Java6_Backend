package com.poly.java5.Service;

import com.poly.java5.Entity.Voucher;
import com.poly.java5.Repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
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
        Voucher v = new Voucher();
        buildFromBody(v, body);
        validateVoucherConstraints(v);
        return voucherRepository.save(v);
    }

    @Transactional
    public void delete(Integer id) {
        if (!voucherRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy voucher ID: " + id);
        }
        voucherRepository.deleteById(id);
    }

    // ── USER: Áp dụng voucher ────────────────────────────────

    public Map<String, Object> applyVoucher(String code, Double orderAmount) {
        Voucher v = voucherRepository
                .findValidVoucher(code.toUpperCase(), LocalDate.now(), orderAmount)
                .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ hoặc đã hết hạn."));

        Double discount = v.calculateDiscount(orderAmount);

        return Map.of(
                "voucherId", v.getId(),
                "code", v.getCode(),
                "discountType", v.getDiscountType(),
                "discount", discount,
                "finalAmount", Math.max(0, orderAmount - discount)
        );
    }

    @Transactional
    public void incrementUsedCount(Integer voucherId) {
        Voucher v = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher."));
        v.setUsedCount(v.getUsedCount() + 1);
        voucherRepository.save(v);
    }

    public List<Voucher> findActiveVouchers() {
        return voucherRepository.findActiveVouchers(LocalDate.now());
    }

    // ── Helper: map body → entity ────────────────────────────
    private Voucher buildFromBody(Voucher v, Map<String, Object> body) {
        if (body.containsKey("discountType"))
            v.setDiscountType(body.get("discountType").toString());

        if (body.containsKey("discountValue")) {
            double discountValue = Double.parseDouble(body.get("discountValue").toString());
            v.setDiscountValue(discountValue);
            Map<String, Double> suggestion = suggestVoucherSettings(discountValue);
            if (!body.containsKey("minOrderValue")) {
                v.setMinOrderValue(suggestion.get("minOrderValue"));
            }
            if (!body.containsKey("maxDiscount")) {
                v.setMaxDiscount(suggestion.get("maxDiscount"));
            }
        }

        if (body.containsKey("minOrderValue"))
            v.setMinOrderValue(Double.parseDouble(body.get("minOrderValue").toString()));

        if (body.containsKey("maxDiscount")) {
            Object md = body.get("maxDiscount");
            v.setMaxDiscount(md != null && !md.toString().isBlank() ? Double.parseDouble(md.toString()) : null);
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

    private static Map<String, Double> suggestVoucherSettings(double percentage) {
        Map<String, Double> map = new HashMap<>();
        if (percentage <= 10) {
            map.put("minOrderValue", 200000.0);
            map.put("maxDiscount", 100000.0);
        } else if (percentage <= 15) {
            map.put("minOrderValue", 300000.0);
            map.put("maxDiscount", 200000.0);
        } else if (percentage <= 20) {
            map.put("minOrderValue", 400000.0);
            map.put("maxDiscount", 250000.0);
        } else if (percentage <= 25) {
            map.put("minOrderValue", 500000.0);
            map.put("maxDiscount", 250000.0);
        } else if (percentage <= 30) {
            map.put("minOrderValue", 600000.0);
            map.put("maxDiscount", 300000.0);
        } else {
            map.put("minOrderValue", 1000000.0);
            map.put("maxDiscount", 500000.0);
        }
        return map;
    }

    // ── Validation ────────────────────────────────────────
    private void validateVoucherConstraints(Voucher v) {
        if (v.getDiscountValue() == null) {
            throw new IllegalArgumentException("Giá trị phần trăm giảm không được để trống.");
        }
        // Phần trăm giảm phải trong khoảng 10% - 50%
        if (v.getDiscountValue() < 10 || v.getDiscountValue() > 50) {
            throw new IllegalArgumentException("Phần trăm giảm phải trong khoảng 10% - 50%.");
        }
        // Tiền giảm tối đa phải trong khoảng 10.000đ - 1.000.000đ (nếu cung cấp)
        if (v.getMaxDiscount() != null) {
            if (v.getMaxDiscount() < 10000) {
                throw new IllegalArgumentException("Tiền giảm tối đa không được dưới 10.000 VND.");
            }
            if (v.getMaxDiscount() > 1000000) {
                throw new IllegalArgumentException("Tiền giảm tối đa không được vượt quá 1.000.000 VND.");
            }
        }
        // Giá trị đơn hàng tối thiểu phải ít nhất 300.000 VND
        if (v.getMinOrderValue() != null && v.getMinOrderValue() < 300000) {
            throw new IllegalArgumentException("Giá trị đơn hàng tối thiểu phải ít nhất 300.000 VND.");
        }
        // Phần trăm giảm không được vượt quá 80%
        if (v.getDiscountValue() != null && v.getDiscountValue() > 80) {
            throw new IllegalArgumentException("Phần trăm giảm không được vượt quá 80%.");
        }
    }
}
