package com.poly.java5.Service;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserVoucher;
import com.poly.java5.Entity.Voucher;
import com.poly.java5.Repository.UserVoucherRepository;
import com.poly.java5.Repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserVoucherService {

    private final UserVoucherRepository userVoucherRepository;
    private final VoucherRepository voucherRepository;

    /**
     * Người dùng nhận một voucher (thêm vào danh sách của họ)
     */
    @Transactional
    public UserVoucher claimVoucher(User user, Integer voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        // Kiểm tra voucher còn hiệu lực chung
        if (!voucher.getComputedStatus().equals("ACTIVE")) {
            throw new RuntimeException("Voucher không còn hiệu lực");
        }

        // Kiểm tra user đã nhận voucher này chưa
        if (userVoucherRepository.findByUserAndVoucher(user, voucher).isPresent()) {
            throw new RuntimeException("Bạn đã nhận voucher này rồi");
        }

        UserVoucher uv = UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .isUsed(false)
                .usedDate(null)
                .build();
        return userVoucherRepository.save(uv);
    }

    /**
     * Lấy danh sách voucher chưa dùng của user
     */
    public List<UserVoucher> getUnusedVouchers(User user) {
        return userVoucherRepository.findUnusedByUser(user);
    }

    /**
     * Lấy tất cả voucher user đã nhận (cả đã dùng và chưa dùng)
     */
    public List<UserVoucher> getAllUserVouchers(User user) {
        return userVoucherRepository.findByUser(user);
    }

    /**
     * Đánh dấu voucher đã được sử dụng (khi đặt hàng thành công)
     */
    @Transactional
    public void useVoucher(User user, Voucher voucher) {
        UserVoucher uv = userVoucherRepository.findByUserAndVoucher(user, voucher)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có voucher này"));

        if (uv.getIsUsed()) {
            throw new RuntimeException("Voucher đã được sử dụng trước đó");
        }

        uv.setIsUsed(true);
        uv.setUsedDate(LocalDateTime.now());
        userVoucherRepository.save(uv);
    }

    /**
     * Kiểm tra user có thể sử dụng voucher này không (chưa dùng và còn hạn)
     */
    public boolean isUsableForUser(User user, Voucher voucher) {
        // Kiểm tra trạng thái chung của voucher
        if (!voucher.getComputedStatus().equals("ACTIVE")) {
            return false;
        }
        // Kiểm tra user đã dùng chưa (giả sử mỗi voucher chỉ dùng 1 lần/user)
        return !userVoucherRepository.existsByUserAndVoucherAndIsUsedTrue(user, voucher);
    }
}