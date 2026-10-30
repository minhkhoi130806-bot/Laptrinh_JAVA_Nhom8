"use client";
import { useMemo, useState } from "react";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import { num } from "@/lib/api";
import PageHeader from "@/components/admin/PageHeader";
import StateBox from "@/components/admin/StateBox";

const tier = (points) =>
  points >= 1000
    ? ["Kim cương", "blue"]
    : points >= 500
      ? ["Vàng", "amber"]
      : points >= 100
        ? ["Bạc", "gray"]
        : ["Thành viên", "green"];
function exportCsv(rows) {
  const csvRows = [
    ["Mã", "Họ tên", "Điện thoại", "Email", "Điểm", "Hạng"],
    ...rows.map((c) => [
      c.id,
      c.fullName,
      c.phone,
      c.email,
      c.loyaltyPoints || 0,
      tier(c.loyaltyPoints || 0)[0],
    ]),
  ];
  const csv =
    "\uFEFF" +
    csvRows
      .map((r) =>
        r.map((v) => `"${String(v ?? "").replace(/"/g, '""')}"`).join(","),
      )
      .join("\r\n");
  const url = URL.createObjectURL(
    new Blob([csv], { type: "text/csv;charset=utf-8;" }),
  );
  const a = document.createElement("a");
  a.href = url;
  a.download = "khach-hang-thanh-vien.csv";
  a.click();
  URL.revokeObjectURL(url);
}
export default function LoyaltyPage() {
  const data = useApi("/customers");
  const [search, setSearch] = useState("");
  const [level, setLevel] = useState("ALL");
  const rows = Array.isArray(data.data) ? data.data : [];
  const visible = useMemo(
    () =>
      rows
        .filter((c) => {
          const points = Number(c.loyaltyPoints || 0);
          const t =
            points >= 1000
              ? "DIAMOND"
              : points >= 500
                ? "GOLD"
                : points >= 100
                  ? "SILVER"
                  : "MEMBER";
          return (
            `${c.fullName || ""} ${c.phone || ""} ${c.email || ""}`
              .toLowerCase()
              .includes(search.toLowerCase()) &&
            (level === "ALL" || t === level)
          );
        })
        .sort((a, b) => (b.loyaltyPoints || 0) - (a.loyaltyPoints || 0)),
    [rows, search, level],
  );
  const points = rows.reduce((sum, c) => sum + Number(c.loyaltyPoints || 0), 0);
  return (
    <>
      <PageHeader
        icon="🏅"
        title="Khách hàng thân thiết"
        subtitle="Xếp hạng tham khảo dựa trên điểm tích lũy đang lưu trong hồ sơ khách hàng."
      >
        <button className="adm-btn ghost sm" onClick={() => data.reload()}>
          ↻ Làm mới
        </button>
        <button className="adm-btn sm" onClick={() => exportCsv(visible)}>
          ⬇ Xuất CSV
        </button>
      </PageHeader>
      <StateBox
        loading={data.loading && !data.data}
        error={data.error}
        onRetry={data.reload}
      />
      <div className="adm-stats" style={{ marginTop: 14 }}>
        <div className="adm-stat">
          <span className="adm-muted">Tổng thành viên</span>
          <b>{num(rows.length)}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Tổng điểm hiện có</span>
          <b>{num(points)}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Hạng Vàng trở lên</span>
          <b>{rows.filter((c) => (c.loyaltyPoints || 0) >= 500).length}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Chưa có điểm</span>
          <b>{rows.filter((c) => !(c.loyaltyPoints || 0)).length}</b>
        </div>
      </div>
      <div className="adm-card adm-section" style={{ marginTop: 16 }}>
        <div
          style={{
            display: "flex",
            gap: 8,
            flexWrap: "wrap",
            justifyContent: "space-between",
            marginBottom: 12,
          }}
        >
          <h3 style={{ margin: 0 }}>Danh sách thành viên ({visible.length})</h3>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <input
              placeholder="Tìm tên, điện thoại, email…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select value={level} onChange={(e) => setLevel(e.target.value)}>
              <option value="ALL">Mọi hạng</option>
              <option value="DIAMOND">Kim cương</option>
              <option value="GOLD">Vàng</option>
              <option value="SILVER">Bạc</option>
              <option value="MEMBER">Thành viên</option>
            </select>
          </div>
        </div>
        {visible.length === 0 ? (
          <div className="adm-empty">Không có thành viên phù hợp.</div>
        ) : (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Khách hàng</th>
                  <th>Liên hệ</th>
                  <th>Điểm</th>
                  <th>Hạng tham khảo</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {visible.map((c) => {
                  const [label, color] = tier(c.loyaltyPoints || 0);
                  return (
                    <tr key={c.id}>
                      <td>
                        <b>{c.fullName}</b>
                        <div className="adm-muted">Mã #{c.id}</div>
                      </td>
                      <td>
                        {c.phone || "—"}
                        <div className="adm-muted">{c.email || ""}</div>
                      </td>
                      <td>
                        <b>{num(c.loyaltyPoints || 0)}</b>
                      </td>
                      <td>
                        <span className={`adm-badge ${color}`}>{label}</span>
                      </td>
                      <td>
                        <Link
                          className="adm-link"
                          href={`/admin/customers/${c.id}`}
                        >
                          Hồ sơ →
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        <p className="adm-muted" style={{ fontSize: 12 }}>
          Hạng được tính ở giao diện để phân nhóm, chưa phải chương trình ưu đãi
          tự động. Việc cộng/trừ điểm vẫn cần quy tắc nghiệp vụ phía backend.
        </p>
      </div>
    </>
  );
}
