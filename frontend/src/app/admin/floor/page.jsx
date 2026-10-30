"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { STATUS } from "@/lib/constants";
import PageHeader from "@/components/admin/PageHeader";
import StateBox from "@/components/admin/StateBox";
import Badge from "@/components/admin/Badge";
import { useToast } from "@/components/admin/Feedback";

const TABLE_STATUS_OPTIONS = [
  { value: "AVAILABLE", label: "Đánh dấu trống" },
  { value: "RESERVED", label: "Đánh dấu đã đặt" },
  { value: "OCCUPIED", label: "Đánh dấu đang dùng" },
];

export default function FloorPage() {
  const toast = useToast();
  const [tables, setTables] = useState([]);
  const [orders, setOrders] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [tableRows, orderRows, reservationRows] = await Promise.all([
        api("/tables"),
        api("/orders"),
        api("/reservations"),
      ]);
      setTables((tableRows || []).filter((t) => !t.deleted));
      setOrders(
        (orderRows || []).filter((o) => ["OPEN", "SERVING"].includes(o.status)),
      );
      setReservations(
        (reservationRows || []).filter((r) =>
          ["PENDING", "CONFIRMED"].includes(r.status),
        ),
      );
    } catch (e) {
      setError(e.message || "Không thể tải sơ đồ bàn.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const orderByTable = useMemo(() => {
    const result = {};
    orders.forEach((order) => {
      if (order.tableId != null)
        result[order.tableId] = [...(result[order.tableId] || []), order];
    });
    return result;
  }, [orders]);

  const reservationByTable = useMemo(() => {
    const result = {};
    reservations.forEach((reservation) => {
      if (reservation.tableId != null)
        result[reservation.tableId] = [
          ...(result[reservation.tableId] || []),
          reservation,
        ];
    });
    return result;
  }, [reservations]);

  const filtered = tables.filter((table) => {
    const matchesStatus =
      statusFilter === "ALL" || table.status === statusFilter;
    const term = query.trim().toLocaleLowerCase("vi");
    return (
      matchesStatus &&
      (!term ||
        `${table.tableCode} ${table.capacity}`
          .toLocaleLowerCase("vi")
          .includes(term))
    );
  });

  const updateStatus = async (table, status) => {
    setBusyId(table.id);
    try {
      await api(`/tables/${table.id}/status?value=${status}`, {
        method: "PATCH",
      });
      toast(`Đã cập nhật ${table.tableCode}: ${STATUS[status]?.[0] || status}`);
      await load();
    } catch (e) {
      toast(e.message || "Không thể cập nhật bàn.", "err");
    } finally {
      setBusyId(null);
    }
  };

  const release = async (table) => {
    setBusyId(table.id);
    try {
      await api(`/tables/${table.id}/release`, { method: "POST" });
      toast(`Đã trả ${table.tableCode} về trạng thái phù hợp.`);
      await load();
    } catch (e) {
      toast(e.message || "Không thể trả bàn.", "err");
    } finally {
      setBusyId(null);
    }
  };

  const counts = tables.reduce((acc, table) => {
    acc[table.status] = (acc[table.status] || 0) + 1;
    return acc;
  }, {});

  return (
    <>
      <PageHeader
        icon="🪑"
        title="Sơ đồ bàn nhanh"
        subtitle="Xem trạng thái bàn, đơn đang mở và lượt đặt bàn sắp tới"
      >
        <button className="adm-btn ghost" onClick={load} disabled={loading}>
          {loading ? "Đang tải…" : "↻ Làm mới"}
        </button>
      </PageHeader>

      <div className="adm-stats">
        <div className="adm-stat">
          <span className="adm-muted">Tổng số bàn</span>
          <b>{tables.length}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Đang trống</span>
          <b>{counts.AVAILABLE || 0}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Đã đặt</span>
          <b>{counts.RESERVED || 0}</b>
        </div>
        <div className="adm-stat">
          <span className="adm-muted">Đang dùng</span>
          <b>{counts.OCCUPIED || 0}</b>
        </div>
      </div>

      <div className="adm-tools" style={{ margin: "16px 0" }}>
        <input
          className="adm-input"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm mã bàn hoặc sức chứa…"
        />
        <select
          className="adm-input"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">Tất cả trạng thái</option>
          {TABLE_STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {STATUS[option.value]?.[0]}
            </option>
          ))}
        </select>
        <Link className="adm-btn ghost" href="/admin/tables">
          Quản lý bàn
        </Link>
      </div>

      <StateBox loading={loading} error={error} onRetry={load} />
      {!loading && !error && filtered.length === 0 && (
        <div className="adm-card adm-pad">
          Không tìm thấy bàn phù hợp với bộ lọc.
        </div>
      )}

      {!error && filtered.length > 0 && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns:
              "repeat(auto-fit, minmax(min(100%, 245px), 1fr))",
            gap: 14,
          }}
        >
          {filtered.map((table) => {
            const activeOrders = orderByTable[table.id] || [];
            const activeReservations = reservationByTable[table.id] || [];
            const tone =
              table.status === "AVAILABLE"
                ? "var(--green)"
                : table.status === "RESERVED"
                  ? "var(--amber)"
                  : "var(--red)";
            return (
              <article
                key={table.id}
                className="adm-card"
                style={{ padding: 18, borderTop: `4px solid ${tone}` }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    gap: 8,
                    alignItems: "center",
                  }}
                >
                  <div>
                    <div style={{ fontSize: 24 }}>🪑</div>
                    <h3 style={{ margin: "4px 0" }}>{table.tableCode}</h3>
                    <p className="adm-muted" style={{ margin: 0 }}>
                      {table.capacity} chỗ ngồi
                    </p>
                  </div>
                  <Badge value={table.status} />
                </div>
                <div
                  style={{
                    marginTop: 14,
                    padding: 10,
                    borderRadius: 10,
                    background: "var(--gray-bg)",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      gap: 8,
                    }}
                  >
                    <span className="adm-muted">Đơn đang mở</span>
                    <b>{activeOrders.length}</b>
                  </div>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      gap: 8,
                      marginTop: 5,
                    }}
                  >
                    <span className="adm-muted">Lượt đặt còn hiệu lực</span>
                    <b>{activeReservations.length}</b>
                  </div>
                </div>
                {activeOrders.length > 0 && (
                  <div style={{ marginTop: 10 }}>
                    {activeOrders.map((o) => (
                      <div key={o.id} className="adm-muted">
                        🧾{" "}
                        <Link
                          className="adm-link"
                          href={`/admin/orders/${o.id}`}
                        >
                          Đơn #{o.id}
                        </Link>{" "}
                        · {o.status === "OPEN" ? "Mới tạo" : "Đang phục vụ"}
                      </div>
                    ))}
                  </div>
                )}
                <div className="adm-tools" style={{ marginTop: 14 }}>
                  <Link
                    className="adm-btn sm"
                    href={`/admin/orders/create?tableId=${table.id}`}
                  >
                    ＋ Gọi món
                  </Link>
                  {table.status !== "AVAILABLE" && (
                    <button
                      className="adm-btn ghost sm"
                      disabled={busyId === table.id}
                      onClick={() => release(table)}
                    >
                      {busyId === table.id ? "Đang lưu…" : "Trả bàn"}
                    </button>
                  )}
                  <select
                    aria-label={`Đổi trạng thái ${table.tableCode}`}
                    className="adm-input"
                    style={{ flex: "1 1 100%" }}
                    value={table.status}
                    disabled={busyId === table.id}
                    onChange={(e) => updateStatus(table, e.target.value)}
                  >
                    {TABLE_STATUS_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </>
  );
}
