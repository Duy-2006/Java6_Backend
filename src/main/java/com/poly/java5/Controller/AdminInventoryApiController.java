package com.poly.java5.Controller;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.DTO.ImportRequestDTO;
import com.poly.java5.DTO.InventoryLogDTO;
import com.poly.java5.Service.BookService;
import com.poly.java5.Service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminInventoryApiController {

    @Autowired private BookService      bookService;
    @Autowired private InventoryService inventoryService;

    // GET /api/admin/inventory/books -> trả về List<BookDTO>
    @GetMapping("/books")
    public ResponseEntity<List<BookDTO>> getBooks() {
        // Giả sử bookService.getAllBooksDTO() đã convert entity -> BookDTO
        // và set imageFile = null vì không cần upload ở đây
        List<BookDTO> books = bookService.getAllBooksDTO();
        return ResponseEntity.ok(books);
    }

    // GET /api/admin/inventory/logs -> trả về List<InventoryLogDTO>
    @GetMapping("/logs")
    public ResponseEntity<List<InventoryLogDTO>> getLogs() {
        List<InventoryLogDTO> logs = inventoryService.findAllLogsDTO();
        return ResponseEntity.ok(logs);
    }

    // GET /api/admin/inventory/low-stock -> trả về List<BookDTO> (quantity < 10)
    @GetMapping("/low-stock")
    public ResponseEntity<List<BookDTO>> getLowStock() {
        List<BookDTO> lowStockBooks = bookService.findLowStockDTO(10);
        return ResponseEntity.ok(lowStockBooks);
    }

    // POST /api/admin/inventory/import -> nhận ImportRequestDTO thay vì Map
    @PostMapping("/import")
    public ResponseEntity<?> importStock(@Valid @RequestBody ImportRequestDTO request) {
        inventoryService.importStock(
            request.getBookId(), 
            request.getQuantity(), 
            request.getNote() != null ? request.getNote() : ""
        );
        return ResponseEntity.ok(Map.of("message", "Nhập kho thành công"));
    }
}