import LocalManagerPage from "@/components/admin/LocalManagerPage";
export default function ShiftsPage() {
  return (
    <LocalManagerPage
      config={{
        storageKey: "restaurant_shifts_v1",
        fileName: "ca-lam-viec",
        icon: "🕐",
        title: "Lịch ca làm việc",
        subtitle: "Lập lịch ca, ghi nhận phân công và bàn giao giữa các ca.",
        formTitle: "Tạo ca làm việc",
        fields: [
          { name: "employee", label: "Tên nhân viên", required: true },
          {
            name: "role",
            label: "Vị trí",
            type: "select",
            required: true,
            options: ["Quản lý", "Thu ngân", "Phục vụ", "Đầu bếp"],
          },
          { name: "date", label: "Ngày làm", type: "date", required: true },
          {
            name: "shift",
            label: "Ca",
            type: "select",
            required: true,
            options: ["Sáng", "Chiều", "Tối", "Ca gãy"],
          },
          { name: "start", label: "Giờ bắt đầu", type: "time", required: true },
          { name: "end", label: "Giờ kết thúc", type: "time", required: true },
          { name: "handover", label: "Nội dung bàn giao", type: "textarea" },
        ],
        doneLabel: "Đã bàn giao",
      }}
    />
  );
}
