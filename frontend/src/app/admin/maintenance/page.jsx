import LocalManagerPage from "@/components/admin/LocalManagerPage";
export default function MaintenancePage() {
  return (
    <LocalManagerPage
      config={{
        storageKey: "restaurant_maintenance_v1",
        fileName: "bao-tri-thiet-bi",
        icon: "🛠️",
        title: "Bảo trì & Sự cố thiết bị",
        subtitle: "Ghi nhận thiết bị hỏng, lịch kiểm tra và theo dõi sửa chữa.",
        formTitle: "Tạo phiếu sự cố",
        fields: [
          { name: "equipment", label: "Thiết bị", required: true },
          { name: "area", label: "Khu vực", required: true },
          {
            name: "severity",
            label: "Mức độ",
            type: "select",
            required: true,
            options: ["Thấp", "Trung bình", "Cao", "Ngừng hoạt động"],
          },
          {
            name: "reportedAt",
            label: "Thời điểm báo",
            type: "datetime-local",
            required: true,
          },
          { name: "assignee", label: "Người xử lý" },
          {
            name: "description",
            label: "Mô tả sự cố",
            type: "textarea",
            required: true,
          },
          { name: "cost", label: "Chi phí dự kiến (VNĐ)", type: "number" },
        ],
        doneLabel: "Đã sửa xong",
      }}
    />
  );
}
