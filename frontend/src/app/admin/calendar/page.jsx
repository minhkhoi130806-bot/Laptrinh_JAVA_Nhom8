"use client";
import { useMemo, useState } from "react";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import PageHeader from "@/components/admin/PageHeader";
import StateBox from "@/components/admin/StateBox";
import Badge from "@/components/admin/Badge";
const dateKey = (value) => {
  const d = new Date(value);
  return Number.isNaN(d.getTime())
    ? ""
    : `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
export default function ReservationCalendarPage() {
  const reservationsApi = useApi("/reservations");
  const customersApi = useApi("/customers?scope=ALL");
  const tablesApi = useApi("/tables?scope=ALL");
  const customers = Array.isArray(customersApi.data) ? customersApi.data : [];
  const tables = Array.isArray(tablesApi.data) ? tablesApi.data : [];
  const customerNames = Object.fromEntries(
    customers.map((c) => [
      c.id,
      c.phone ? `${c.fullName} – ${c.phone}` : c.fullName,
    ]),
  );
  const tableNames = Object.fromEntries(
    tables.map((t) => [t.id, `${t.tableCode} (${t.capacity} chỗ)`]),
  );
  const [month, setMonth] = useState(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  });
  const [selected, setSelected] = useState(() => dateKey(new Date()));
  const [status, setStatus] = useState("ALL");
  const rows = Array.isArray(reservationsApi.data) ? reservationsApi.data : [];
  const visible = useMemo(
    () =>
      rows.filter(
        (r) =>
          dateKey(r.reservedAt).startsWith(month) &&
          (status === "ALL" || r.status === status),
      ),
    [rows, month, status],
  );
  const daily = useMemo(
    () =>
      visible
        .filter((r) => dateKey(r.reservedAt) === selected)
        .sort((a, b) => new Date(a.reservedAt) - new Date(b.reservedAt)),
    [visible, selected],
  );
  const monthDate = new Date(`${month}-01T12:00:00`);
  const days = new Date(
    monthDate.getFullYear(),
    monthDate.getMonth() + 1,
    0,
  ).getDate();
  const offset = (monthDate.getDay() + 6) % 7;
  const prevMonth = () => {
    const d = new Date(`${month}-01T12:00:00`);
    d.setMonth(d.getMonth() - 1);
    const next = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
    setMonth(next);
    setSelected(`${next}-01`);
  };
  const nextMonth = () => {
    const d = new Date(`${month}-01T12:00:00`);
    d.setMonth(d.getMonth() + 1);
    const next = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
    setMonth(next);
    setSelected(`${next}-01`);
  };
  const countByDay = Object.fromEntries(
    visible.reduce((map, r) => {
      const key = dateKey(r.reservedAt);
      map.set(key, (map.get(key) || 0) + 1);
      return map;
    }, new Map()),
  );
  return (
    <>
      <PageHeader
        icon="🗓️"
        title="Lịch đặt bàn"
        subtitle="Xem lịch đặt bàn theo tháng, ngày và trạng thái; dữ liệu lấy từ backend."
      >
        <button
          className="adm-btn ghost sm"
          onClick={() => {
            reservationsApi.reload();
            customersApi.reload();
            tablesApi.reload();
          }}
        >
          ↻ Làm mới
        </button>
        <Link className="adm-btn sm" href="/admin/reservations/create">
          ＋ Đặt bàn
        </Link>
      </PageHeader>
      <StateBox
        loading={
          reservationsApi.loading || customersApi.loading || tablesApi.loading
        }
        error={[reservationsApi.error, customersApi.error, tablesApi.error]
          .filter(Boolean)
          .join(" · ")}
        onRetry={() => {
          reservationsApi.reload();
          customersApi.reload();
          tablesApi.reload();
        }}
      />
      <div className="adm-card adm-section" style={{ marginTop: 14 }}>
        <div
          style={{
            display: "flex",
            gap: 10,
            alignItems: "center",
            justifyContent: "space-between",
            flexWrap: "wrap",
          }}
        >
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <button className="adm-btn ghost sm" onClick={prevMonth}>
              ←
            </button>
            <h2 style={{ margin: 0 }}>
              {monthDate.toLocaleDateString("vi-VN", {
                month: "long",
                year: "numeric",
              })}
            </h2>
            <button className="adm-btn ghost sm" onClick={nextMonth}>
              →
            </button>
          </div>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="ALL">Mọi trạng thái</option>
            <option value="PENDING">Chờ xác nhận</option>
            <option value="CONFIRMED">Đã xác nhận</option>
            <option value="COMPLETED">Hoàn tất</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
        <div
          className="booking-calendar"
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(7,minmax(0,1fr))",
            gap: 6,
            marginTop: 16,
          }}
        >
          {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((d) => (
            <div
              key={d}
              className="adm-muted"
              style={{ textAlign: "center", fontWeight: 700, padding: 6 }}
            >
              {d}
            </div>
          ))}
          {Array.from({ length: offset }, (_, i) => (
            <div key={`empty-${i}`} />
          ))}
          {Array.from({ length: days }, (_, i) => {
            const day = i + 1;
            const key = `${month}-${String(day).padStart(2, "0")}`;
            const count = countByDay[key] || 0;
            return (
              <button
                key={key}
                onClick={() => setSelected(key)}
                aria-pressed={selected === key}
                style={{
                  minHeight: 76,
                  borderRadius: 10,
                  border:
                    selected === key
                      ? "2px solid var(--primary)"
                      : "1px solid var(--line)",
                  background:
                    selected === key ? "var(--surface-soft)" : "var(--surface)",
                  padding: 8,
                  textAlign: "left",
                  cursor: "pointer",
                }}
              >
                <b>{day}</b>
                <div
                  style={{
                    fontSize: 12,
                    marginTop: 8,
                    color: count ? "var(--primary)" : "var(--muted)",
                  }}
                >
                  {count ? `${count} lượt đặt` : "—"}
                </div>
              </button>
            );
          })}
        </div>
      </div>
      <div className="adm-card adm-section" style={{ marginTop: 16 }}>
        <h3 style={{ marginTop: 0 }}>
          Lượt đặt ngày{" "}
          {new Date(`${selected}T12:00:00`).toLocaleDateString("vi-VN", {
            weekday: "long",
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
          })}{" "}
          <span className="adm-muted">({daily.length})</span>
        </h3>
        {daily.length === 0 ? (
          <div className="adm-empty">
            Không có lượt đặt bàn theo bộ lọc trong ngày này.
          </div>
        ) : (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Mã</th>
                  <th>Giờ</th>
                  <th>Khách hàng</th>
                  <th>Bàn</th>
                  <th>Số khách</th>
                  <th>Ghi chú</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {daily.map((r) => (
                  <tr key={r.id}>
                    <td>#{r.id}</td>
                    <td>
                      {new Date(r.reservedAt).toLocaleTimeString("vi-VN", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </td>
                    <td>
                      {customerNames[r.customerId] || `Khách #${r.customerId}`}
                    </td>
                    <td>{tableNames[r.tableId] || `Bàn #${r.tableId}`}</td>
                    <td>{r.guestCount}</td>
                    <td>{r.note || "—"}</td>
                    <td>
                      <Badge value={r.status} />
                    </td>
                    <td>
                      <Link
                        className="adm-link"
                        href={`/admin/reservations/${r.id}`}
                      >
                        Chi tiết
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
