# UniCare – phiên bản Java/XML chỉnh theo cách học trên lớp

Phiên bản này được chỉnh từ bản `UniCare_Firebase(1).rar` bạn gửi ngày 25/10/2026. **Đây không phải một ứng dụng viết lại từ đầu.** Các chức năng kết nối Firebase và các ID view cũ được giữ lại để không làm thay đổi nghiệp vụ hiện có.

## 1. Những kỹ thuật theo bài mẫu của thầy

- **ConstraintLayout** là root của 13 màn hình Activity. Ở các màn hình danh sách, các phần tiêu đề, lọc và RecyclerView được ràng buộc theo chiều dọc; với màn hình dài, ScrollView được đặt bên trong ConstraintLayout.
- **Material 3**: theme `Theme.Material3.DayNight.NoActionBar`; các khung thẻ dùng `MaterialCardView`; nút và ô nhập sử dụng `MaterialButton`, `TextInputLayout`, `TextInputEditText`.
- **mapping() và validateData()**: `AuthActivity`, `ProfileActivity`, `BookingActivity`, `RecordEditorActivity` sử dụng kiểu khai báo và kiểm tra dữ liệu tương tự project quản lý sinh viên của thầy. Lỗi hiển thị tại `TextInputLayout.setError()`.
- **MaterialDatePicker**: dùng cho ngày khám (`BookingActivity`) và ngày sinh (`ProfileActivity`). Ngày chọn từ MaterialDatePicker được xử lý theo UTC để tránh bị lệch một ngày khi hiển thị ở Việt Nam.
- **RecyclerView + Adapter + ViewHolder**: màn hình danh sách bệnh án được tách thành `RecordsActivity`, `RecordAdapter`, `RecordViewHolder` như mẫu `StudentAdapter`, `StudentViewHolder`. Các danh sách khác vốn đã dùng RecyclerView và được giữ nguyên nghiệp vụ.
- **Material dialog + validateData()**: hộp thoại nhập thuốc và chỉ số sức khỏe không tự đóng khi người dùng bỏ trống dữ liệu bắt buộc.

## 2. Những phần giữ lại vì ứng dụng thật cần dùng

Firebase Authentication, Cloud Firestore, Firestore Security Rules, Listener, Transaction chống đặt trùng giờ, xử lý các vai trò bệnh nhân/bác sĩ, lưu lịch khám, bệnh án, thuốc, đánh giá. Đây là phần nghiệp vụ cao hơn bài tập giao diện của thầy, nhưng cần để UniCare hoạt động.

## 3. Mở và chạy trên Android Studio

1. Giải nén ZIP. Trong Android Studio, chọn **File > Open** rồi chọn đúng thư mục `UniCare_Firebase` (có `settings.gradle.kts`).
2. Đảm bảo Android Studio đã cài Android SDK và JDK phù hợp với Gradle của project. Cho phép **Sync Project with Gradle Files**.
3. File `app/google-services.json` được sao chép nguyên từ gói RAR người dùng gửi. Nếu bạn muốn chuyển sang Firebase project khác, thay bằng file tải từ Firebase Console đúng `applicationId`.
4. Chọn máy ảo Android hoặc điện thoại và bấm **Run**.
5. Sử dụng tài khoản test có role `patient` và `doctor`. Firestore Security Rules phải được triển khai trong Firebase Console; file `firestore.rules` trong thư mục project không tự áp dụng lên máy chủ.

**Lưu ý**: Môi trường tạo bộ ZIP không có Android SDK/Android Studio và không tải được các thư viện Gradle, nên chưa thể build APK hoặc xác nhận đã chạy thành công trên thiết bị. Đã kiểm tra cấu trúc XML, tên ID Java/XML và thành phần source. Bạn cần Sync + Run trong Android Studio trước khi dùng để nộp/demonstrate.

## 4. Những file thay đổi nổi bật

`app/src/main/res/values/themes.xml`, các file XML `activity_*.xml` và `item_*.xml`, `AuthActivity.java`, `BookingActivity.java`, `ProfileActivity.java`, `RecordEditorActivity.java`, `RecordsActivity.java`, `MedicationActivity.java`, `HealthVitalsActivity.java`. Có thêm `RecordAdapter.java` và `RecordViewHolder.java`.

**Không đẩy file `app/google-services.json` lên repository công khai nếu chính sách nhóm không cho phép.** File này đã có trong ZIP để bạn kiểm tra với Firebase project hiện tại, nhưng `.gitignore` của project vẫn bỏ qua khi commit.
