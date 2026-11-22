# 🥬 PantryFlow — Nội trợ thảnh thơi

> **10 giây chụp hóa đơn, cả tuần nhàn tênh.**  
> Ứng dụng web (mobile-first) giúp người nội trợ giảm thiểu tối đa thời gian quản lý thực phẩm trong nhà, không còn nỗi lo quên đồ trong tủ lạnh và chủ động lên thực đơn mỗi ngày.

---

## ✨ Tính năng nổi bật

- **📸 Nhập siêu tốc bằng chụp hóa đơn:** Chụp hoặc tải ảnh hóa đơn siêu thị (WinMart, Bách Hóa Xanh, Co.opmart...), hệ thống tự động nhận diện tên món, tách số lượng/đơn vị và hiển thị dưới dạng **bảng chỉnh sửa trực quan như Excel** để duyệt nhanh trong vài giây.
- **🃏 Nhập thủ công dạng Flashcard 3D:** Giao diện thẻ lật 2 mặt tối ưu cho thao tác một tay trên điện thoại (mặt trước nhập tên, mặt sau chọn số lượng, đơn vị có sẵn hoặc tự nhập).
- **⏱️ Ước tính hạn sử dụng thông minh:** Tự động gợi ý nơi bảo quản (Ngăn mát, Ngăn đông, Tủ bếp) và hạn dùng dự kiến dựa trên chuẩn an toàn thực phẩm (USDA). Hỗ trợ chọn **3 mức độ tươi lúc mua** (*Rất tươi / Đồ chợ / Dùng gấp*) để nhắc nhở an toàn hơn với thực phẩm không nhãn mác.
- **🔔 Nhắc nhở sắp hết hạn:** Tự động đẩy các món sắp đến hạn lên đầu danh sách kèm mã màu trực quan (Đỏ / Vàng / Xanh) và tùy chỉnh thời gian nhận thông báo.
- **🍳 Gợi ý món ăn "Giải cứu tủ lạnh":** Tự động ưu tiên chọn các nguyên liệu sắp hết hạn kết hợp với đồ còn sẵn trong nhà để đề xuất công thức nấu ăn nhanh gọn, kèm nút trừ kho trực tiếp sau khi nấu xong.
- **📊 Cập nhật tiêu thụ linh hoạt:** Dễ dàng ghi nhận lượng thực phẩm đã dùng một phần, đã dùng hết hoặc bỏ đi để danh sách luôn phản ánh đúng thực tế trong tủ lạnh.

---

## 🛠️ Công nghệ sử dụng

- **Frontend:** React, TypeScript, Vite
- **Styling & Icons:** Tailwind CSS, Lucide React
- **AI / OCR & Recipe Engine:** Google Gemini API (`@google/genai` - `gemini-2.5-flash` với Structured Output)
- **Lưu trữ dữ liệu:** LocalStorage (hoạt động mượt mà không cần đăng nhập)

---

## 🚀 Hướng dẫn cài đặt & Chạy trên máy (Local)

### 1. Clone dự án
```bash
git clone [https://github.com/](https://github.com/)<ten-github-cua-ban>/pantryflow.git
cd pantryflow
