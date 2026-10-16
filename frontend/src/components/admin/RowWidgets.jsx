'use client';
import { useRouter } from 'next/navigation';
import { api, dt } from '@/lib/api';
import { TABLE_STATUSES } from '@/lib/constants';
import Badge from './Badge';
import { useConfirm, useToast } from './Feedback';

const HOLD_MIN = 30;

// Lượt đặt còn hiệu lực nhưng đã quá giờ hẹn > 30 phút (khách chưa đến)
export const isOverdue = (r) =>
  ['PENDING', 'CONFIRMED'].includes(r.status) && new Date(r.reservedAt).getTime() < Date.now() - HOLD_MIN * 60000;

// Lượt đặt sắp tới gần nhất của 1 bàn (chưa quá hạn)
export const nextReservation = (reservations, tableId) =>
  (reservations || [])
    .filter((r) => r.tableId === tableId && ['PENDING', 'CONFIRMED'].includes(r.status)
      && new Date(r.reservedAt).getTime() >= Date.now() - HOLD_MIN * 60000)
    .sort((a, b) => new Date(a.reservedAt) - new Date(b.reservedAt))[0];

// Công tắc bật/tắt bán món ngay trong danh sách
export function AvailableSwitch({ row, reload }) {
  const toast = useToast();
  const toggle = async (e) => {
    try {
      await api(`/menu-items/${row.id}/available?value=${e.target.checked}`, { method: 'PATCH' });
      reload();
    } catch (ex) { toast(ex.message, 'err'); }
  };
  return (
    <label className="adm-switch" title={row.available ? 'Đang bán' : 'Tạm ngưng'}>
      <input type="checkbox" checked={Boolean(row.available)} onChange={toggle} /><span />
    </label>
  );
}

// Thẻ bàn trong sơ đồ bàn: đổi trạng thái nhanh + hiện lượt đặt sắp tới của bàn
export function TableCard({ t, reload, lk }) {
  const toast = useToast();
  const setStatus = async (s) => {
    try { await api(`/tables/${t.id}/status?value=${s}`, { method: 'PATCH' }); reload(); }
    catch (e) { toast(e.message, 'err'); }
  };
  const next = nextReservation(lk?.reservations?.rows, t.id);
  return (
    <div className={`adm-tcard ${t.status}`}>
      <div className="adm-tcard-top"><b className="adm-tcard-code">{t.tableCode}</b><Badge value={t.status} /></div>
      <div className="adm-muted">👥 {t.capacity} chỗ · {t.zone || 'Chưa chia khu'}</div>
      {next && (
        <div className="adm-next">
          📅 {dt(next.reservedAt)} · {lk?.customers?.map?.[next.customerId] ?? `Khách #${next.customerId}`} ({next.guestCount})
          {next.status === 'PENDING' && <span className="adm-muted"> · chờ xác nhận</span>}
        </div>
      )}
      <div className="adm-seg" onClick={(e) => e.stopPropagation()}>
        {TABLE_STATUSES.map((s) => (
          <button key={s} type="button" className={t.status === s ? 'on' : ''} disabled={t.status === s} onClick={() => setStatus(s)}>
            <Badge value={s} />
          </button>
        ))}
      </div>
    </div>
  );
}

// Nút xử lý đặt bàn: Xác nhận -> Nhận bàn (khách đến) / Hoàn tất / Hủy
export function ReservationActions({ r, reload }) {
  const toast = useToast();
  const confirm = useConfirm();
  const router = useRouter();
  if (!['PENDING', 'CONFIRMED'].includes(r.status)) return null;

  const setStatus = (status, msg) => async () => {
    if (status === 'CANCELLED' && !(await confirm({ title: 'Hủy lượt đặt', message: `Hủy lượt đặt #${r.id}? Bàn sẽ được trả lại nếu đang giữ chỗ.`, okText: 'Hủy đặt', danger: true }))) return;
    try {
      await api(`/reservations/${r.id}/status`, { method: 'PATCH', body: { status } });
      toast(msg); reload();
    } catch (e) { toast(e.message, 'err'); }
  };

  const checkIn = async () => {
    try {
      await api(`/reservations/${r.id}/check-in`, { method: 'POST' });
      toast('Khách đã đến — bàn chuyển sang "Đang dùng"');
      reload();
      if (await confirm({ title: 'Tạo đơn cho bàn này?', message: 'Mở trang tạo đơn với bàn và khách đã điền sẵn?', okText: 'Tạo đơn ngay' })) {
        router.push(`/admin/orders/create?tableId=${r.tableId}&customerId=${r.customerId}`);
      }
    } catch (e) { toast(e.message, 'err'); }
  };

  return (
    <>
      {r.status === 'PENDING' && <button className="adm-btn info sm" onClick={setStatus('CONFIRMED', 'Đã xác nhận lượt đặt')}>Xác nhận</button>}
      <button className="adm-btn ok sm" onClick={checkIn}>Nhận bàn</button>
      <button className="adm-btn ghost sm" onClick={setStatus('CANCELLED', 'Đã hủy lượt đặt')}>Hủy đặt</button>
    </>
  );
}
