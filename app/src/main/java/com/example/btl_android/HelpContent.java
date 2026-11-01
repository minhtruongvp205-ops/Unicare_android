package com.example.btl_android;

import java.util.ArrayList;
import java.util.List;

/** Nội dung hướng dẫn mẫu của ứng dụng UniCare, có thể sửa lại khi triển khai thực tế. */
public final class HelpContent {
    private HelpContent() {}

    public static class Article {
        public final String title;
        public final String content;
        Article(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    private static void add(List<Article> items, String title, String content) {
        items.add(new Article(title, content));
    }

    public static List<Article> guides() {
        List<Article> items = new ArrayList<>();
        add(items, "Hướng dẫn sử dụng và cài đặt UniCare",
                "1. Cài đặt UniCare từ bản ứng dụng được nhóm phát triển cung cấp.\n\n"
                + "2. Mở ứng dụng và tạo tài khoản hoặc đăng nhập bằng tài khoản đã có.\n\n"
                + "3. Đăng nhập bằng tài khoản bệnh nhân để sử dụng các chức năng đặt khám, xem lịch hẹn và hồ sơ sức khỏe.\n\n"
                + "4. Tại trang chủ, chọn tính năng cần sử dụng. Các mục Hướng dẫn khách hàng, Dịch vụ nổi bật và Bảng giá dịch vụ nằm trong phần tiện ích.\n\n"
                + "5. Vào trang Cá nhân để kiểm tra hoặc cập nhật thông tin.\n\n"
                + "Lưu ý: Đây là ứng dụng bài tập lớn đang trong giai đoạn thử nghiệm. Chỉ sử dụng tài khoản và dữ liệu phù hợp với môi trường thử nghiệm.");

        add(items, "Hướng dẫn đồng bộ hồ sơ người bệnh trên UniCare",
                "1. Đăng nhập đúng tài khoản bệnh nhân đã sử dụng để đặt khám.\n\n"
                + "2. Mở mục Cá nhân, kiểm tra họ tên, số điện thoại và ngày sinh, sau đó nhấn Lưu nếu có chỉnh sửa.\n\n"
                + "3. Vào Hồ sơ sức khỏe hoặc Lịch sử đặt khám để xem dữ liệu đang gắn với tài khoản.\n\n"
                + "4. Các lịch khám và bệnh án do hệ thống lưu trên Firebase được liên kết theo tài khoản đăng nhập. Hãy bật Internet và mở lại màn hình nếu dữ liệu chưa hiển thị.\n\n"
                + "5. Nếu sử dụng tài khoản khác, lịch khám và hồ sơ có thể không xuất hiện.\n\n"
                + "Lưu ý: Phiên bản hiện tại chưa hỗ trợ nhập số hồ sơ giấy từ bệnh viện để tự đồng bộ. Không chia sẻ mật khẩu hoặc thông tin bệnh án cho người lạ.");

        add(items, "Hướng dẫn đặt khám trên UniCare",
                "1. Đăng nhập bằng tài khoản bệnh nhân.\n\n"
                + "2. Tại trang chủ chọn Đặt khám.\n\n"
                + "3. Chọn bác sĩ trong danh sách được hệ thống tải về.\n\n"
                + "4. Chọn ngày khám, giờ khám và nhập lý do khám (ít nhất 3 ký tự).\n\n"
                + "5. Nhấn Đặt khám để gửi yêu cầu.\n\n"
                + "6. Khi tạo lịch thành công, trạng thái ban đầu là Chờ xác nhận (PENDING). Vào Lịch sử đặt khám để theo dõi trạng thái.\n\n"
                + "Nếu khung giờ đã được người khác đặt, ứng dụng sẽ yêu cầu bạn chọn khung giờ khác. Giờ khám phải cách thời điểm hiện tại ít nhất 5 phút.");

        add(items, "Hướng dẫn hủy phiếu đặt khám",
                "1. Đăng nhập tài khoản đã đặt lịch.\n\n"
                + "2. Chọn Lịch sử đặt khám trên trang chủ.\n\n"
                + "3. Tìm lịch có trạng thái Chờ xác nhận hoặc Đã xác nhận.\n\n"
                + "4. Nhấn Hủy lịch trên phiếu muốn hủy.\n\n"
                + "5. Kiểm tra thông báo xác nhận, sau đó chọn Hủy lịch để hoàn tất.\n\n"
                + "6. Kiểm tra trạng thái Đã hủy trong danh sách lịch hẹn.\n\n"
                + "Lịch đã hoàn thành hoặc đã hủy không có nút Hủy lịch. Nếu thao tác thất bại, kiểm tra kết nối mạng và thử lại.");

        add(items, "Hướng dẫn quy trình khám bệnh",
                "Bước 1 - Đặt lịch: Bệnh nhân chọn bác sĩ, ngày giờ và gửi yêu cầu đặt khám trên UniCare.\n\n"
                + "Bước 2 - Xác nhận: Bác sĩ xem lịch được gửi đến và xác nhận hoặc từ chối.\n\n"
                + "Bước 3 - Đến khám: Người bệnh theo dõi trạng thái lịch hẹn và đến cơ sở y tế đã thống nhất với bác sĩ.\n\n"
                + "Bước 4 - Ghi nhận khám: Bác sĩ có thể nhập thông tin bệnh án cho lịch khám trong ứng dụng.\n\n"
                + "Bước 5 - Hoàn thành: Bác sĩ cập nhật trạng thái lịch thành Hoàn thành. Bệnh nhân có thể xem bệnh án và đánh giá sau khám nếu tính năng đã được sử dụng.\n\n"
                + "Lưu ý: Ứng dụng UniCare trong bài tập lớn không thay thế quy trình tiếp nhận, chẩn đoán hoặc cấp cứu của cơ sở y tế thực tế.");

        add(items, "Hướng dẫn thanh toán đơn thuốc trên UniCare",
                "1. Mở đơn thuốc hợp lệ đã được tạo cho bệnh nhân trong hệ thống.\n\n"
                + "2. Tại màn hình Thanh toán, kiểm tra danh sách thuốc, số lượng và số tiền hiển thị.\n\n"
                + "3. Nhập địa chỉ nhận thuốc và chọn phương thức thanh toán.\n\n"
                + "4. Nhấn Xác nhận thanh toán. Nếu hệ thống lưu đơn thành công, ứng dụng sẽ thông báo mã đơn và chuyển đến màn hình hóa đơn.\n\n"
                + "5. Kiểm tra lại chi tiết đơn trước khi xác nhận.\n\n"
                + "Lưu ý: Chức năng đặt mua/thanh toán thuốc trong phiên bản bài tập lớn chỉ là mô phỏng quy trình, không xác nhận đã thu tiền hoặc giao thuốc thực tế. Cần có đơn thuốc hợp lệ để sử dụng.");
        return items;
    }

    public static List<Article> faqs() {
        List<Article> items = new ArrayList<>();
        add(items, "Tôi chưa có tài khoản thì đặt lịch khám được không?",
                "Bạn cần đăng ký hoặc đăng nhập tài khoản bệnh nhân trước khi đặt lịch. Tài khoản bác sĩ được dùng để quản lý lịch khám và bệnh án, không phải để đặt lịch như bệnh nhân.");
        add(items, "Vì sao tôi không thấy danh sách bác sĩ?",
                "Ứng dụng tải danh sách bác sĩ từ Firebase. Hãy kiểm tra Internet, đăng nhập đúng tài khoản bệnh nhân và mở lại màn hình Đặt khám. Nếu danh sách vẫn trống, hệ thống thử nghiệm có thể chưa có dữ liệu bác sĩ.");
        add(items, "Đặt lịch thành công có nghĩa là đã được khám chưa?",
                "Chưa. Lịch mới gửi sẽ ở trạng thái Chờ xác nhận (PENDING). Khi bác sĩ đồng ý, trạng thái sẽ chuyển thành Đã xác nhận (CONFIRMED). Bạn nên kiểm tra trạng thái trước khi đến khám.");
        add(items, "Tôi có thể thay đổi ngày giờ lịch khám không?",
                "Phiên bản hiện tại chưa có nút sửa trực tiếp ngày giờ của lịch đã đặt. Nếu lịch vẫn ở trạng thái có thể hủy, hãy hủy lịch cũ và tạo lịch mới với khung giờ phù hợp.");
        add(items, "Tôi có thể hủy lịch khám khi nào?",
                "Bạn có thể bấm Hủy lịch khi phiếu ở trạng thái Chờ xác nhận hoặc Đã xác nhận. Ứng dụng sẽ hiện hộp thoại để bạn xác nhận trước khi hủy.");
        add(items, "Vì sao lịch khám hoặc bệnh án không hiển thị?",
                "Hãy kiểm tra Internet và chắc chắn bạn đăng nhập bằng đúng tài khoản đã đặt khám. Bệnh án chỉ xuất hiện khi dữ liệu tương ứng đã được bác sĩ tạo và hệ thống lưu thành công.");
        add(items, "UniCare có nhận thanh toán thật không?",
                "Hiện chức năng thanh toán đơn thuốc của phiên bản bài tập lớn là bản mô phỏng và lưu thông tin đơn vào hệ thống thử nghiệm. Không chuyển khoản hoặc cung cấp thông tin thẻ thanh toán dựa trên màn hình demo.");
        add(items, "Bảng giá dịch vụ có phải số tiền tôi chắc chắn phải trả không?",
                "Không. Bảng giá hiển thị trong UniCare là dữ liệu tham khảo từ một văn bản giá công khai năm 2026, không phải báo giá của một cơ sở khám chữa bệnh hợp tác với ứng dụng. Chi phí thực tế tùy cơ sở, dịch vụ, bảo hiểm y tế và các khoản phát sinh.");
        add(items, "Tôi quên mật khẩu hoặc không đăng nhập được thì sao?",
                "Kiểm tra lại email và mật khẩu. Nếu màn hình đăng nhập hỗ trợ khôi phục mật khẩu, hãy làm theo hướng dẫn tại đó. Không chia sẻ mật khẩu hoặc mã xác nhận cho người khác. Nếu hệ thống thử nghiệm chưa có chức năng khôi phục, hãy liên hệ người quản trị dự án.");
        add(items, "Dữ liệu sức khỏe của tôi có được giữ riêng tư không?",
                "Bạn nên chỉ dùng dữ liệu mẫu khi thử nghiệm ứng dụng. Không đăng tải số định danh, ảnh hồ sơ bệnh án thật hoặc thông tin nhạy cảm khi chưa xác nhận chính sách bảo mật và quyền truy cập dữ liệu của hệ thống.");
        add(items, "UniCare có sử dụng được trong trường hợp cấp cứu không?",
                "Không sử dụng tính năng đặt lịch của UniCare để xử lý cấp cứu. Khi có dấu hiệu nguy hiểm hoặc cần trợ giúp y tế khẩn cấp, hãy gọi số cấp cứu 115 hoặc liên hệ cơ sở y tế gần nhất.");
        return items;
    }
}
