'use client';
import Link from 'next/link';
import { dt, vnd } from '@/lib/api';
import { useApi, useLookups } from '@/lib/hooks';
import { LK, nameOf } from '@/lib/admin/resources';
import Badge from './Badge';
import DataTable from './DataTable';

const DEFS = { tables: LK.tables };

// Phần mở rộng ở trang chi tiết khách hàng: các đơn đã mua + lịch sử đặt bàn
export default function CustomerOrders({ customerId }) {
  const orders = useApi(`/orders?customerId=${customerId}`);
  const resv = useApi(`/reservations?customerId=${customerId}`);
  const { lk } = useLookups(DEFS);

  const list = orders.data || [];
  const paid = list.filter((o) => o.status === 'PAID');
  const spent = paid.reduce((s, o) => s + Number(o.total || 0), 0);

  return (
    <>
      <div className="adm-stats">
        <div className="adm-stat"><span className="adm-muted">Tổng số đơn</span><b>{list.length}</b></div>
        <div className="adm-stat"><span className="adm-muted">Đơn đã thanh toán</span><b>{paid.length}</b></div>
        <div className="adm-stat"><span className="adm-muted">Tổng chi tiêu</span><b>{vnd(spent)}</b></div>
        <div className="adm-stat"><span className="adm-muted">Lượt đặt bàn</span><b>{resv.data?.length ?? 0}</b></div>
      </div>

      <section className="adm-section" style={{ padding: 0 }}>
        <h3 style={{ marginBottom: 10 }}>🧾 Đơn hàng đã mua</h3>
        {orders.error && <div className="adm-alert">{orders.error}</div>}
        {orders.data && (
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
        {resv.loading && <div className="adm-muted">Đang tải lịch sử đặt bàn…</div>}
        {resv.error && <div className="adm-alert">{resv.error}</div>}
        {resv.data && (
          <DataTable rows={[...resv.data].sort((a, b) => new Date(b.reservedAt) - new Date(a.reservedAt))}
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
