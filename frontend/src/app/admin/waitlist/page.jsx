import LocalManagerPage from "@/components/admin/LocalManagerPage";
export default function WaitlistPage() {
  return (
    <LocalManagerPage
      config={{
        storageKey: "restaurant_waitlist_v1",
        fileName: "hang-doi-khach",
        icon: "🕒",
        title: "Danh sách chờ bàn",
        subtitle: "Quản lý khách đến trực tiếp khi chưa có bàn trống.",
        formTitle: "Thêm khách vào hàng chờ",
        fields: [
          { name: "customer", label: "Tên khách", required: true },
          { name: "phone", label: "Số điện thoại", required: true },
          {
            name: "partySize",
            label: "Số khách",
            type: "number",
            required: true,
          },
          { name: "arrival", label: "Giờ đến", type: "time", required: true },
          {
            name: "area",
            label: "Khu vực mong muốn",
            placeholder: "Trong nhà / gần cửa sổ",
          },
          { name: "note", label: "Yêu cầu / ghi chú", type: "textarea" },
        ],
        doneLabel: "Đã xếp bàn",
      }}
    />
  );
}
