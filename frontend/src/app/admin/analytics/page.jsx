"use client";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import { vnd, num } from "@/lib/api";
import PageHeader from "@/components/admin/PageHeader";
import StateBox from "@/components/admin/StateBox";

function Stat({ label, value, hint, href }) {
  const body = (
    <div className="adm-stat" style={{ height: "100%" }}>
      <span className="adm-muted">{label}</span>
      <b style={{ fontSize: 22 }}>{value}</b>
      {hint && (
        <span className="adm-muted" style={{ fontSize: 12 }}>
          {hint}
        </span>
      )}
    </div>
  );
  return href ? (
    <Link href={href} style={{ textDecoration: "none", color: "inherit" }}>
      {body}
    </Link>
  ) : (
    body
  );
}
export default function AnalyticsPage() {
  const rev = useApi("/invoices/report/revenue?days=30");
  const orders = useApi("/orders");
  const tables = useApi("/tables");
  const reservations = useApi("/reservations");
  const ingredients = useApi("/ingredients/report");
  const best = useApi("/menu-items/best-sellers");
  const loading = [rev, orders, tables, reservations, ingredients, best].some(
    (x) => x.loading,
  );
  const error = [rev, orders, tables, reservations, ingredients, best]
    .map((x) => x.error)
    .filter(Boolean)
    .join(" · ");
  const revenue = rev.data || {};
  const orderRows = Array.isArray(orders.data) ? orders.data : [];
  const tableRows = Array.isArray(tables.data) ? tables.data : [];
  const reservationRows = Array.isArray(reservations.data)
    ? reservations.data
    : [];
  const inv = ingredients.data || {};
  const bestRows = Array.isArray(best.data) ? best.data : [];
  const today = new Date().toDateString();
  const todayOrders = orderRows.filter(
    (o) => new Date(o.createdAt).toDateString() === today,
  );
  const openOrders = orderRows.filter((o) =>
    ["OPEN", "SERVING"].includes(o.status),
  );
  const busyTables = tableRows.filter((t) => t.status === "OCCUPIED").length;
  const upcoming = reservationRows.filter(
    (r) =>
      ["PENDING", "CONFIRMED"].includes(r.status) &&
      new Date(r.reservedAt) >= new Date(),
  ).length;
  const refresh = () =>
    [rev, orders, tables, reservations, ingredients, best].forEach((x) =>
      x.reload(),
    );
  return (
    <>
      <PageHeader
        icon="📊"
        title="Trung tâm phân tích"
        subtitle="Tổng hợp hoạt động nhà hàng từ đơn hàng, bàn, đặt bàn, doanh thu và kho."
      >
        <button className="adm-btn ghost sm" onClick={refresh}>
          ↻ Làm mới dữ liệu
        </button>
      </PageHeader>
      <StateBox
        loading={loading && !rev.data}
        error={error}
        onRetry={refresh}
      />
      <div className="adm-stats" style={{ marginTop: 16 }}>
        <Stat
          label="Doanh thu 30 ngày"
          value={vnd(revenue.totalRevenue || 0)}
          hint={`${num(revenue.totalInvoices || 0)} hóa đơn`}
          href="/admin/reports"
        />
        <Stat
          label="Đơn đang mở"
          value={num(openOrders.length)}
          hint="Mới tạo hoặc đang phục vụ"
          href="/admin/orders"
        />
        <Stat
          label="Đơn tạo hôm nay"
          value={num(todayOrders.length)}
          href="/admin/orders"
        />
        <Stat
          label="Bàn đang sử dụng"
          value={`${busyTables}/${tableRows.length}`}
          href="/admin/floor"
        />
        <Stat
          label="Lượt đặt sắp tới"
          value={num(upcoming)}
          href="/admin/calendar"
        />
        <Stat
          label="Nguyên liệu sắp hết"
          value={num(inv.lowStockCount || 0)}
          hint={`${num(inv.outOfStockCount || 0)} nguyên liệu hết hàng`}
          href="/admin/ingredients"
        />
      </div>
      <div className="adm-card adm-section" style={{ marginTop: 16 }}>
        <h3 style={{ marginTop: 0 }}>🚀 Truy cập nhanh</h3>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit,minmax(190px,1fr))",
            gap: 10,
          }}
        >
          {[
            ["Đơn hàng", "/admin/orders", "Theo dõi đơn và trạng thái phục vụ"],
            ["Lịch đặt bàn", "/admin/calendar", "Xem lượng khách theo ngày"],
            ["Màn hình bếp", "/admin/kitchen", "Xem hàng đợi chế biến"],
            ["Sơ đồ bàn", "/admin/floor", "Tình trạng bàn đang dùng"],
            ["Khuyến mãi", "/admin/marketing", "Theo dõi chương trình ưu đãi"],
            ["Lịch ca làm", "/admin/shifts", "Phân công và bàn giao ca"],
            [
              "Checklist vệ sinh",
              "/admin/cleaning",
              "Kiểm soát công việc vệ sinh",
            ],
            ["Sự cố thiết bị", "/admin/maintenance", "Theo dõi sửa chữa"],
          ].map(([title, href, desc]) => (
            <Link
              key={href}
              href={href}
              className="adm-card"
              style={{ padding: 14, textDecoration: "none", color: "inherit" }}
            >
              <b>{title} ↗</b>
              <div className="adm-muted" style={{ fontSize: 12, marginTop: 5 }}>
                {desc}
              </div>
            </Link>
          ))}
        </div>
      </div>
      <div className="adm-card adm-section" style={{ marginTop: 16 }}>
        <h3 style={{ marginTop: 0 }}>🍜 Món bán chạy</h3>
        {bestRows.length === 0 ? (
          <p className="adm-muted">Chưa có dữ liệu món bán chạy.</p>
        ) : (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Món ăn</th>
                  <th>Thông tin bán</th>
                </tr>
              </thead>
              <tbody>
                {bestRows.slice(0, 8).map((item, i) => (
                  <tr key={item.id || item.menuItemId || i}>
                    <td>{i + 1}</td>
                    <td>
                      {item.nameVi ||
                        item.name ||
                        item.menuItemName ||
                        `Món #${item.menuItemId || item.id || i + 1}`}
                    </td>
                    <td>
                      {item.totalQuantitySold != null
                        ? `${num(item.totalQuantitySold)} phần`
                        : item.quantitySold != null
                          ? `${num(item.quantitySold)} phần`
                          : item.soldCount != null
                            ? `${num(item.soldCount)} phần`
                            : item.totalSold != null
                              ? `${num(item.totalSold)} phần`
                              : "Theo báo cáo món bán chạy"}
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
