'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api, dt, toInputDateTime } from '@/lib/api';
import { LK } from '@/lib/admin/resources';
import Badge from './Badge';
import PageHeader from './PageHeader';
import StateBox from './StateBox';
import { ReservationActions, isOverdue } from './RowWidgets';
import { useToast } from './Feedback';

const DEFS = { customers: LK.customers, tables: LK.tables };

// Gợi ý mặc định: giờ tròn kế tiếp, cách hiện tại ít nhất 1 tiếng
const defaultTime = () => { const d = new Date(Date.now() + 3600e3); d.setMinutes(0, 0, 0); d.setHours(d.getHours() + 1); return toInputDateTime(d); };

function Body({ record, lk, onSaved }) {
  const router = useRouter();
  const toast = useToast();
  const isNew = !record;
  const locked = !isNew && !['PENDING', 'CONFIRMED'].includes(record.status);

  const [f, setF] = useState(() => ({
    customerId: record?.customerId ?? '', tableId: record?.tableId ?? '', guestCount: record?.guestCount ?? 2,
    reservedAt: record ? String(record.reservedAt).slice(0, 16) : defaultTime(), note: record?.note ?? '',
  }));
  const [free, setFree] = useState(null); // null = đang tải
  const [extra, setExtra] = useState([]); // khách vừa thêm nhanh
  const [quick, setQuick] = useState(false);
  const [qc, setQc] = useState({ fullName: '', phone: '' });
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');
  const set = (k, v) => setF((s) => ({ ...s, [k]: v }));

  // Mỗi khi đổi giờ / số khách -> hỏi backend những bàn còn trống đủ chỗ
  useEffect(() => {
    if (locked || !f.reservedAt || !(Number(f.guestCount) > 0)) return;
    let alive = true;
    const q = `at=${encodeURIComponent(f.reservedAt)}&guests=${f.guestCount}${record ? `&ignoreId=${record.id}` : ''}`;
    api(`/reservations/available-tables?${q}`)
      .then((rows) => {
        if (!alive) return;
        setFree(rows);
        setF((s) => (s.tableId && !rows.some((t) => t.id === Number(s.tableId)) ? { ...s, tableId: '' } : s));
      })
      .catch((e) => alive && setErr(e.message));
    return () => { alive = false; };
  }, [f.reservedAt, f.guestCount, locked, record]);

  const addCustomer = async () => {
    try {
      const c = await api('/customers', { method: 'POST', body: { fullName: qc.fullName, phone: qc.phone } });
      setExtra((x) => [...x, { value: c.id, label: `${c.fullName} – ${c.phone}` }]);
      set('customerId', c.id); setQuick(false); setQc({ fullName: '', phone: '' }); toast('Đã thêm khách hàng mới');
    } catch (e) { toast(e.message, 'err'); }
  };

  const submit = async (e) => {
    e.preventDefault(); setErr('');
    if (!f.tableId) return setErr('Vui lòng chọn bàn');
    setSaving(true);
    try {
      const body = { customerId: Number(f.customerId), tableId: Number(f.tableId), reservedAt: f.reservedAt, guestCount: Number(f.guestCount), note: f.note.trim() || null };
      const saved = await api(isNew ? '/reservations' : `/reservations/${record.id}`, { method: isNew ? 'POST' : 'PUT', body });
      toast(isNew ? 'Đã tạo lượt đặt bàn' : 'Đã lưu thay đổi');
      if (isNew) router.push(`/admin/reservations/${saved.id}`); else onSaved();
    } catch (ex) { setErr(ex.message); } finally { setSaving(false); }
  };

  // Bàn hiện tại của lượt (đang sửa) luôn có trong danh sách để hiển thị, dù đã bị loại do đổi giờ
  const current = record && lk.tables.rows.find((t) => t.id === record.tableId);
  const tables = locked ? (current ? [current] : []) : free;
  const customerOpts = [...lk.customers.opts, ...extra];

  return (
    <form onSubmit={submit} className="adm-card adm-form-card adm-section">
      {locked && <div className="adm-alert warn">Lượt đặt đã kết thúc ({record.status === 'COMPLETED' ? 'hoàn tất / khách đã đến' : 'đã hủy'}) nên không thể chỉnh sửa.</div>}
      {!locked && !isNew && isOverdue(record) && <div className="adm-alert warn">Đã quá giờ hẹn hơn 30 phút mà khách chưa đến. Bạn có thể “Nhận bàn” nếu khách vừa tới, hoặc “Hủy đặt” để trả bàn.</div>}

      <div className="adm-form-grid">
        <div className="adm-field full">
          <span className="adm-label">Khách hàng <span className="adm-req">*</span></span>
          <select className="adm-input" required disabled={locked} value={f.customerId} onChange={(e) => set('customerId', e.target.value)}>
            <option value="">— Chọn khách hàng —</option>
            {customerOpts.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
          {!locked && !quick && <button type="button" className="adm-link" style={{ alignSelf: 'flex-start', background: 'none', border: 0, cursor: 'pointer', padding: 0 }} onClick={() => setQuick(true)}>＋ Khách mới (chưa có trong danh sách)</button>}
          {quick && (
            <div className="adm-inline-add">
              <input className="adm-input" placeholder="Họ tên" value={qc.fullName} onChange={(e) => setQc({ ...qc, fullName: e.target.value })} />
              <input className="adm-input" placeholder="Số điện thoại" value={qc.phone} onChange={(e) => setQc({ ...qc, phone: e.target.value })} />
              <button type="button" className="adm-btn sm" disabled={!qc.fullName.trim() || !qc.phone.trim()} onClick={addCustomer}>Thêm</button>
              <button type="button" className="adm-btn ghost sm" onClick={() => setQuick(false)}>Hủy</button>
            </div>
          )}
        </div>

        <label className="adm-field"><span className="adm-label">Thời gian đặt <span className="adm-req">*</span></span>
          <input className="adm-input" type="datetime-local" required disabled={locked} min={isNew ? toInputDateTime() : undefined}
            value={f.reservedAt} onChange={(e) => set('reservedAt', e.target.value)} /></label>
        <label className="adm-field"><span className="adm-label">Số khách <span className="adm-req">*</span></span>
          <input className="adm-input" type="number" min="1" required disabled={locked} value={f.guestCount} onChange={(e) => set('guestCount', e.target.value)} /></label>

        <div className="adm-field full">
          <span className="adm-label">Bàn <span className="adm-req">*</span> <span className="adm-muted">— chỉ hiện bàn còn trống vào giờ này và đủ chỗ</span></span>
          {tables === null && <span className="adm-muted">Đang kiểm tra bàn trống…</span>}
          {tables && tables.length === 0 && <div className="adm-alert warn" style={{ margin: 0 }}>Không còn bàn nào trống đủ {f.guestCount} chỗ vào giờ này. Hãy đổi giờ hoặc số khách.</div>}
          {tables && tables.length > 0 && (
            <div className="adm-table-pick">
              {tables.map((t) => (
                <button type="button" key={t.id} disabled={locked} className={`adm-table-opt ${Number(f.tableId) === t.id ? 'on' : ''}`} onClick={() => set('tableId', t.id)}>
                  <b>{t.tableCode}</b><span className="adm-muted">👥 {t.capacity} · {t.zone}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        <label className="adm-field full"><span className="adm-label">Ghi chú</span>
          <textarea className="adm-input" rows={2} disabled={locked} placeholder="Sinh nhật, cần ghế trẻ em, yêu cầu riêng…" value={f.note} onChange={(e) => set('note', e.target.value)} /></label>
      </div>

      {err && <div className="adm-alert">{err}</div>}
      <div className="adm-form-f">
        <button type="button" className="adm-btn ghost" onClick={() => router.push('/admin/reservations')}>{locked ? 'Quay lại' : 'Hủy'}</button>
        {!locked && <button className="adm-btn" disabled={saving}>{saving ? 'Đang lưu…' : isNew ? 'Tạo lượt đặt' : 'Lưu thay đổi'}</button>}
      </div>
    </form>
  );
}

// Trang TẠO (không có id) và CHI TIẾT + SỬA (có id) của đặt bàn
export default function ReservationForm({ id }) {
  const isNew = id == null;
  const [record, setRecord] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(!isNew);
  const [lk, setLk] = useState({});
  const [ready, setReady] = useState(false);
  const [lkError, setLkError] = useState('');

  // Tải lượt đặt và dữ liệu khách/bàn trực tiếp tại trang.
  const reload = async () => {
    try {
      setError('');
      setLkError('');
      setLoading(!isNew);
      setReady(false);

      const requests = [];
      if (!isNew) requests.push(api(`/reservations/${id}`));
      requests.push(api(DEFS.customers.endpoint));
      requests.push(api(DEFS.tables.endpoint));
      const results = await Promise.all(requests);

      let index = 0;
      if (!isNew) {
        setRecord(results[0]);
        index = 1;
      } else {
        setRecord(null);
      }

      const customers = results[index];
      const tables = results[index + 1];
      setLk({
        customers: {
          rows: customers,
          opts: customers.filter((item) => !item.deleted).map((item) => ({ value: item.id, label: DEFS.customers.label(item) })),
          map: Object.fromEntries(customers.map((item) => [item.id, DEFS.customers.label(item)])),
        },
        tables: {
          rows: tables,
          opts: tables.filter((item) => !item.deleted).map((item) => ({ value: item.id, label: DEFS.tables.label(item) })),
          map: Object.fromEntries(tables.map((item) => [item.id, DEFS.tables.label(item)])),
        },
      });
      setReady(true);
    } catch (err) {
      setError(err.message || 'Không thể tải lượt đặt bàn.');
      setLkError(err.message || 'Không thể tải danh sách khách hàng và bàn.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload();
    // Tải lại khi chuyển giữa trang tạo mới và trang chi tiết.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  return (
    <>
      <PageHeader icon="📅" title={isNew ? 'Đặt bàn mới' : `Lượt đặt #${id}`} back={{ href: '/admin/reservations', label: 'Đặt bàn' }}
        subtitle={record ? `${dt(record.reservedAt)} · bàn ${lk.tables?.map?.[record.tableId] ?? record.tableId}` : undefined}>
        {record && <Badge value={record.status} />}
        {record && <ReservationActions r={record} reload={reload} />}
        {record && <Link href={`/admin/customers/${record.customerId}`} className="adm-btn ghost">Xem khách hàng</Link>}
      </PageHeader>
      <StateBox loading={loading || (!ready && !lkError)} error={error || lkError} onRetry={reload} />
      {ready && (isNew || record) && !error && <Body key={record?.id ?? 'new'} record={record} lk={lk} onSaved={reload} />}
    </>
  );
}
