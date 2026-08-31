package com.poly.java5.Controller;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Author;
import com.poly.java5.Entity.Publisher;
import com.poly.java5.Entity.BookFormat;
import com.poly.java5.Service.BookService;
import com.poly.java5.Repository.BookFormatRepository;
import com.poly.java5.ai.service.BookIndexingService;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/books")

public class AdminBooksApiController {

	@Autowired
	private BookService bookService;

	@Autowired
	private BookFormatRepository bookFormatRepository;

	@Autowired
	private BookIndexingService bookIndexingService;

	@Autowired
	private com.poly.java5.Service.ImageSearchService imageSearchService;

	@Autowired
	private com.poly.java5.Repository.AuthorRepository authorRepository;

	@Autowired
	private com.poly.java5.Repository.PublisherRepository publisherRepository;

	@Autowired
	private com.poly.java5.Repository.UserLibraryRepository userLibraryRepository;

	private static final String UPLOAD_DIR = "src/main/resources/static/uploads/books/";

	// ================= GET ALL =================
	@GetMapping
	public ResponseEntity<List<BookDTO>> getAll() {
		List<BookDTO> dtos = bookService.getAllBooks().stream().map(this::convertToDTO).collect(Collectors.toList());

		return ResponseEntity.ok(dtos);
	}

	// ================= GET ONE =================
	@GetMapping("/{id}")
	public ResponseEntity<?> getOne(@PathVariable Integer id) {
		Book book = bookService.findById(id);

		if (book == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}

		return ResponseEntity.ok(convertToDTO(book));
	}

	// ================= CREATE =================
	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<?> create(@Valid @ModelAttribute BookDTO dto, BindingResult result) throws Exception {

		if (result.hasErrors()) {
			return ResponseEntity.badRequest().body(result.getAllErrors());
		}

		Book book = convertToEntity(dto);

		// upload ảnh
		if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
			book.setImageUrl(saveImage(dto.getImageFile()));
		}

		bookService.save(book);

		// Save PHYSICAL format
		BookFormat physical = new BookFormat();
		physical.setBook(book);
		physical.setFormatType("PHYSICAL");
		physical.setPrice(dto.getPrice());
		bookFormatRepository.save(physical);

		// Save AUDIO format if provided
		if (dto.getAudioPrice() != null) {
			BookFormat audio = new BookFormat();
			audio.setBook(book);
			audio.setFormatType("AUDIO");
			audio.setPrice(dto.getAudioPrice());
			bookFormatRepository.save(audio);
		}

		CompletableFuture.runAsync(() -> {
			try {
				bookIndexingService.indexBook(book.getId());
				imageSearchService.updateBookIndex(book.getId(), book.getImageUrl());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return ResponseEntity.ok(convertToDTO(book));
	}

	// ================= UPDATE =================
	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	public ResponseEntity<?> update(@PathVariable Integer id, @Valid @ModelAttribute BookDTO dto, BindingResult result)
			throws Exception {

		if (result.hasErrors()) {
			return ResponseEntity.badRequest().body(result.getAllErrors());
		}

		Book existing = bookService.findById(id);

		if (existing == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}

		// cập nhật dữ liệu
		existing.setTitle(dto.getTitle());
		existing.setIsbn(dto.getIsbn());
		existing.setPublisher(dto.getPublisher());
		existing.setPrice(dto.getPrice());
		existing.setQuantity(dto.getQuantity());
		existing.setActive(dto.getActive());
		existing.setDescription(dto.getDescription());

		// AUTHORS (List<Long>)
		if (dto.getAuthorIds() != null && !dto.getAuthorIds().isEmpty()) {
			List<com.poly.java5.Entity.Author> authors = dto.getAuthorIds().stream()
				.map(authorId -> authorRepository.findById(authorId).orElse(null))
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toList());
			existing.setAuthors(authors);
		} else if (dto.getAuthorId() != null) {
			com.poly.java5.Entity.Author author = authorRepository.findById(dto.getAuthorId()).orElse(null);
			if (author != null) {
				existing.setAuthors(List.of(author));
			}
		}

		// PUBLISHERS (List<Integer>)
		if (dto.getPublisherIds() != null && !dto.getPublisherIds().isEmpty()) {
			List<com.poly.java5.Entity.Publisher> publishers = dto.getPublisherIds().stream()
				.map(pubId -> publisherRepository.findById(pubId).orElse(null))
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toList());
			existing.setPublishers(publishers);
		}

		// CATEGORY (Integer)
		if (dto.getCategoryId() != null) {
			existing.setCategory(bookService.findCategoryById(dto.getCategoryId()));
		}

		// upload ảnh mới
		if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
			existing.setImageUrl(saveImage(dto.getImageFile()));
		}

		bookService.save(existing);

		// Update or create PHYSICAL format
		BookFormat physical = bookFormatRepository.findByBookIdAndFormatType(existing.getId(), "PHYSICAL")
				.orElse(new BookFormat());
		physical.setBook(existing);
		physical.setFormatType("PHYSICAL");
		physical.setPrice(dto.getPrice());
		bookFormatRepository.save(physical);

		// Update or create AUDIO format
		if (dto.getAudioPrice() != null) {
			BookFormat audio = bookFormatRepository.findByBookIdAndFormatType(existing.getId(), "AUDIO")
					.orElse(new BookFormat());
			audio.setBook(existing);
			audio.setFormatType("AUDIO");
			audio.setPrice(dto.getAudioPrice());
			bookFormatRepository.save(audio);
		}

		CompletableFuture.runAsync(() -> {
			try {
				bookIndexingService.indexBook(existing.getId());
				imageSearchService.updateBookIndex(existing.getId(), existing.getImageUrl());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return ResponseEntity.ok(convertToDTO(existing));
	}

	// ================= DELETE =================
	// DELETE (Ẩn sách)
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Integer id) {
		Book book = bookService.findById(id);
		if (book == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}
		book.setActive(false); // Ẩn: active = false
		bookService.save(book);

		CompletableFuture.runAsync(() -> {
			try {
				bookIndexingService.indexBook(id);
				imageSearchService.deleteBookIndex(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return ResponseEntity.ok("Đã ẩn sách thành công");
	}

	// RESTORE (Hiện sách)
	@PutMapping("/{id}/restore")
	public ResponseEntity<?> restore(@PathVariable Integer id) {
		Book book = bookService.findById(id);
		if (book == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}
		book.setActive(true); // Hiện: active = true
		bookService.save(book);

		CompletableFuture.runAsync(() -> {
			try {
				bookIndexingService.indexBook(id);
				imageSearchService.updateBookIndex(book.getId(), book.getImageUrl());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return ResponseEntity.ok("Đã bật sách thành công");
	}

	// UPDATE AUDIO PRICE
	@PutMapping("/{id}/audio-price")
	public ResponseEntity<?> updateAudioPrice(@PathVariable Integer id, @RequestParam java.math.BigDecimal audioPrice) {
		Book existing = bookService.findById(id);
		if (existing == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}
		if (audioPrice == null || audioPrice.compareTo(java.math.BigDecimal.ZERO) < 0) {
			return ResponseEntity.badRequest().body("Giá sách nói phải >= 0");
		}

		BookFormat audio = bookFormatRepository.findByBookIdAndFormatType(existing.getId(), "AUDIO")
				.orElse(new BookFormat());
		audio.setBook(existing);
		audio.setFormatType("AUDIO");
		audio.setPrice(audioPrice);
		bookFormatRepository.save(audio);
		
		CompletableFuture.runAsync(() -> {
			try {
				bookIndexingService.indexBook(existing.getId());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return ResponseEntity.ok(convertToDTO(existing));
	}

	// ================= CONVERT ENTITY -> DTO =================
	private BookDTO convertToDTO(Book book) {
		BookDTO dto = new BookDTO();

		dto.setId(book.getId()); // Integer
		dto.setTitle(book.getTitle());
		dto.setIsbn(book.getIsbn());
		dto.setPublisher(book.getPublisher());
		dto.setPrice(book.getPrice());
		dto.setQuantity(book.getQuantity());
		dto.setActive(book.getActive());
		dto.setDescription(book.getDescription());
		dto.setImageUrl(book.getImageUrl());

		// Authors mapping
		if (book.getAuthors() != null) {
			dto.setAuthorIds(book.getAuthors().stream().map(Author::getId).collect(Collectors.toList()));
			dto.setAuthorNames(book.getAuthors().stream().map(Author::getName).collect(Collectors.toList()));
			if (!book.getAuthors().isEmpty()) {
				dto.setAuthorId(book.getAuthors().get(0).getId());
				dto.setAuthorName(book.getAuthors().get(0).getName());
			}
		} else {
			dto.setAuthorIds(new java.util.ArrayList<>());
			dto.setAuthorNames(new java.util.ArrayList<>());
		}

		// Publishers mapping
		if (book.getPublishers() != null) {
			dto.setPublisherIds(book.getPublishers().stream().map(Publisher::getId).collect(Collectors.toList()));
			dto.setPublisherNames(book.getPublishers().stream().map(Publisher::getName).collect(Collectors.toList()));
			dto.setPublisher(book.getPublishers().stream().map(Publisher::getName).collect(Collectors.joining(", ")));
		} else {
			dto.setPublisherIds(new java.util.ArrayList<>());
			dto.setPublisherNames(new java.util.ArrayList<>());
		}

		// Category (Integer)
		if (book.getCategory() != null) {
			dto.setCategoryId(book.getCategory().getId());
			dto.setCategoryName(book.getCategory().getName());
		}

		// Fetch format prices
		bookFormatRepository.findByBookIdAndFormatType(book.getId(), "AUDIO")
				.ifPresent(f -> dto.setAudioPrice(f.getPrice()));

		// Check if book is purchased by any user
		dto.setIsPurchased(userLibraryRepository.existsByBook_Id(book.getId()));

		return dto;
	}

	// ================= CONVERT DTO -> ENTITY =================
	private Book convertToEntity(BookDTO dto) {
		Book book = new Book();

		book.setTitle(dto.getTitle());
		book.setIsbn(dto.getIsbn());
		book.setPrice(dto.getPrice());
		book.setQuantity(dto.getQuantity());
		book.setActive(dto.getActive());
		book.setDescription(dto.getDescription());

		// Authors mapping
		if (dto.getAuthorIds() != null && !dto.getAuthorIds().isEmpty()) {
			List<Author> authors = dto.getAuthorIds().stream()
				.map(authorId -> authorRepository.findById(authorId).orElse(null))
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toList());
			book.setAuthors(authors);
		} else if (dto.getAuthorId() != null) {
			Author author = authorRepository.findById(dto.getAuthorId()).orElse(null);
			if (author != null) {
				book.setAuthors(List.of(author));
			}
		}

		// Publishers mapping
		if (dto.getPublisherIds() != null && !dto.getPublisherIds().isEmpty()) {
			List<Publisher> publishers = dto.getPublisherIds().stream()
				.map(pubId -> publisherRepository.findById(pubId).orElse(null))
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toList());
			book.setPublishers(publishers);
		}

		// Category (Integer)
		if (dto.getCategoryId() != null) {
			book.setCategory(bookService.findCategoryById(dto.getCategoryId()));
		}

		// ID (Integer)
		if (dto.getId() != null) {
			book.setId(dto.getId());
		}

		return book;
	}

	// ================= SAVE IMAGE =================
	private String saveImage(MultipartFile file) throws Exception {
		String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

		Files.createDirectories(Paths.get(UPLOAD_DIR));

		Files.copy(file.getInputStream(), Paths.get(UPLOAD_DIR + fileName), StandardCopyOption.REPLACE_EXISTING);

		return fileName;
	}
}