-- =============================================================================
-- SCRIPT TẠO DATABASE VÀ BẢNG CHO SQL SERVER (KHÓA NGOẠI NẰM DƯỚI MỖI BẢNG)
-- =============================================================================

CREATE DATABASE BookStoreee;
GO

USE BookStoreee;
GO

-- =============================================================================
-- 1. BẢNG KHÔNG PHỤ THUỘ KHÓA NGOẠI
-- =============================================================================

-- Bảng Người dùng
CREATE TABLE Users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password NVARCHAR(100) NOT NULL,
    email NVARCHAR(100) NOT NULL UNIQUE,
    full_name NVARCHAR(100) NULL,
    phone NVARCHAR(20) NULL,
    address NVARCHAR(255) NULL,
    avatar NVARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    active BIT NOT NULL DEFAULT 1,
    created_date DATETIME2 NULL,
    reset_otp NVARCHAR(255) NULL,
    reset_otp_expiry DATETIME2 NULL,
    lifetime_value DECIMAL(12,2) DEFAULT 0.00,
    customer_rank VARCHAR(20) DEFAULT 'BRONZE'
);
GO

-- Bảng Thể loại
CREATE TABLE Categories (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    image_url NVARCHAR(500) NULL
);
GO

-- Bảng Tác giả
CREATE TABLE authors (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    email NVARCHAR(255) NULL
);
GO

-- Bảng Nhà xuất bản
CREATE TABLE publishers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    address NVARCHAR(255) NULL,
    phone NVARCHAR(20) NULL,
    active BIT DEFAULT 1
);
GO

-- Bảng Voucher
CREATE TABLE vouchers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENT',
    discount_value FLOAT NOT NULL,
    minoder_value FLOAT NOT NULL DEFAULT 0.0,
    max_discount FLOAT NULL,
    quantity INT NOT NULL DEFAULT 100,
    used_count INT NOT NULL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status BIT NOT NULL DEFAULT 1
);
GO

-- Bảng Banner
CREATE TABLE banners (
    id INT IDENTITY(1,1) PRIMARY KEY,
    title NVARCHAR(255) NULL,
    description NVARCHAR(1000) NULL,
    image_url NVARCHAR(1000) NOT NULL,
    link NVARCHAR(500) NULL,
    active BIT DEFAULT 1,
    position INT NULL,
    start_date DATE NULL,
    end_date DATE NULL
);
GO

-- Bảng Ngôn ngữ hệ thống TTS
CREATE TABLE System_Language (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name NVARCHAR(100) NOT NULL
);
GO

-- Bảng Đơn thanh toán PayOS
CREATE TABLE payment_orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_code BIGINT NOT NULL UNIQUE,
    amount INT NOT NULL,
    description NVARCHAR(25) NULL,
    status VARCHAR(20) NOT NULL,
    checkout_url NVARCHAR(500) NULL,
    qr_code NVARCHAR(500) NULL,
    created_at DATETIME2 NULL,
    paid_at DATETIME2 NULL
);
GO

-- Bảng Thanh toán tổng hợp
CREATE TABLE Payment (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    orderId NVARCHAR(255) NOT NULL,
    amount FLOAT NOT NULL,
    status NVARCHAR(255) NOT NULL,
    paymentGateway NVARCHAR(255) NULL
);
GO

-- Bảng Rút tiền Seller
CREATE TABLE Payout (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sellerId NVARCHAR(255) NOT NULL,
    amount FLOAT NOT NULL,
    payoutDate DATE NOT NULL,
    status NVARCHAR(255) NULL
);
GO

-- Bảng Hoa hồng Seller
CREATE TABLE Commission (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sellerId NVARCHAR(255) NOT NULL,
    commissionAmount FLOAT NOT NULL
);
GO

-- Bảng Sản phẩm nổi bật
CREATE TABLE FeaturedProduct (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    productId NVARCHAR(255) NOT NULL,
    isFeatured BIT NOT NULL DEFAULT 0
);
GO

-- Bảng Khuyến mãi
CREATE TABLE promotions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    discount_value DECIMAL(5,2) NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    status BIT DEFAULT 1,
    usage_limit INT NULL,
    used_count INT DEFAULT 0,
    apply_type VARCHAR(20) NULL
);
GO


-- =============================================================================
-- 2. BẢNG PHỤ THUỘ CẤP 1 (KHÓA NGOẠI NẰM Ở CUỐI MỖI BẢNG)
-- =============================================================================

-- Bảng Địa chỉ người dùng
CREATE TABLE user_address (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    receiver_name NVARCHAR(255) NULL,
    receiver_phone VARCHAR(20) NULL,
    province_id INT NOT NULL,
    province_name NVARCHAR(100) NULL,
    district_id INT NOT NULL,
    ward_code VARCHAR(50) NULL,
    ward_name NVARCHAR(100) NULL,
    street NVARCHAR(255) NULL,
    is_default BIT NULL,

    CONSTRAINT FK_user_address_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE SET NULL
);
GO

-- Bảng Sách
CREATE TABLE Books (
    id INT IDENTITY(1,1) PRIMARY KEY,
    title NVARCHAR(200) NOT NULL,
    isbn VARCHAR(20) NULL,
    price DECIMAL(10,2) NULL,
    stock_quantity INT NOT NULL,
    image_url NVARCHAR(500) NULL,
    description NVARCHAR(MAX) NULL,
    active BIT DEFAULT 1,
    deleted BIT DEFAULT 0,
    created_date DATETIME2 NULL,
    book_type VARCHAR(20) NULL,
    category_id INT NULL,
    seller_id INT NULL,

    CONSTRAINT FK_Books_Categories FOREIGN KEY (category_id) REFERENCES Categories(id),
    CONSTRAINT FK_Books_Users_Seller FOREIGN KEY (seller_id) REFERENCES Users(id)
);
GO

-- Bảng liên kết Sách & NXB
CREATE TABLE Book_Publishers (
    book_id INT NOT NULL,
    publisher_id INT NOT NULL,

    PRIMARY KEY (book_id, publisher_id),
    CONSTRAINT FK_BookPublishers_Books FOREIGN KEY (book_id) REFERENCES Books(id) ON DELETE CASCADE,
    CONSTRAINT FK_BookPublishers_publishers FOREIGN KEY (publisher_id) REFERENCES publishers(id) ON DELETE CASCADE
);
GO

-- Bảng liên kết Sách & Tác giả
CREATE TABLE Book_Authors (
    book_id INT NOT NULL,
    author_id BIGINT NOT NULL,

    PRIMARY KEY (book_id, author_id),
    CONSTRAINT FK_BookAuthors_Books FOREIGN KEY (book_id) REFERENCES Books(id) ON DELETE CASCADE,
    CONSTRAINT FK_BookAuthors_authors FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
);
GO

-- Bảng Chương sách
CREATE TABLE Book_Chapter (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    chapter_number INT NULL,
    title NVARCHAR(255) NULL,
    content_text NVARCHAR(MAX) NULL,
    book_id INT NULL,

    CONSTRAINT FK_BookChapter_Books FOREIGN KEY (book_id) REFERENCES Books(id) ON DELETE CASCADE
);
GO

-- Bảng Giọng đọc AI TTS
CREATE TABLE TTS_Voice (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    voice_name NVARCHAR(255) NULL,
    narrator_code VARCHAR(50) NULL,
    language_id BIGINT NULL,

    CONSTRAINT FK_TTSVoice_SystemLanguage FOREIGN KEY (language_id) REFERENCES System_Language(id)
);
GO

-- Bảng Voucher người dùng lưu
CREATE TABLE User_Vouchers (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    voucher_id INT NOT NULL,
    is_used BIT NOT NULL DEFAULT 0,
    used_date DATETIME2 NULL,

    CONSTRAINT FK_UserVouchers_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    CONSTRAINT FK_UserVouchers_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE
);
GO

-- Bảng Đơn hàng
CREATE TABLE Orders (
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_code VARCHAR(20) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    customer_name NVARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_address NVARCHAR(500) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NULL,
    status VARCHAR(20) NULL,
    payment_status VARCHAR(20) NULL,
    order_date DATETIME2 NULL,
    order_type VARCHAR(20) NULL,
    transaction_no VARCHAR(100) NULL,
    cancel_reason NVARCHAR(MAX) NULL,
    shipping_fee DECIMAL(10,2) NULL,
    discount_amount DECIMAL(10,2) NULL,
    member_discount DECIMAL(10,2) NULL,
    voucher_id INT NULL,
    delivered_at DATETIME2 NULL,
    completed_at DATETIME2 NULL,

    CONSTRAINT FK_Orders_Users FOREIGN KEY (user_id) REFERENCES Users(id),
    CONSTRAINT FK_Orders_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id)
);
GO

-- Bảng Giỏ hàng
CREATE TABLE Cart (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_date DATETIME2 NULL,
    updated_date DATETIME2 NULL,

    CONSTRAINT FK_Cart_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);
GO

-- Bảng Danh sách yêu thích
CREATE TABLE Wishlist (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    added_date DATETIME2 NULL,

    CONSTRAINT FK_Wishlist_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    CONSTRAINT FK_Wishlist_Books FOREIGN KEY (book_id) REFERENCES Books(id) ON DELETE CASCADE
);
GO

-- Bảng Đánh giá & Bình luận
CREATE TABLE Reviews (
    id INT IDENTITY(1,1) PRIMARY KEY,
    book_id INT NOT NULL,
    user_id INT NOT NULL,
    rating INT NOT NULL,
    comment NVARCHAR(1000) NULL,
    review_date DATETIME2 NULL,

    CONSTRAINT FK_Reviews_Books FOREIGN KEY (book_id) REFERENCES Books(id) ON DELETE CASCADE,
    CONSTRAINT FK_Reviews_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
);
GO

-- Bảng Chi tiết khuyến mãi
CREATE TABLE promotion_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    promotion_id INT NOT NULL,
    book_id INT NULL,
    category_id INT NULL,

    CONSTRAINT FK_PromotionDetails_promotions FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE,
    CONSTRAINT FK_PromotionDetails_Books FOREIGN KEY (book_id) REFERENCES Books(id),
    CONSTRAINT FK_PromotionDetails_Categories FOREIGN KEY (category_id) REFERENCES Categories(id)
);
GO

-- Bảng Nhật ký thao tác
CREATE TABLE activity_log (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    activity_type NVARCHAR(255) NULL,
    description NVARCHAR(MAX) NULL,
    ip_address VARCHAR(50) NULL,
    created_date DATETIME2 NULL,

    CONSTRAINT FK_ActivityLog_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE SET NULL
);
GO

-- Bảng Định dạng sách
CREATE TABLE Book_Format (
    id INT IDENTITY(1,1) PRIMARY KEY,
    book_id INT NOT NULL,
    format_type VARCHAR(50) NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    original_price DECIMAL(18,2) NULL,
    active BIT NOT NULL DEFAULT 1,

    CONSTRAINT UQ_BookFormat_Book_Type UNIQUE (book_id, format_type),
    CONSTRAINT FK_BookFormat_Books FOREIGN KEY (book_id) REFERENCES Books(id) ON DELETE CASCADE
);
GO


-- =============================================================================
-- 3. BẢNG PHỤ THUỘ CẤP 2 (KHÓA NGOẠI NẰM Ở CUỐI MỖI BẢNG)
-- =============================================================================

-- Bảng Audio Book
CREATE TABLE AUDIO_BOOK (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    audio_url NVARCHAR(MAX) NULL,
    duration_seconds INT NULL,
    tts_status VARCHAR(20) NULL,
    task_id NVARCHAR(255) NULL,
    chapter_id BIGINT NULL,
    language_id BIGINT NULL,
    sequence_order INT NULL,
    is_outdated BIT DEFAULT 0,

    CONSTRAINT FK_AudioBook_BookChapter FOREIGN KEY (chapter_id) REFERENCES Book_Chapter(id) ON DELETE CASCADE,
    CONSTRAINT FK_AudioBook_TTSVoice FOREIGN KEY (language_id) REFERENCES TTS_Voice(id)
);
GO

-- Bảng Tiến trình nghe Audio
CREATE TABLE AUDIO_PLAYBACK_PROGRESS (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    chapter_id BIGINT NOT NULL,
    segment_index INT NULL,
    current_time_seconds FLOAT NULL,
    playback_rate FLOAT NULL,
    updated_at DATETIME2 NULL,

    CONSTRAINT UQ_AudioPlaybackProgress_User_Book UNIQUE (user_id, book_id),
    CONSTRAINT FK_AudioPlayback_Users FOREIGN KEY (user_id) REFERENCES Users(id),
    CONSTRAINT FK_AudioPlayback_Books FOREIGN KEY (book_id) REFERENCES Books(id),
    CONSTRAINT FK_AudioPlayback_BookChapter FOREIGN KEY (chapter_id) REFERENCES Book_Chapter(id)
);
GO

-- Bảng Chi tiết đơn hàng
CREATE TABLE OrderDetails (
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL,
    book_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,

    CONSTRAINT FK_OrderDetails_Orders FOREIGN KEY (order_id) REFERENCES Orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetails_Books FOREIGN KEY (book_id) REFERENCES Books(id)
);
GO

-- Bảng Chi tiết giỏ hàng
CREATE TABLE cart_detail (
    id INT IDENTITY(1,1) PRIMARY KEY,
    cart_id INT NOT NULL,
    book_id INT NOT NULL,
    quantity INT NULL,
    price DECIMAL(18,2) NULL,
    selected BIT NOT NULL DEFAULT 0,

    CONSTRAINT FK_CartDetail_Cart FOREIGN KEY (cart_id) REFERENCES Cart(id) ON DELETE CASCADE,
    CONSTRAINT FK_CartDetail_Books FOREIGN KEY (book_id) REFERENCES Books(id)
);
GO

-- Bảng Tủ sách cá nhân
CREATE TABLE User_Library (
    id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    variant_id INT NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    purchased_at DATETIME2 NULL,

    CONSTRAINT UQ_UserLibrary_User_Book_Variant UNIQUE (user_id, book_id, variant_id),
    CONSTRAINT FK_UserLibrary_Users FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE,
    CONSTRAINT FK_UserLibrary_Books FOREIGN KEY (book_id) REFERENCES Books(id),
    CONSTRAINT FK_UserLibrary_BookFormat FOREIGN KEY (variant_id) REFERENCES Book_Format(id)
);
GO
