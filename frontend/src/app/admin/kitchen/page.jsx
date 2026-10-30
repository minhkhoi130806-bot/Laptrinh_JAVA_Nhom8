"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api, dt, vnd } from "@/lib/api";
import PageHeader from "@/components/admin/PageHeader";
import StateBox from "@/components/admin/StateBox";
import Badge from "@/components/admin/Badge";
import { useToast } from "@/components/admin/Feedback";

const ACTIVE = new Set(["OPEN", "SERVING"]);

export default function KitchenPage() {
  const toast = useToast();
  const [orders, setOrders] = useState([]);
  const [itemsByOrder, setItemsByOrder] = useState({});
  const [menu, setMenu] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [updating, setUpdating] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  const load = useCallback(async (quiet = false) => {
    if (quiet) setRefreshing(true);
    else setLoading(true);
    setError("");
    try {
      const [allOrders, menuRows] = await Promise.all([
        api("/orders"),
        api("/menu-items"),
      ]);
      const activeOrders = (allOrders || [])
        .filter((o) => ACTIVE.has(o.status))
        .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
      const pairs = await Promise.all(
        activeOrders.map(async (order) => {
          const lines = await api(`/order-items?orderId=${order.id}`);
          return [order.id, lines || []];
        }),
      );
      setOrders(activeOrders);
      setItemsByOrder(Object.fromEntries(pairs));
      setMenu(menuRows || []);
      setLastUpdated(new Date());
    } catch (e) {
      setError(e.message || "Không thể tải danh sách món cần chế biến.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    load();
    const timer = setInterval(() => load(true), 20000);
    return () => clearInterval(timer);
  }, [load]);

  const changeStatus = async (order, status) => {
    setUpdating(order.id);
    try {
      await api(`/orders/${order.id}/status`, {
        method: "PATCH",
        body: { status },
      });
      toast(
        status === "SERVING"
          ? `Đã nhận chế biến đơn #${order.id}`
          : `Đã cập nhật đơn #${order.id}`,
      );
      await load(true);
    } catch (e) {
      toast(e.message || "Không thể cập nhật trạng thái.", "err");
    } finally {
      setUpdating(null);
    }
  };

  const visibleOrders = orders.filter((order) => {
    const matchesStatus = filter === "ALL" || order.status === filter;
    const table = order.tableId == null ? "khach le" : `ban ${order.tableId}`;
    const term = query.trim().toLocaleLowerCase("vi");
    return (
      matchesStatus &&
      (!term ||
        `#${order.id} ${table} ${order.customerName || ""}`
          .toLocaleLowerCase("vi")
          .includes(term))
    );
  });
  const waitingCount = orders.filter((o) => o.status === "OPEN").length;
  const cookingCount = orders.filter((o) => o.status === "SERVING").length;

  return (
    <>
      <PageHeader
        icon="👨‍🍳"
        title="Màn hình bếp"
        subtitle="Theo dõi các đơn đang mở và đơn đã được nhận chế biến"
      >
        <button
          type="button"
          className="adm-btn ghost"
          onClick={() => load(true)}
          disabled={refreshing}
        >
          {refreshing ? "Đang tải…" : "↻ Làm mới"}
        </button>
      </PageHeader>

      <div className="adm-stats">
        <div className="adm-stat">
          <span className="adm-muted">Chờ nhận</span>
          <b>{waitingCount}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Đang chế biến / phục vụ</span>
          <b>{cookingCount}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Tổng đơn đang mở</span>
          <b>{orders.length}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Cập nhật lần cuối</span>
          <b style={{ fontSize: 14 }}>
            {lastUpdated ? lastUpdated.toLocaleTimeString("vi-VN") : "—"}
          </b>
        </div>
      </div>

      <div className="adm-tools" style={{ margin: "16px 0" }}>
        <input
          className="adm-input"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm theo mã đơn hoặc bàn…"
          style={{ minWidth: 240 }}
        />
        <select
          className="adm-input"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          <option value="ALL">Tất cả trạng thái</option>
          <option value="OPEN">Chờ nhận</option>
          <option value="SERVING">Đang chế biến / phục vụ</option>
        </select>
        <span className="adm-muted">Tự làm mới mỗi 20 giây</span>
      </div>

      <StateBox loading={loading} error={error} onRetry={() => load()} />
      {!loading && !error && visibleOrders.length === 0 && (
        <div className="adm-card adm-pad" style={{ textAlign: "center" }}>
          <div style={{ fontSize: 36 }}>🍲</div>
          <h3>Không có đơn cần chế biến</h3>
          <p className="adm-muted">
            Đơn mới sẽ xuất hiện tại đây khi nhân viên tạo đơn cho bàn.
          </p>
        </div>
      )}

      {!error && visibleOrders.length > 0 && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns:
              "repeat(auto-fit, minmax(min(100%, 330px), 1fr))",
            gap: 16,
          }}
        >
          {visibleOrders.map((order) => {
            const lines = itemsByOrder[order.id] || [];
            const tableLabel =
              order.tableId == null
                ? "Khách vãng lai"
                : `Bàn #${order.tableId}`;
            return (
              <article
                className="adm-card"
                key={order.id}
                style={{
                  padding: 18,
                  borderTop: `4px solid ${order.status === "OPEN" ? "var(--amber)" : "var(--green)"}`,
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: 12,
                    alignItems: "flex-start",
                  }}
                >
                  <div>
                    <h3 style={{ margin: 0 }}>Đơn #{order.id}</h3>
                    <p className="adm-muted" style={{ margin: "4px 0" }}>
                      {tableLabel} · {dt(order.createdAt)}
                    </p>
                  </div>
                  <Badge value={order.status} />
                </div>
                <div
                  style={{
                    borderTop: "1px solid var(--line)",
                    marginTop: 12,
                    paddingTop: 8,
                  }}
                >
                  {lines.length === 0 ? (
                    <p className="adm-muted">Đơn chưa có món.</p>
                  ) : (
                    lines.map((line, index) => {
                      const dish = menu.find(
                        (m) => Number(m.id) === Number(line.menuItemId),
                      );
                      return (
                        <div
                          key={`${line.menuItemId}-${index}`}
                          style={{
                            display: "flex",
                            justifyContent: "space-between",
                            gap: 12,
                            padding: "9px 0",
                            borderBottom: "1px dashed var(--line)",
                          }}
                        >
                          <div>
                            <b>{dish?.nameVi || `Món #${line.menuItemId}`}</b>
                            {line.note && (
                              <div className="adm-muted">📝 {line.note}</div>
                            )}
                          </div>
                          <b style={{ whiteSpace: "nowrap" }}>
                            × {line.quantity}
                          </b>
                        </div>
                      );
                    })
                  )}
                </div>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: 12,
                    marginTop: 14,
                    alignItems: "center",
                  }}
                >
                  <b>
                    {vnd(
                      lines.reduce(
                        (sum, line) =>
                          sum +
                          Number(line.unitPrice || 0) *
                            Number(line.quantity || 0),
                        0,
                      ),
                    )}
                  </b>
                  <div className="adm-tools">
                    <Link
                      className="adm-btn ghost sm"
                      href={`/admin/orders/${order.id}`}
                    >
                      Chi tiết
                    </Link>
                    {order.status === "OPEN" && (
                      <button
                        type="button"
                        className="adm-btn sm"
                        disabled={updating === order.id || lines.length === 0}
                        onClick={() => changeStatus(order, "SERVING")}
                      >
                        {updating === order.id ? "Đang lưu…" : "Nhận chế biến"}
                      </button>
                    )}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </>
  );
}
