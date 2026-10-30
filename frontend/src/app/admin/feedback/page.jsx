import LocalManagerPage from "@/components/admin/LocalManagerPage";
export default function FeedbackPage() {
  return (
    <LocalManagerPage
      config={{
        storageKey: "restaurant_feedback_v1",
        fileName: "phan-hoi-khieu-nai",
        icon: "💬",
        title: "Phản hồi & Khiếu nại",
        subtitle: "Ghi nhận góp ý, sự cố dịch vụ và theo dõi việc xử lý.",
        formTitle: "Ghi nhận phản hồi",
        fields: [
          { name: "customer", label: "Khách hàng", required: true },
          { name: "phone", label: "Số điện thoại / liên hệ" },
          {
            name: "type",
            label: "Loại phản hồi",
            type: "select",
            required: true,
            options: [
              "Góp ý",
              "Khen ngợi",
              "Khiếu nại món ăn",
              "Khiếu nại phục vụ",
              "Vấn đề thanh toán",
              "Khác",
            ],
          },
          {
            name: "priority",
            label: "Mức ưu tiên",
            type: "select",
            required: true,
            options: ["Thấp", "Bình thường", "Cao", "Khẩn cấp"],
          },
          {
            name: "detail",
            label: "Nội dung",
            type: "textarea",
            required: true,
          },
          { name: "assignee", label: "Người phụ trách" },
          { name: "resolution", label: "Hướng xử lý", type: "textarea" },
        ],
        doneLabel: "Đã xử lý",
      }}
    />
  );
}
