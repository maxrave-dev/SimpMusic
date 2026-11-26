# SimpMusic 2.7.0

## Mới
- **Tạo radio**: Tạo → Radio cho bạn chọn nghệ sĩ, bài hát và thể loại, rồi chỉnh độ đa dạng nghệ sĩ, mức khám phá nhạc mới và bộ lọc; bấm Xong là có một radio pha từ tất cả lựa chọn đó. Khi đã đăng nhập, danh sách nghệ sĩ lấy theo gu YouTube Music của chính bạn, và radio nào bạn thích thì lưu lại bằng nút trái tim. ([#2664](https://github.com/maxrave-dev/SimpMusic/issues/2664))
- **Replay Mix**: một thẻ trong Thư viện của bạn phát những bài bạn nghe nhiều nhất tuần này, rồi tới tháng này. Danh sách cập nhật mỗi ngày một lần và phát tiếp như một radio khi hết bài. ([#2664](https://github.com/maxrave-dev/SimpMusic/issues/2664))
- **Xoá lời bài hát**: khi lời đang hiện, menu của bài có mục Xoá lời bài hát. Lời bị xoá khỏi máy này và bản lời đó không hiện lại cho bài đó nữa. Lời của SimpMusic còn được gỡ khỏi máy chủ nếu bạn là người đóng góp, hoặc được bỏ phiếu xuống nếu không phải. ([#2959](https://github.com/maxrave-dev/SimpMusic/issues/2959))
- **Nền lời bài hát chuyển động**: hai công tắc dưới Cài đặt → Giao diện → Kiểu lời bài hát cho các trang lời một nền chuyển động chậm tạo từ ảnh bìa, và nền này còn có thể đập theo tiếng bass của bài. Có trên Android 12 trở lên và trên máy tính. ([#2962](https://github.com/maxrave-dev/SimpMusic/issues/2962))
- **Tìm kiếm trong Cài đặt**: ô tìm kiếm trên cùng trang Cài đặt chỉ hiện những mục khớp với chữ bạn gõ. ([#2960](https://github.com/maxrave-dev/SimpMusic/issues/2960))
- **Thêm nhà cung cấp AI**: Cài đặt → AI giờ có OpenRouter, Groq, DeepSeek, Mistral AI, xAI, Together AI, Fireworks AI, Cerebras và Anthropic, và bạn chọn model từ danh sách model của chính nhà cung cấp, có ô tìm kiếm. ([#2942](https://github.com/maxrave-dev/SimpMusic/issues/2942))
- **Chọn biểu tượng app** (Android): Cài đặt → Giao diện → Biểu tượng app có Liquid Glass, Old-school, Minimalism Light, Minimalism Dark và Pixel. App sẽ đóng khi bạn đổi biểu tượng, và bạn mở lại từ biểu tượng mới. ([#2799](https://github.com/maxrave-dev/SimpMusic/issues/2799), [#2723](https://github.com/maxrave-dev/SimpMusic/issues/2723))
- **Giao diện AMOLED**: Cài đặt → Giao diện → Giao diện có thêm AMOLED, nền đen tuyền, bên cạnh Hệ thống, Tối và Sáng. ([#2774](https://github.com/maxrave-dev/SimpMusic/issues/2774))
- **Toàn màn hình trên máy tính**: bấm F11 để phủ kín màn hình và Esc để thoát. Trên macOS, nút xanh của cửa sổ cũng làm được việc này. ([#2864](https://github.com/maxrave-dev/SimpMusic/issues/2864), [#2794](https://github.com/maxrave-dev/SimpMusic/issues/2794), [#2865](https://github.com/maxrave-dev/SimpMusic/issues/2865))
- **Tắt chữ chạy**: tắt Cài đặt → Giao diện → Cuộn chữ dài thì tên bài dài đứng yên và bị cắt ở mép. ([#196](https://github.com/maxrave-dev/SimpMusic/issues/196), [#2789](https://github.com/maxrave-dev/SimpMusic/issues/2789))

## Cải thiện
- **AutoMix**: đoạn chuyển bài giờ phát đè lên đoạn kết của bài đang phát và không bao giờ bắt đầu trước nửa sau của đoạn drop cuối, nên bạn luôn nghe trọn nửa đầu của drop. Khi Tidal không có nhịp độ của bài, độ dài đoạn chuyển lấy theo nhịp độ SimpMusic tự đo. ([#2966](https://github.com/maxrave-dev/SimpMusic/issues/2966))
- **Playlist trên Android Auto**: playlist, Yêu thích và Đã tải có mục Phát tất cả, bài bạn chạm vào được phát cùng phần còn lại của playlist trong hàng chờ, và playlist local phát đúng thứ tự bạn đã sắp xếp trong app. ([#2930](https://github.com/maxrave-dev/SimpMusic/issues/2930))
- **Menu dạng kính lỏng**: khi bật kính lỏng (liquid glass), menu của bài, của playlist và các bảng nổi khác nằm trên nền kính và chuyển sang nền đặc khi phủ kín màn hình. Chúng cũng theo giao diện của bạn khi mở trên màn tối như Đang phát. ([#2961](https://github.com/maxrave-dev/SimpMusic/issues/2961))
- **Thanh tiêu đề trên macOS**: macOS dùng lại thanh tiêu đề của hệ thống, và thanh này chuyển tối hay sáng theo giao diện của app. ([#2943](https://github.com/maxrave-dev/SimpMusic/issues/2943))
- **Cửa sổ máy tính hẹp**: Thống kê chuyển về một cột và lưới tâm trạng ở Tìm kiếm hiện ít ô hơn khi khung hẹp, ví dụ khi mở cả thanh bên lẫn Đang phát. ([#2964](https://github.com/maxrave-dev/SimpMusic/issues/2964))
- **Bộ chỉnh âm**: mức preamp hạ được tới −30 dB, để nhạc phát nhỏ bên dưới tiếng game hay buổi phát trực tiếp. ([#2577](https://github.com/maxrave-dev/SimpMusic/issues/2577))

## Sửa lỗi
- Android Auto: app crash khi phát một bài từ Trang chủ, hoặc từ kết quả tìm kiếm bằng giọng nói và bằng chữ. ([#2811](https://github.com/maxrave-dev/SimpMusic/issues/2811), [#2857](https://github.com/maxrave-dev/SimpMusic/issues/2857))
- Android Auto: chạm vào một bài trong kết quả tìm kiếm lại phát bài khác, hoặc không phát gì. ([#2445](https://github.com/maxrave-dev/SimpMusic/issues/2445))
- Android Auto: khi gõ tìm kiếm, mỗi ký tự bị lưu thành một mục riêng trong lịch sử tìm kiếm YouTube của bạn. ([#2295](https://github.com/maxrave-dev/SimpMusic/issues/2295))
- Android Auto: không có tiếng khi bạn quay lại xe, phải tạm dừng rồi phát lại mới nghe được. ([#2207](https://github.com/maxrave-dev/SimpMusic/issues/2207))
- Android Auto: Next và Previous chạy hai lần, nên bấm Previous giữa bài lại lùi quá một bài. ([#2968](https://github.com/maxrave-dev/SimpMusic/issues/2968))
- Máy tính: đăng nhập Discord không bao giờ hoàn tất trên Windows và Linux sau khi bạn đã cho phép trong trình duyệt. ([#2894](https://github.com/maxrave-dev/SimpMusic/issues/2894), [#2919](https://github.com/maxrave-dev/SimpMusic/pull/2919))
- Mix cho bạn tải mãi không xong khi YouTube từ chối yêu cầu. Giờ app hiện thông báo lỗi. ([#2944](https://github.com/maxrave-dev/SimpMusic/issues/2944), [#1688](https://github.com/maxrave-dev/SimpMusic/issues/1688))
- Xoá một bản của bài có hai bản trong playlist làm mất cả hai, và không thêm lại bài đó được. ([#2933](https://github.com/maxrave-dev/SimpMusic/issues/2933))
- Trộn bài không chạy với Liked Music, với playlist mở khi không có mạng, và với playlist mà app không đọc được nút Trộn bài. ([#2054](https://github.com/maxrave-dev/SimpMusic/issues/2054), [#2083](https://github.com/maxrave-dev/SimpMusic/issues/2083), [#2814](https://github.com/maxrave-dev/SimpMusic/issues/2814))
- Trộn bài một playlist local chỉ phát một bài ngẫu nhiên, rồi phát phần còn lại theo đúng thứ tự playlist. ([#2520](https://github.com/maxrave-dev/SimpMusic/issues/2520))
- Library sync chép những playlist mà cả hai máy đều đã có sang máy kia sau mỗi lần ghép. ([#2967](https://github.com/maxrave-dev/SimpMusic/issues/2967))
- AutoMix quay về đoạn chuyển cố định vì việc tra nhịp độ và tông bị từ chối. ([#2965](https://github.com/maxrave-dev/SimpMusic/issues/2965))
- Bản remix và bản live nhận nhầm lời của bài gốc từ Spotify và LRCLIB. ([#2963](https://github.com/maxrave-dev/SimpMusic/issues/2963))
- Dịch lời bằng AI bị lỗi với OpenRouter, và với Gemini nếu bạn không tự đổi model. ([#2297](https://github.com/maxrave-dev/SimpMusic/issues/2297), [#2684](https://github.com/maxrave-dev/SimpMusic/issues/2684))
