package com.poly.java5.Controller;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.DTO.CategoryDTO;
import com.poly.java5.DTO.CategoryDetailDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Category;
import com.poly.java5.Repository.CategoryRepository;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.BookFormatRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private CategoryRepository catRepo;

	@Autowired
	private BookRepository bookRepo;

	@Autowired
	private BookFormatRepository bookFormatRepo;

	// Đường dẫn lưu ảnh: thư mục static trong resources (giống như sách)
	private static final String UPLOAD_DIR = "src/main/resources/static/uploads/categories/";

	// Helper lưu file và trả về tên file
	private String saveImage(MultipartFile file) throws IOException {
		Path uploadPath = Paths.get(UPLOAD_DIR);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}
		String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
		Path filePath = uploadPath.resolve(filename);
		Files.copy(file.getInputStream(), filePath);
		return filename;
	}

	private java.util.List<BookDTO> convertToDTOBulk(java.util.List<Book> books) {
		if (books == null || books.isEmpty()) return new java.util.ArrayList<>();
		java.util.List<Integer> ids = books.stream().map(Book::getId).collect(Collectors.toList());

		java.util.Map<Integer, Long> soldMap = new java.util.HashMap<>();
		if (bookRepo != null) {
			java.util.List<Object[]> soldData = bookRepo.getSoldCountByBookIds(ids);
			for (Object[] obj : soldData) {
				soldMap.put((Integer) obj[0], ((Number) obj[1]).longValue());
			}
		}

		java.util.Map<Integer, java.math.BigDecimal> audioMap = new java.util.HashMap<>();
		if (bookFormatRepo != null) {
			java.util.List<com.poly.java5.Entity.BookFormat> formats = bookFormatRepo.findByBookIdInAndFormatType(ids, "AUDIO");
			for (com.poly.java5.Entity.BookFormat f : formats) {
				audioMap.put(f.getBook().getId(), f.getPrice());
			}
		}

		return books.stream().map(b -> {
			BookDTO dto = new BookDTO();
			dto.setId(b.getId());
			dto.setTitle(b.getTitle());
			dto.setPrice(b.getPrice());
			dto.setQuantity(b.getQuantity());
			dto.setImageUrl(b.getImageUrl());
			dto.setAuthorName(b.getAuthor() != null ? b.getAuthor().getName() : null);
			dto.setActive(b.getActive());
			dto.setSoldCount(soldMap.getOrDefault(b.getId(), 0L));
			dto.setAudioPrice(audioMap.get(b.getId()));
			return dto;
		}).collect(Collectors.toList());
	}

	// GET ALL
	@GetMapping
	public ResponseEntity<List<CategoryDetailDTO>> getAll() {
		List<Category> categories = catRepo.findAllWithBooks();
		List<CategoryDetailDTO> dtos = categories.stream().map(category -> {
			List<BookDTO> bookDTOs = convertToDTOBulk(category.getBooks());
			CategoryDetailDTO dto = new CategoryDetailDTO();
			dto.setId(category.getId());
			dto.setName(category.getName());
			dto.setImageUrl(category.getImageUrl());
			dto.setBooks(bookDTOs);
			dto.setBookCount((long) bookDTOs.size());
			return dto;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	// GET BY ID (chi tiết có sách)
	@GetMapping("/{id}")
	public ResponseEntity<?> viewCategory(@PathVariable Integer id) {
		Category category = em.find(Category.class, id);
		if (category == null) {
			return ResponseEntity.notFound().build();
		}
		List<Book> books = em.createQuery("SELECT b FROM Book b WHERE b.category.id = :cid", Book.class)
				.setParameter("cid", id).getResultList();
		CategoryDetailDTO dto = new CategoryDetailDTO();
		dto.setId(category.getId());
		dto.setName(category.getName());
		dto.setImageUrl(category.getImageUrl());
		List<BookDTO> bookDTOs = convertToDTOBulk(books);
		dto.setBooks(bookDTOs);
		return ResponseEntity.ok(dto);
	}

	// GET BY ID (chỉ category)
	@GetMapping("/{id}/books")
	public ResponseEntity<CategoryDTO> getOne(@PathVariable Integer id) {
		return catRepo.findById(id)
				.map(cat -> ResponseEntity.ok(new CategoryDTO(cat.getId(), cat.getName(), cat.getImageUrl())))
				.orElse(ResponseEntity.notFound().build());
	}

	// CREATE với upload ảnh
	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<?> create(@RequestParam("name") String name,
			@RequestParam(value = "image", required = false) MultipartFile image) {
		Category category = new Category();
		category.setName(name);
		if (image != null && !image.isEmpty()) {
			try {
				String filename = saveImage(image);
				category.setImageUrl("/uploads/categories/" + filename);
			} catch (IOException e) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi lưu ảnh: " + e.getMessage());
			}
		}
		Category saved = catRepo.save(category);
		CategoryDTO response = new CategoryDTO(saved.getId(), saved.getName(), saved.getImageUrl());
		return ResponseEntity.ok(response);
	}

	// UPDATE với upload ảnh
	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	public ResponseEntity<?> update(@PathVariable Integer id, @RequestParam("name") String name,
			@RequestParam(value = "image", required = false) MultipartFile image) {
		return catRepo.findById(id).map(old -> {
			old.setName(name);
			if (image != null && !image.isEmpty()) {
				try {
					String filename = saveImage(image);
					old.setImageUrl("/uploads/categories/" + filename);
				} catch (IOException e) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body("Lỗi lưu ảnh: " + e.getMessage());
				}
			}
			Category updated = catRepo.save(old);
			CategoryDTO response = new CategoryDTO(updated.getId(), updated.getName(), updated.getImageUrl());
			return ResponseEntity.ok(response);
		}).orElse(ResponseEntity.notFound().build());
	}

	// DELETE
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Integer id) {
		try {
			catRepo.deleteById(id);
			return ResponseEntity.ok("Xóa thành công!");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Không thể xóa thể loại đang có sách!");
		}
	}
    
    @Autowired
    private com.poly.java5.Service.OrderService debugOrderService;
    
    @GetMapping("/debug/orders")
    public ResponseEntity<?> debugOrders() {
        try {
            return ResponseEntity.ok(debugOrderService.findAdminOrdersWithPriority(null).stream()
                    .map(o -> java.util.Map.of("id", o.getId(), "orderCode", o.getOrderCode(), "orderType", o.getOrderType() != null ? o.getOrderType() : "NULL"))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    @GetMapping("/debug/user-orders")
    public ResponseEntity<?> debugUserOrders(@RequestParam Integer userId, @RequestParam String bookType, @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(debugOrderService.findOrdersByUser(userId, status, bookType).stream()
                    .map(o -> java.util.Map.of("id", o.getId(), "orderCode", o.getOrderCode(), "orderType", o.getOrderType() != null ? o.getOrderType() : "NULL"))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/debug/real-orders")
    public ResponseEntity<?> debugRealOrders(@RequestParam Integer userId, @RequestParam(required = false) String bookType, @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(debugOrderService.findOrdersByUser(userId, status, bookType).stream()
                    .map(o -> java.util.Map.of("id", o.getId(), "orderCode", o.getOrderCode(), "orderType", o.getOrderType() != null ? o.getOrderType() : "NULL"))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

}