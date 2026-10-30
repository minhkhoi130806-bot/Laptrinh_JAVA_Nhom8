import LocalManagerPage from "@/components/admin/LocalManagerPage";
export default function CleaningPage() {
  return (
    <LocalManagerPage
      config={{
        storageKey: "restaurant_cleaning_v1",
        fileName: "ve-sinh-nha-hang",
        icon: "🧽",
        title: "Checklist vệ sinh",
        subtitle: "Theo dõi vệ sinh bàn, khu vực ăn uống, bếp và nhà vệ sinh.",
        formTitle: "Thêm nhiệm vụ vệ sinh",
        fields: [
          { name: "task", label: "Nhiệm vụ", required: true },
          {
            name: "area",
            label: "Khu vực",
            type: "select",
            required: true,
            options: [
              "Phòng ăn",
              "Bếp",
              "Nhà vệ sinh",
              "Kho",
              "Quầy thu ngân",
              "Khu vực ngoài",
            ],
          },
          {
            name: "frequency",
            label: "Tần suất",
            type: "select",
            required: true,
            options: [
              "Sau mỗi lượt khách",
              "Mỗi giờ",
              "Đầu ca",
              "Cuối ca",
              "Hằng ngày",
            ],
          },
          { name: "assignee", label: "Người phụ trách" },
          { name: "due", label: "Hạn hoàn thành", type: "datetime-local" },
          { name: "note", label: "Ghi chú", type: "textarea" },
        ],
        doneLabel: "Đã vệ sinh",
      }}
    />
  );
}
