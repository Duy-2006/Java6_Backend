package com.poly.java5.Controller;

import com.poly.java5.Service.BookService;
import com.poly.java5.Service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminInventoryApiController {

    @Autowired private BookService      bookService;
    @Autowired private InventoryService inventoryService;

    // GET /api/admin/inventory/books
    @GetMapping("/books")
    public ResponseEntity<?> getBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    // GET /api/admin/inventory/logs
    @GetMapping("/logs")
    public ResponseEntity<?> getLogs() {
        return ResponseEntity.ok(inventoryService.findAllLogs());
    }

    // GET /api/admin/inventory/low-stock
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStock() {
        return ResponseEntity.ok(bookService.findLowStock(10));
    }

    // POST /api/admin/inventory/import
    @PostMapping("/import")
    public ResponseEntity<?> importStock(@RequestBody Map<String, Object> body) {
        // ✅ parse về Long trước, InventoryService sẽ tự ép về Integer
        Long   bookId   = Long.parseLong(body.get("bookId").toString());
        int    quantity = Integer.parseInt(body.get("quantity").toString());
        String note     = body.getOrDefault("note", "").toString();

        inventoryService.importStock(bookId, quantity, note);
        return ResponseEntity.ok(Map.of("message", "Nhập kho thành công"));
    }
}