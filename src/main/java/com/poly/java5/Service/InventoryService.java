package com.poly.java5.Service;

import com.poly.java5.DTO.InventoryLogDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.InventoryLog;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.InventoryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

	@Autowired
	private InventoryLogRepository inventoryLogRepository;

	@Autowired
	private BookRepository bookRepository;

	// Lấy tất cả lịch sử giao dịch kho
	public List<InventoryLog> findAllLogs() {
		return inventoryLogRepository.findAllByOrderByLogDateDesc();
	}

	// Nhập kho: tăng số lượng sách + ghi log
	@Transactional
	public void importStock(Long bookId, int quantity, String note) {
		// ✅ Ép kiểu Long → Integer vì BookRepository dùng Integer làm ID
		Integer id = bookId.intValue();

		Book book = bookRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy sách ID: " + id));

		// Cộng số lượng
		book.setQuantity(book.getQuantity() + quantity);
		bookRepository.save(book);

		// Ghi log nhập kho
		InventoryLog log = new InventoryLog();
		log.setBook(book);
		log.setChangeAmount(quantity);
		log.setType("IMPORT");
		log.setNote(note);
		log.setLogDate(LocalDateTime.now());
		inventoryLogRepository.save(log);
	}

	// Xuất kho khi bán hàng
	@Transactional
	public void exportStock(Long bookId, int quantity) {
		Integer id = bookId.intValue();

		Book book = bookRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy sách ID: " + id));

		if (book.getQuantity() < quantity) {
			throw new RuntimeException("Không đủ hàng trong kho");
		}

		book.setQuantity(book.getQuantity() - quantity);
		bookRepository.save(book);

		InventoryLog log = new InventoryLog();
		log.setBook(book);
		log.setChangeAmount(quantity);
		log.setType("EXPORT");
		log.setNote("Xuất bán");
		log.setLogDate(LocalDateTime.now());
		inventoryLogRepository.save(log);
	}
	// Trong InventoryService, thêm các phương thức sau:

	// Lấy tất cả logs dưới dạng DTO
	public List<InventoryLogDTO> findAllLogsDTO() {
		List<InventoryLog> logs = inventoryLogRepository.findAllByOrderByLogDateDesc();
		return logs.stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	// Chuyển đổi từ Entity sang DTO
	private InventoryLogDTO convertToDTO(InventoryLog entity) {
		InventoryLogDTO dto = new InventoryLogDTO();
		dto.setId(entity.getId().longValue()); 
		dto.setBookId(entity.getBook().getId().longValue());
		dto.setBookTitle(entity.getBook().getTitle());
		dto.setChangeAmount(entity.getChangeAmount());
		dto.setType(entity.getType());
		dto.setNote(entity.getNote());
		dto.setLogDate(entity.getLogDate());
		return dto;
	}
}