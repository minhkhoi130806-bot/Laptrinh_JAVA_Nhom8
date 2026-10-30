"use client";
import { useEffect, useState } from "react";
import PageHeader from "@/components/admin/PageHeader";

const DEFAULTS = {
  name: "Nhà hàng Hàn Quốc Nhóm 8",
  phone: "",
  address: "",
  opening: "09:00",
  closing: "22:00",
  serviceCharge: "0",
  vat: "0",
  reservationWindow: "30",
  receiptFooter: "Cảm ơn quý khách và hẹn gặp lại!",
};
export default function SettingsPage() {
  const [settings, setSettings] = useState(DEFAULTS);
  const [ready, setReady] = useState(false);
  const [message, setMessage] = useState("");
  useEffect(() => {
    try {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSettings({
        ...DEFAULTS,
        ...JSON.parse(localStorage.getItem("restaurant_settings_v1") || "{}"),
      });
    } catch {}
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setReady(true);
  }, []);
  const change = (key, value) =>
    setSettings((old) => ({ ...old, [key]: value }));
  const save = (e) => {
    e.preventDefault();
    localStorage.setItem("restaurant_settings_v1", JSON.stringify(settings));
    setMessage("Đã lưu cấu hình trong trình duyệt này.");
  };
  const reset = () => {
    if (confirm("Khôi phục cấu hình mặc định?")) {
      setSettings(DEFAULTS);
      localStorage.removeItem("restaurant_settings_v1");
      setMessage("Đã khôi phục cấu hình mặc định.");
    }
  };
  return (
    <>
      <PageHeader
        icon="⚙️"
        title="Cấu hình nhà hàng"
        subtitle="Thông tin liên hệ, giờ mở cửa và thông số tham khảo cho vận hành."
      />
      {ready && (
        <form className="adm-card adm-section" onSubmit={save}>
          <div className="adm-form-grid">
            {[
              ["name", "Tên nhà hàng", "text"],
              ["phone", "Số điện thoại", "tel"],
              ["address", "Địa chỉ", "text"],
              ["opening", "Giờ mở cửa", "time"],
              ["closing", "Giờ đóng cửa", "time"],
              ["serviceCharge", "Phí dịch vụ (%)", "number"],
              ["vat", "Thuế VAT (%)", "number"],
              ["reservationWindow", "Số ngày nhận đặt bàn trước", "number"],
            ].map(([key, label, type]) => (
              <label key={key} className="adm-field">
                <span>{label}</span>
                <input
                  type={type}
                  min={type === "number" ? 0 : undefined}
                  max={["serviceCharge", "vat"].includes(key) ? 100 : undefined}
                  value={settings[key]}
                  onChange={(e) => change(key, e.target.value)}
                />
              </label>
            ))}
            <label className="adm-field" style={{ gridColumn: "1 / -1" }}>
              <span>Lời nhắn cuối hóa đơn</span>
              <textarea
                rows={3}
                value={settings.receiptFooter}
                onChange={(e) => change("receiptFooter", e.target.value)}
              />
            </label>
          </div>
          <p className="adm-muted">
            Cấu hình này mới lưu cục bộ trên trình duyệt; chưa tự áp dụng vào
            tính tiền backend hoặc hóa đơn đã phát hành.
          </p>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="adm-btn" type="submit">
              Lưu cấu hình
            </button>
            <button className="adm-btn ghost" type="button" onClick={reset}>
              Khôi phục mặc định
            </button>
          </div>
          {message && <p role="status">{message}</p>}
        </form>
      )}
    </>
  );
}
