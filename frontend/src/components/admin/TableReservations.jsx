'use client';
import Link from 'next/link';
import { dt } from '@/lib/api';
import { useApi, useLookups } from '@/lib/hooks';
import { LK, nameOf } from '@/lib/admin/resources';
import Badge from './Badge';
import DataTable from './DataTable';
import { ReservationActions, isOverdue } from './RowWidgets';

const DEFS = { customers: LK.customers };

// Phần mở rộng ở trang chi tiết bàn: các lượt đặt của bàn này
export default function TableReservations({ tableId }) {
  const { data, error, reload } = useApi(`/reservations?tableId=${tableId}`);
  const { lk } = useLookups(DEFS);
  const rows = [...(data || [])].sort((a, b) => new Date(b.reservedAt) - new Date(a.reservedAt));

  return (
    <section className="adm-section" style={{ padding: 0 }}>
      <h3 style={{ marginBottom: 10 }}>📅 Lượt đặt của bàn này
        <Link href="/admin/reservations/create" className="adm-btn sm">＋ Đặt bàn</Link></h3>
      {error && <div className="adm-alert">{error}</div>}
      {data && (
        <DataTable rows={rows} rowHref={(r) => `/admin/reservations/${r.id}`} empty="Bàn này chưa có lượt đặt nào." ctx={{ lk, reload }}
          columns={[
            { key: 'reservedAt', label: 'Thời gian', render: (r) => <span className="adm-nowrap">{dt(r.reservedAt)}</span> },
            { key: 'customerId', label: 'Khách hàng', render: (r) => nameOf(lk, 'customers', r.customerId) },
            { key: 'guestCount', label: 'Số khách', align: 'right' },
            { key: 'status', label: 'Trạng thái', render: (r) => <><Badge value={r.status} />{isOverdue(r) && <> <Badge label="Quá giờ" color="red" /></>}</> },
            { key: 'act', label: '', render: (r) => <div className="adm-row-actions"><ReservationActions r={r} reload={reload} /></div> },
          ]} />
      )}
    </section>
  );
}
