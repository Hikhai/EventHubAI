-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: eventhub_db
-- ------------------------------------------------------
-- Server version	8.4.11

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uq_category_name` (`category_name`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'Hội thảo','Các buổi hội thảo chuyên đề, seminar, talkshow',1,'2026-08-20 20:41:32'),(2,'Workshop','Buổi thực hành kỹ năng, hands-on training',1,'2026-08-20 20:41:32'),(3,'Buổi họp','Họp CLB, sinh hoạt định kỳ, meeting nội bộ',1,'2026-08-20 20:41:32'),(4,'Hoạt động ngoại khóa','Picnic, teambuilding, thiện nguyện, outdoor',1,'2026-08-20 20:41:32'),(5,'Cuộc thi','Hackathon, cuộc thi học thuật, sáng tạo',1,'2026-08-20 20:41:32'),(6,'Khác','Sự kiện khác ngoài các nhóm trên',1,'2026-08-20 20:41:32');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_logs`
--

DROP TABLE IF EXISTS `chat_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_logs` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('user','assistant') COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `user_id` (`user_id`),
  KEY `idx_chat_session` (`session_id`),
  CONSTRAINT `chat_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_logs`
--

LOCK TABLES `chat_logs` WRITE;
/*!40000 ALTER TABLE `chat_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `chat_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `events`
--

DROP TABLE IF EXISTS `events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `events` (
  `event_id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `summary_ai` text COLLATE utf8mb4_unicode_ci,
  `image_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_source` enum('UPLOADED','AI_GENERATED','DEFAULT') COLLATE utf8mb4_unicode_ci DEFAULT 'DEFAULT',
  `location` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `registration_deadline` datetime NOT NULL,
  `max_participants` int NOT NULL,
  `current_registered` int NOT NULL DEFAULT '0',
  `avg_rating` decimal(3,1) DEFAULT '0.0',
  `total_reviews` int DEFAULT '0',
  `status` enum('DRAFT','PUBLISHED','CANCELLED','COMPLETED') COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT',
  `category_id` int NOT NULL,
  `created_by` int NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  KEY `created_by` (`created_by`),
  KEY `idx_events_status` (`status`),
  KEY `idx_events_start_time` (`start_time`),
  KEY `idx_events_category` (`category_id`),
  CONSTRAINT `events_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `events_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_deadline_before_start` CHECK ((`registration_deadline` <= `start_time`)),
  CONSTRAINT `chk_end_after_start` CHECK ((`end_time` > `start_time`)),
  CONSTRAINT `chk_max_positive` CHECK ((`max_participants` > 0)),
  CONSTRAINT `chk_rating_range` CHECK ((`avg_rating` between 0.0 and 5.0)),
  CONSTRAINT `chk_registered_non_negative` CHECK ((`current_registered` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `events`
--

LOCK TABLES `events` WRITE;
/*!40000 ALTER TABLE `events` DISABLE KEYS */;
INSERT INTO `events` VALUES (1,'Hội thảo AI trong Giáo dục 2026','Buổi hội thảo chuyên sâu về ứng dụng trí tuệ nhân tạo trong giáo dục đại học. Nội dung gồm: Gemini, ChatGPT, xây dựng lộ trình học tập cá nhân hóa, demo công cụ AI hỗ trợ giảng dạy. Diễn giả đến từ Google Developer Groups và các trường đại học hàng đầu.','Khám phá cách AI thay đổi giáo dục đại học với demo thực tế và chia sẻ từ chuyên gia.','default_hoithao.jpg','DEFAULT','Hội trường A, Tòa nhà Chính','2026-08-28 05:41:32','2026-08-28 08:41:32','2026-08-26 19:41:32',50,9,0.0,0,'PUBLISHED',1,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(2,'Workshop Thiết kế UI/UX Nâng cao với Figma','Khóa học thực hành thiết kế giao diện hiện đại bằng Figma: Design System, Auto Layout, Prototyping, Usability Testing. Phù hợp sinh viên CNTT, Digital Marketing và Design. Có bài tập nhóm và nhận xét trực tiếp từ mentor.','Thực hành UI/UX chuyên sâu trên Figma cùng mentor giàu kinh nghiệm.','default_workshop.jpg','DEFAULT','Phòng Lab 2, Tầng 3','2026-08-31 10:41:32','2026-08-31 14:41:32','2026-08-29 18:41:32',30,10,0.0,0,'PUBLISHED',2,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(3,'Hackathon 24h — Build with AI','Cuộc thi lập trình 24 giờ với chủ đề xây dựng sản phẩm AI thực tế. Đội thi 2-4 người. Ban giám khảo từ startup công nghệ. Giải thưởng tổng giá trị 15 triệu đồng + cơ hội thực tập.','Lập trình xuyên đêm 24h, biến ý tưởng AI thành sản phẩm và săn giải thưởng lớn.','default_cuocthi.jpg','DEFAULT','Trung tâm CNTT, Phòng 401-405','2026-09-04 04:41:32','2026-09-05 04:41:32','2026-09-02 16:41:32',100,8,0.0,0,'PUBLISHED',5,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(4,'Sinh hoạt CLB Tiếng Anh — Presentation Skills','Buổi sinh hoạt định kỳ giúp thành viên luyện kỹ năng thuyết trình tiếng Anh: cấu trúc bài nói, ngôn ngữ cơ thể, xử lý Q&A. Có mini contest và feedback cá nhân.','Luyện thuyết trình tiếng Anh thực chiến, nhận feedback trực tiếp.','default_hoihop.jpg','DEFAULT','Phòng họp D101','2026-08-24 12:41:32','2026-08-24 14:41:32','2026-08-23 17:41:32',25,5,0.0,0,'PUBLISHED',3,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(5,'Teambuilding Xanh — Green Campus Day','Hoạt động ngoại khóa kết hợp dọn vệ sinh khuôn viên, trồng cây và mini games gắn kết. Có phần thưởng cho đội xuất sắc và chứng nhận tình nguyện.','Ngày hội xanh đầy năng lượng: trồng cây, gắn kết và lan tỏa lối sống bền vững.','default_ngoaikhoa.jpg','DEFAULT','Sân vận động trường','2026-09-02 03:41:32','2026-09-02 07:41:32','2026-08-31 14:41:32',80,6,0.0,0,'PUBLISHED',4,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(6,'Workshop Python Data Analysis với Pandas','Buổi thực hành phân tích dữ liệu bằng Python: DataFrame, làm sạch dữ liệu, visualization cơ bản với Matplotlib/Seaborn. Học viên mang laptop cá nhân.','Học phân tích dữ liệu Python thực tế với Pandas chỉ trong một buổi.','default_workshop.jpg','DEFAULT','Phòng C201, Tầng 2','2026-08-30 09:41:32','2026-08-30 13:41:32','2026-08-28 16:41:32',40,6,0.0,0,'PUBLISHED',2,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(7,'Talkshow Khởi nghiệp Sinh viên 2026','Chia sẻ hành trình startup từ ý tưởng đến gọi vốn. Nội dung: tìm product-market fit, xây dựng team, storytelling cho nhà đầu tư. Q&A mở với founder trẻ.','Lắng nghe founder trẻ chia sẻ hành trình khởi nghiệp thật và bài học xương máu.','default_hoithao.jpg','DEFAULT','Hội trường B','2026-09-08 05:41:32','2026-09-08 08:41:32','2026-09-06 18:41:32',120,6,0.0,0,'PUBLISHED',1,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(8,'Cuộc thi Design Sprint 12h','Cuộc thi thiết kế sản phẩm số trong 12 giờ: research, wireframe, UI hi-fi và pitch. Chủ đề năm nay: giải pháp học tập cho sinh viên năm nhất.','Thiết kế - pitching trong 12 giờ, thử thách tư duy sản phẩm của bạn.','default_cuocthi.jpg','DEFAULT','Studio Design, Tòa F','2026-09-10 04:41:32','2026-09-10 16:41:32','2026-09-07 17:41:32',60,0,0.0,0,'PUBLISHED',5,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(9,'Đêm nhạc Acoustic CLB Văn nghệ','Đêm nhạc giao lưu acoustic với các tiết mục do sinh viên biểu diễn. Không gian ấm cúng, có spot mở cho khán giả đăng ký hát.','Thư giãn cuối tuần với đêm acoustic ấm áp do sinh viên tổ chức.','default_other.jpg','DEFAULT','Sân khấu ngoài trời khu A','2026-08-27 15:41:32','2026-08-27 17:41:32','2026-08-26 08:41:32',150,5,0.0,0,'PUBLISHED',6,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(10,'Workshop Git & GitHub cho người mới','Nhập môn Git từ zero: commit, branch, merge, pull request, xử lý conflict. Demo workflow làm việc nhóm thực tế trên GitHub.','Nắm vững Git/GitHub để làm việc nhóm chuyên nghiệp ngay từ năm nhất.','default_workshop.jpg','DEFAULT','Phòng Lab 1, Tầng 2','2026-08-25 10:41:32','2026-08-25 13:41:32','2026-08-24 18:41:32',35,10,0.0,0,'PUBLISHED',2,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(11,'Hội thảo Blockchain & Web3 cho Sinh viên','Giới thiệu kiến thức nền tảng Blockchain, Smart Contract và cơ hội nghề nghiệp Web3. Nội dung đang hoàn thiện, dự kiến mở đăng ký sớm.','Chuẩn bị kiến thức Web3 từ cơ bản đến định hướng nghề nghiệp.','default_hoithao.jpg','DEFAULT','Hội trường C','2026-09-20 05:41:32','2026-09-20 08:41:32','2026-09-15 14:41:32',90,0,0.0,0,'DRAFT',1,1,'2026-08-20 20:41:32',NULL),(12,'Workshop Mobile App với Flutter','Thực hành xây dựng app mobile đa nền tảng bằng Flutter. Roadmap đang soạn, sẽ publish sau khi chốt diễn giả.','Học Flutter hands-on để tự tay dựng app mobile đầu tiên.','default_workshop.jpg','DEFAULT','Phòng Lab Mobile','2026-09-18 09:41:32','2026-09-18 13:41:32','2026-09-14 16:41:32',45,0,0.0,0,'DRAFT',2,1,'2026-08-20 20:41:32',NULL),(13,'Workshop Python Cơ bản cho Người mới bắt đầu','Workshop thực hành Python từ cú pháp cơ bản đến hàm, list, file I/O. Phù hợp sinh viên chưa có nền tảng lập trình.','Nhập môn Python thân thiện, dễ hiểu cho người mới bắt đầu.','default_workshop.jpg','DEFAULT','Phòng C201, Tầng 2','2026-08-11 05:41:32','2026-08-11 08:41:32','2026-08-09 14:41:32',40,6,4.6,5,'COMPLETED',2,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(14,'Hội thảo Kỹ năng Phỏng vấn Xin việc','Chia sẻ cách viết CV, trả lời behavioral questions, mock interview theo nhóm nhỏ. Rất hữu ích cho sinh viên năm cuối.','Nâng cấp kỹ năng phỏng vấn với mock interview thực chiến.','default_hoithao.jpg','DEFAULT','Hội trường A','2026-08-01 10:41:32','2026-08-01 13:41:32','2026-07-30 16:41:32',70,5,4.3,4,'COMPLETED',1,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(15,'Cuộc thi Quiz Công nghệ 2025','Cuộc thi kiến thức công nghệ theo đội. Nhiều câu hỏi về AI, Web, Mobile và cybersecurity. Không khí sôi động, giải thưởng hấp dẫn.','Đấu trí công nghệ theo đội, vừa học vừa chơi cực cuốn.','default_cuocthi.jpg','DEFAULT','Nhà thi đấu đa năng','2026-07-22 04:41:32','2026-07-22 08:41:32','2026-07-19 17:41:32',80,7,4.8,6,'COMPLETED',5,1,'2026-08-20 20:41:32','2026-08-20 20:41:32'),(16,'Workshop Cloud AWS Fundamentals (Đã hủy)','Workshop giới thiệu AWS EC2, S3, IAM. Sự kiện bị hủy do diễn giả bận lịch đột xuất.','Sự kiện cloud AWS đã hủy, sẽ mở lại vào học kỳ sau.','default_workshop.jpg','DEFAULT','Phòng Lab Cloud','2026-09-01 05:41:32','2026-09-01 08:41:32','2026-08-29 14:41:32',50,0,0.0,0,'CANCELLED',2,1,'2026-08-20 20:41:32',NULL);
/*!40000 ALTER TABLE `events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registrations`
--

DROP TABLE IF EXISTS `registrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registrations` (
  `registration_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `event_id` int NOT NULL,
  `status` enum('REGISTERED','CANCELLED') COLLATE utf8mb4_unicode_ci DEFAULT 'REGISTERED',
  `registered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `cancelled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`registration_id`),
  UNIQUE KEY `uq_user_event` (`user_id`,`event_id`),
  KEY `idx_reg_user_id` (`user_id`),
  KEY `idx_reg_event_id` (`event_id`),
  KEY `idx_reg_status` (`status`),
  CONSTRAINT `registrations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `registrations_ibfk_2` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registrations`
--

LOCK TABLES `registrations` WRITE;
/*!40000 ALTER TABLE `registrations` DISABLE KEYS */;
INSERT INTO `registrations` VALUES (1,2,1,'REGISTERED','2026-08-08 20:41:32',NULL),(2,3,1,'REGISTERED','2026-08-09 20:41:32',NULL),(3,4,1,'CANCELLED','2026-08-10 20:41:32','2026-08-12 20:41:32'),(4,5,1,'REGISTERED','2026-08-11 20:41:32',NULL),(5,6,1,'REGISTERED','2026-08-12 20:41:32',NULL),(6,7,1,'REGISTERED','2026-08-13 20:41:32',NULL),(7,8,1,'REGISTERED','2026-08-14 20:41:32',NULL),(8,9,1,'REGISTERED','2026-08-15 20:41:32',NULL),(9,10,1,'REGISTERED','2026-08-16 20:41:32',NULL),(10,11,1,'REGISTERED','2026-08-17 20:41:32',NULL),(11,2,2,'REGISTERED','2026-08-06 20:41:32',NULL),(12,3,2,'REGISTERED','2026-08-07 20:41:32',NULL),(13,5,2,'REGISTERED','2026-08-08 20:41:32',NULL),(14,6,2,'REGISTERED','2026-08-09 20:41:32',NULL),(15,7,2,'REGISTERED','2026-08-10 20:41:32',NULL),(16,8,2,'REGISTERED','2026-08-11 20:41:32',NULL),(17,9,2,'REGISTERED','2026-08-12 20:41:32',NULL),(18,10,2,'REGISTERED','2026-08-13 20:41:32',NULL),(19,12,2,'REGISTERED','2026-08-14 20:41:32',NULL),(20,13,2,'REGISTERED','2026-08-15 20:41:32',NULL),(21,2,3,'CANCELLED','2026-08-05 20:41:32','2026-08-10 20:41:32'),(22,3,3,'REGISTERED','2026-08-06 20:41:32',NULL),(23,4,3,'REGISTERED','2026-08-07 20:41:32',NULL),(24,5,3,'REGISTERED','2026-08-08 20:41:32',NULL),(25,7,3,'REGISTERED','2026-08-09 20:41:32',NULL),(26,9,3,'REGISTERED','2026-08-11 20:41:32',NULL),(27,11,3,'REGISTERED','2026-08-12 20:41:32',NULL),(28,14,3,'REGISTERED','2026-08-14 20:41:32',NULL),(29,15,3,'REGISTERED','2026-08-16 20:41:32',NULL),(30,4,4,'REGISTERED','2026-08-15 20:41:32',NULL),(31,6,4,'REGISTERED','2026-08-16 20:41:32',NULL),(32,8,4,'REGISTERED','2026-08-17 20:41:32',NULL),(33,10,4,'REGISTERED','2026-08-18 20:41:32',NULL),(34,12,4,'REGISTERED','2026-08-19 20:41:32',NULL),(35,2,5,'REGISTERED','2026-08-13 20:41:32',NULL),(36,3,5,'REGISTERED','2026-08-14 20:41:32',NULL),(37,5,5,'REGISTERED','2026-08-15 20:41:32',NULL),(38,7,5,'REGISTERED','2026-08-16 20:41:32',NULL),(39,9,5,'REGISTERED','2026-08-17 20:41:32',NULL),(40,11,5,'REGISTERED','2026-08-18 20:41:32',NULL),(41,2,6,'REGISTERED','2026-08-12 20:41:32',NULL),(42,4,6,'REGISTERED','2026-08-13 20:41:32',NULL),(43,6,6,'REGISTERED','2026-08-14 20:41:32',NULL),(44,8,6,'REGISTERED','2026-08-15 20:41:32',NULL),(45,13,6,'REGISTERED','2026-08-17 20:41:32',NULL),(46,15,6,'REGISTERED','2026-08-18 20:41:32',NULL),(47,3,7,'REGISTERED','2026-08-11 20:41:32',NULL),(48,5,7,'REGISTERED','2026-08-12 20:41:32',NULL),(49,7,7,'REGISTERED','2026-08-13 20:41:32',NULL),(50,9,7,'REGISTERED','2026-08-15 20:41:32',NULL),(51,11,7,'REGISTERED','2026-08-16 20:41:32',NULL),(52,14,7,'REGISTERED','2026-08-18 20:41:32',NULL),(53,2,9,'REGISTERED','2026-08-16 20:41:32',NULL),(54,3,9,'REGISTERED','2026-08-17 20:41:32',NULL),(55,4,9,'REGISTERED','2026-08-17 20:41:32',NULL),(56,6,9,'REGISTERED','2026-08-18 20:41:32',NULL),(57,8,9,'REGISTERED','2026-08-19 20:41:32',NULL),(58,2,10,'REGISTERED','2026-08-14 20:41:32',NULL),(59,3,10,'REGISTERED','2026-08-15 20:41:32',NULL),(60,4,10,'REGISTERED','2026-08-15 20:41:32',NULL),(61,5,10,'REGISTERED','2026-08-16 20:41:32',NULL),(62,6,10,'REGISTERED','2026-08-16 20:41:32',NULL),(63,7,10,'REGISTERED','2026-08-17 20:41:32',NULL),(64,8,10,'REGISTERED','2026-08-17 20:41:32',NULL),(65,9,10,'REGISTERED','2026-08-18 20:41:32',NULL),(66,10,10,'REGISTERED','2026-08-18 20:41:32',NULL),(67,11,10,'REGISTERED','2026-08-19 20:41:32',NULL),(68,2,13,'REGISTERED','2026-07-31 20:41:32',NULL),(69,3,13,'REGISTERED','2026-08-01 20:41:32',NULL),(70,4,13,'REGISTERED','2026-08-02 20:41:32',NULL),(71,5,13,'REGISTERED','2026-08-02 20:41:32',NULL),(72,6,13,'REGISTERED','2026-08-03 20:41:32',NULL),(73,7,13,'REGISTERED','2026-08-04 20:41:32',NULL),(74,2,14,'REGISTERED','2026-07-23 20:41:32',NULL),(75,3,14,'REGISTERED','2026-07-24 20:41:32',NULL),(76,8,14,'REGISTERED','2026-07-25 20:41:32',NULL),(77,9,14,'REGISTERED','2026-07-26 20:41:32',NULL),(78,12,14,'REGISTERED','2026-07-27 20:41:32',NULL),(79,2,15,'REGISTERED','2026-07-11 20:41:32',NULL),(80,3,15,'REGISTERED','2026-07-12 20:41:32',NULL),(81,4,15,'REGISTERED','2026-07-13 20:41:32',NULL),(82,5,15,'REGISTERED','2026-07-14 20:41:32',NULL),(83,6,15,'REGISTERED','2026-07-15 20:41:32',NULL),(84,10,15,'REGISTERED','2026-07-16 20:41:32',NULL),(85,11,15,'REGISTERED','2026-07-17 20:41:32',NULL);
/*!40000 ALTER TABLE `registrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `review_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `event_id` int NOT NULL,
  `rating` tinyint NOT NULL,
  `comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  UNIQUE KEY `uq_user_event_review` (`user_id`,`event_id`),
  KEY `idx_reviews_event_id` (`event_id`),
  CONSTRAINT `reviews_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `reviews_ibfk_2` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`),
  CONSTRAINT `chk_rating_valid` CHECK (((`rating` >= 1) and (`rating` <= 5)))
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
INSERT INTO `reviews` VALUES (1,2,13,5,'Workshop rất dễ hiểu, mentor nhiệt tình hỗ trợ từng bước một.','2026-08-11 20:41:32'),(2,3,13,5,'Nội dung rõ ràng, bài tập thực hành sát thực tế, rất đáng tham gia.','2026-08-11 20:41:32'),(3,4,13,4,'Hay và bổ ích, nếu kéo dài thêm 30 phút sẽ tuyệt hơn.','2026-08-12 20:41:32'),(4,5,13,5,'Mình từ zero mà code được chương trình nhỏ sau buổi học. 10 điểm!','2026-08-12 20:41:32'),(5,6,13,4,'Tài liệu tốt, không gian học thoải mái, sẽ tham gia workshop tiếp theo.','2026-08-13 20:41:32'),(6,2,14,5,'Mock interview cực kỳ hữu ích, nhận được góp ý chi tiết về cách trả lời.','2026-08-02 20:41:32'),(7,3,14,4,'Nội dung thực tế, phần CV review rất đáng tiền.','2026-08-03 20:41:32'),(8,8,14,4,'Học được nhiều tips hay, mong có thêm buổi nâng cao.','2026-08-03 20:41:32'),(9,9,14,4,'Không khí chuyên nghiệp, diễn giả chia sẻ gần gũi dễ hiểu.','2026-08-04 20:41:32'),(10,2,15,5,'Cuộc thi siêu vui và kịch tính, kiến thức công nghệ cập nhật!','2026-07-23 20:41:32'),(11,3,15,5,'Ban tổ chức chỉn chu, câu hỏi chất lượng, giải thưởng hấp dẫn.','2026-07-23 20:41:32'),(12,4,15,5,'Team mình học được rất nhiều qua phần tranh biện. Xuất sắc!','2026-07-24 20:41:32'),(13,5,15,4,'Rất đáng tham gia, chỉ hơi đông nên khâu check-in hơi chậm.','2026-07-24 20:41:32'),(14,6,15,5,'Một trong những event hay nhất học kỳ này, sẽ rủ bạn bè tham gia tiếp.','2026-07-25 20:41:32'),(15,10,15,5,'Câu hỏi AI/Web rất chất, MC dẫn chương trình cuốn hút.','2026-07-25 20:41:32');
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('ADMIN','USER') COLLATE utf8mb4_unicode_ci DEFAULT 'USER',
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'Quản trị viên','admin@eventhub.com','$2a$10$jwncVjqEzp0IvqGARqzXJOTUHwdfWVnKSkxNMrjiDDcc/TUjJdvzO','ADMIN',1,'2026-08-20 20:41:32',NULL),(2,'Nguyễn Văn An','an@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(3,'Trần Thị Bình','binh@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(4,'Lê Văn Cường','cuong@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(5,'Phạm Minh Đức','duc@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(6,'Hoàng Thị Em','em@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(7,'Vũ Quốc Phong','phong@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(8,'Đặng Thu Hà','ha@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(9,'Bùi Gia Huy','huy@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(10,'Ngô Bảo Ngọc','ngoc@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(11,'Đỗ Thanh Tùng','tung@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(12,'Lý Mỹ Linh','linh@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(13,'Trịnh Văn Khoa','khoa@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(14,'Mai Anh Thư','thu@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL),(15,'Phan Đức Thịnh','thinh@example.com','$2a$10$/tvnnRZ7qKyb5KcBq4395.p0rtnFg1dhXKo8RYSjpkrRmvY.b3PWK','USER',1,'2026-08-20 20:41:32',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'eventhub_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-20 20:46:06
