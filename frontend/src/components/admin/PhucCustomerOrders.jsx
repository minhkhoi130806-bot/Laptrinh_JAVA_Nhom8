'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { api, dt, vnd } from '@/lib/api';
import { LK, nameOf } from '@/lib/admin/resources';
import Badge from './Badge';
import DataTable from './DataTable';

const DEFS = { tables: LK.tables };

// Phần mở rộng ở trang chi tiết khách hàng: các đơn đã mua + lịch sử đặt bàn
export default function CustomerOrders({ customerId }) {
  const [orders, setOrders] = useState(null);
  const [ordersError, setOrdersError] = useState('');
  const [reservations, setReservations] = useState(null);
  const [reservationsError, setReservationsError] = useState('');
  const [lk, setLk] = useState({ tables: { rows: [], opts: [], map: {} } });

  useEffect(() => {
    const fetchCustomerHistory = async () => {
      try {
        setOrdersError('');
        setReservationsError('');
        const results = await Promise.all([
          api(`/orders?customerId=${customerId}`),
          api(`/reservations?customerId=${customerId}`),
          api(DEFS.tables.endpoint),
        ]);
        setOrders(results[0]);
        setReservations(results[1]);
        const tables = results[2];
        setLk({ tables: {
          rows: tables,
          opts: tables.filter((item) => !item.deleted).map((item) => ({ value: item.id, label: DEFS.tables.label(item) })),
          map: Object.fromEntries(tables.map((item) => [item.id, DEFS.tables.label(item)])),
        } });
      } catch (err) {
        const message = err.message || 'Không thể tải lịch sử khách hàng.';
        setOrdersError(message);
        setReservationsError(message);
      }
    };

    fetchCustomerHistory();
  }, [customerId]);

  const list = orders || [];
  const paid = list.filter((o) => o.status === 'PAID');
  const spent = paid.reduce((s, o) => s + Number(o.total || 0), 0);

  return (
    <>
      <div className="adm-stats">
        <div className="adm-stat"><span className="adm-muted">Tổng số đơn</span><b>{list.length}</b></div>
        <div className="adm-stat"><span className="adm-muted">Đơn đã thanh toán</span><b>{paid.length}</b></div>
        <div className="adm-stat"><span className="adm-muted">Tổng chi tiêu</span><b>{vnd(spent)}</b></div>
      </div>

      <section className="adm-section" style={{ padding: 0 }}>
        <h3 style={{ marginBottom: 10 }}>🧾 Đơn hàng đã mua</h3>
        {ordersError && <div className="adm-alert">{ordersError}</div>}
        {orders && (
          <DataTable rows={list} rowHref={(o) => `/admin/orders/${o.id}`} empty="Khách hàng này chưa có đơn hàng nào."
            columns={[
              { key: 'id', label: 'Mã đơn', render: (o) => <b>#{o.id}</b> },
              { key: 'createdAt', label: 'Thời gian', render: (o) => dt(o.createdAt) },
              { key: 'tableId', label: 'Bàn', render: (o) => nameOf(lk, 'tables', o.tableId) },
              { key: 'itemCount', label: 'Số món', align: 'right', render: (o) => o.itemCount ?? 0 },
              { key: 'total', label: 'Tổng tiền', align: 'right', render: (o) => <b>{vnd(o.total)}</b> },
              { key: 'status', label: 'Trạng thái', render: (o) => <Badge value={o.status} /> },
              { key: 'go', label: '', render: (o) => <Link href={`/admin/orders/${o.id}`} className="adm-btn ghost sm">Chi tiết</Link> },
            ]} />
        )}
      </section>

      <section className="adm-section" style={{ padding: 0, marginTop: 20 }}>
        <h3 style={{ marginBottom: 10 }}>📅 Lịch sử đặt bàn</h3>
        {reservationsError && <div className="adm-alert">{reservationsError}</div>}
        {reservations && (
          <DataTable rows={[...reservations].sort((a, b) => new Date(b.reservedAt) - new Date(a.reservedAt))}
            rowHref={(r) => `/admin/reservations/${r.id}`} empty="Chưa có lượt đặt bàn nào."
            columns={[
              { key: 'id', label: 'Mã', render: (r) => `#${r.id}` },
              { key: 'reservedAt', label: 'Thời gian', render: (r) => dt(r.reservedAt) },
              { key: 'tableId', label: 'Bàn', render: (r) => nameOf(lk, 'tables', r.tableId) },
              { key: 'guestCount', label: 'Số khách', align: 'right' },
              { key: 'status', label: 'Trạng thái', render: (r) => <Badge value={r.status} /> },
            ]} />
        )}
      </section>
    </>
  );
}
