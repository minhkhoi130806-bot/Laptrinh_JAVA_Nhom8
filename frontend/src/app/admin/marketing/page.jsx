import LocalManagerPage from "@/components/admin/LocalManagerPage";
export default function MarketingPage() {
  return (
    <LocalManagerPage
      config={{
        storageKey: "restaurant_marketing_v1",
        fileName: "chuong-trinh-khuyen-mai",
        icon: "🎟️",
        title: "Khuyến mãi & Mã giảm giá",
        subtitle:
          "Lên kế hoạch chương trình ưu đãi, thời hạn và điều kiện áp dụng.",
        formTitle: "Tạo chương trình",
        fields: [
          { name: "name", label: "Tên chương trình", required: true },
          { name: "code", label: "Mã khuyến mãi", required: true },
          {
            name: "kind",
            label: "Loại ưu đãi",
            type: "select",
            required: true,
            options: [
              "Giảm theo %",
              "Giảm số tiền",
              "Tặng món",
              "Combo ưu đãi",
            ],
          },
          { name: "value", label: "Giá trị / mô tả ưu đãi", required: true },
          {
            name: "startDate",
            label: "Ngày bắt đầu",
            type: "date",
            required: true,
          },
          {
            name: "endDate",
            label: "Ngày kết thúc",
            type: "date",
            required: true,
          },
          { name: "condition", label: "Điều kiện áp dụng", type: "textarea" },
          { name: "limit", label: "Số lượt tối đa", type: "number" },
        ],
        doneLabel: "Ngừng chương trình",
      }}
    />
  );
}
