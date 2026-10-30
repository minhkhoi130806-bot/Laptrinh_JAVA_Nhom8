"use client";
import Link from "next/link";
import { useApi } from "@/lib/hooks";
import { vnd, num } from "@/lib/api";
import PageHeader from "@/components/admin/PageHeader";
import StateBox from "@/components/admin/StateBox";
export default function MenuPlannerPage() {
  const best = useApi("/menu-items/best-sellers");
  const stock = useApi("/menu-items/stock-status");
  const menu = useApi("/menu-items");
  const bestRows = Array.isArray(best.data) ? best.data : [];
  const stockRows = Array.isArray(stock.data) ? stock.data : [];
  const menuRows = Array.isArray(menu.data) ? menu.data : [];
  const unavailable = menuRows.filter((m) => m.available === false);
  const low = stockRows.filter((s) => s.hasEnoughStock === false);
  const refresh = () => [best, stock, menu].forEach((x) => x.reload());
  return (
    <>
      <PageHeader
        icon="🍱"
        title="Điều phối thực đơn"
        subtitle="Kết hợp món bán chạy, trạng thái nguyên liệu và khả năng bán để hỗ trợ quyết định vận hành."
      >
        <button className="adm-btn ghost sm" onClick={refresh}>
          ↻ Làm mới
        </button>
        <Link className="adm-btn sm" href="/admin/menu">
          Quản lý món
        </Link>
      </PageHeader>
      <StateBox
        loading={[best, stock, menu].some((x) => x.loading)}
        error={[best, stock, menu]
          .map((x) => x.error)
          .filter(Boolean)
          .join(" · ")}
        onRetry={refresh}
      />
      <div className="adm-stats" style={{ marginTop: 14 }}>
        <div className="adm-stat">
          <span className="adm-muted">Tổng món</span>
          <b>{num(menuRows.length)}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Món đang tạm ngưng</span>
          <b>{num(unavailable.length)}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Món thiếu nguyên liệu</span>
          <b>{num(low.length)}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Món bán chạy có dữ liệu</span>
          <b>{num(bestRows.length)}</b>
        </div>
      </div>
      <div className="adm-card adm-section" style={{ marginTop: 16 }}>
        <h3 style={{ marginTop: 0 }}>⚠️ Món cần xem lại do tồn kho</h3>
        {low.length === 0 ? (
          <p className="adm-muted">
            Không có món bị đánh dấu thiếu nguyên liệu theo dữ liệu hiện tại.
          </p>
        ) : (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Món</th>
                  <th>Trạng thái nguyên liệu</th>
                  <th>Gợi ý</th>
                </tr>
              </thead>
              <tbody>
                {low.map((r, i) => (
                  <tr key={r.menuItemId || r.id || i}>
                    <td>
                      {r.nameVi ||
                        r.menuItemName ||
                        r.name ||
                        `Món #${r.menuItemId || r.id || i + 1}`}
                    </td>
                    <td>
                      <span
                        className={`adm-badge ${r.currentlyAvailable ? "amber" : "red"}`}
                      >
                        {r.currentlyAvailable ? "Đang bán" : "Đang tạm ngưng"}
                      </span>
                    </td>
                    <td>
                      {(r.missingIngredients || []).length
                        ? `Thiếu: ${r.missingIngredients.join(", ")}`
                        : "Kiểm tra công thức và cân nhắc đồng bộ trạng thái bán."}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div style={{ marginTop: 12 }}>
          <Link className="adm-btn ghost sm" href="/admin/reports">
            Mở báo cáo tồn kho →
          </Link>
        </div>
      </div>
      <div className="adm-card adm-section" style={{ marginTop: 16 }}>
        <h3 style={{ marginTop: 0 }}>🔥 Món bán chạy</h3>
        {bestRows.length === 0 ? (
          <p className="adm-muted">Chưa có dữ liệu bán hàng để xếp hạng.</p>
        ) : (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Hạng</th>
                  <th>Món</th>
                  <th>Số lượng</th>
                  <th>Giá tham khảo</th>
                </tr>
              </thead>
              <tbody>
                {bestRows.map((r, i) => (
                  <tr key={r.id || r.menuItemId || i}>
                    <td>#{i + 1}</td>
                    <td>
                      {r.nameVi ||
                        r.name ||
                        r.menuItemName ||
                        `Món #${r.menuItemId || r.id || i + 1}`}
                    </td>
                    <td>
                      {r.totalQuantitySold != null
                        ? num(r.totalQuantitySold)
                        : r.quantitySold != null
                          ? num(r.quantitySold)
                          : r.soldCount != null
                            ? num(r.soldCount)
                            : r.totalSold != null
                              ? num(r.totalSold)
                              : "—"}
                    </td>
                    <td>{r.price != null ? vnd(r.price) : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <p className="adm-muted" style={{ fontSize: 12, marginTop: 12 }}>
        Trang này là màn hình hỗ trợ quyết định, không tự thay đổi giá hoặc
        trạng thái món. Thao tác cập nhật vẫn thực hiện tại Quản lý món/Báo cáo
        kho.
      </p>
    </>
  );
}
