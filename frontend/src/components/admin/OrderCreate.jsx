'use client';
import { useMemo, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { api, toInputDateTime, vnd } from '@/lib/api';
import { useLookups } from '@/lib/hooks';
import { LK } from '@/lib/admin/resources';
import PageHeader from './PageHeader';
import StateBox from './StateBox';
import Thumb from './Thumb';
import { useToast } from './Feedback';

const DEFS = { customers: LK.customers, tables: LK.tables, employees: LK.employees, menu: LK.menu };

// Tạo đơn tại bàn: chọn bàn / khách / nhân viên + bấm món để thêm vào giỏ
export default function OrderCreate() {
  const router = useRouter();
  const sp = useSearchParams(); // từ "Nhận bàn" của đặt bàn: /admin/orders/create?tableId=3&customerId=7
  const toast = useToast();
  const { lk, ready, error, reload } = useLookups(DEFS);
  const [info, setInfo] = useState(() => ({ customerId: sp.get('customerId') || '', tableId: sp.get('tableId') || '', employeeId: '' }));
  const [cart, setCart] = useState([]); // [{ id, name, price, qty, note }]
  const [q, setQ] = useState('');
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');

  const dishes = useMemo(() => (lk.menu?.rows || [])
    .filter((m) => m.available && !m.deleted)
    .filter((m) => !q || `${m.nameVi} ${m.nameKo ?? ''}`.toLowerCase().includes(q.toLowerCase())), [lk, q]);
  const total = cart.reduce((s, l) => s + l.qty * l.price, 0);

  const add = (m) => setCart((c) => (c.some((l) => l.id === m.id)
    ? c.map((l) => (l.id === m.id ? { ...l, qty: l.qty + 1 } : l))
    : [...c, { id: m.id, name: m.nameVi, price: Number(m.price), qty: 1, note: '' }]));
  const change = (id, d) => setCart((c) => c.map((l) => (l.id === id ? { ...l, qty: l.qty + d } : l)).filter((l) => l.qty > 0));
  const note = (id, v) => setCart((c) => c.map((l) => (l.id === id ? { ...l, note: v } : l)));

  const submit = async () => {
    if (!info.tableId) return setErr('Vui lòng chọn bàn');
    if (cart.length === 0) return setErr('Giỏ món đang trống');
    setSaving(true); setErr('');
    let order;
    try {
      order = await api('/orders', {
        method: 'POST',
        body: {
          customerId: info.customerId ? Number(info.customerId) : null,
          tableId: Number(info.tableId),
          employeeId: info.employeeId ? Number(info.employeeId) : null,
          createdAt: toInputDateTime(), status: 'OPEN',
        },
      });
    } catch (e) { setSaving(false); return setErr(e.message); }

    // Thêm lần lượt từng món vào đơn vừa tạo
    const failed = [];
    for (const l of cart) {
      try {
        await api('/order-items', { method: 'POST', body: { orderId: order.id, menuItemId: l.id, quantity: l.qty, unitPrice: l.price, note: l.note || null } });
      } catch { failed.push(l.name); }
    }
    // Bàn có khách -> đang dùng (không bắt buộc, lỗi cũng bỏ qua)
    try { await api(`/tables/${info.tableId}/status?value=OCCUPIED`, { method: 'PATCH' }); } catch {}

    toast(failed.length ? `Đã tạo đơn #${order.id} nhưng lỗi thêm: ${failed.join(', ')}` : `Đã tạo đơn #${order.id}`, failed.length ? 'err' : 'ok');
    router.push(`/admin/orders/${order.id}`);
  };

  return (
    <>
      <PageHeader icon="🧾" title="Tạo đơn mới" back={{ href: '/admin/orders', label: 'Đơn hàng' }} />
      <StateBox loading={!ready && !error} error={error} onRetry={reload} />
      {ready && (
        <div className="adm-pos">
          <section>
            <div className="adm-tools" style={{ marginBottom: 12 }}>
              <input className="adm-input" style={{ width: '100%' }} placeholder="Tìm món…" value={q} onChange={(e) => setQ(e.target.value)} />
            </div>
            {dishes.length === 0 ? <div className="adm-empty">Không có món nào đang bán.</div> : (
              <div className="adm-picker">
                {dishes.map((m) => (
                  <button type="button" key={m.id} className="adm-pick" onClick={() => add(m)}>
                    <span className="adm-pick-img">{m.imageUrl
                      // eslint-disable-next-line @next/next/no-img-element
                      ? <img src={m.imageUrl} alt={m.nameVi} loading="lazy" /> : '🍽️'}</span>
                    <b>{m.nameVi}</b>
                    <span className="adm-muted">{vnd(m.price)}</span>
                  </button>
                ))}
              </div>
            )}
          </section>

          <aside className="adm-card adm-section adm-cart">
            <h3>Thông tin đơn</h3>
            <div className="adm-form-grid" style={{ gridTemplateColumns: '1fr', marginBottom: 14 }}>
              <label className="adm-field"><span className="adm-label">Bàn <span className="adm-req">*</span></span>
                <select className="adm-input" value={info.tableId} onChange={(e) => setInfo({ ...info, tableId: e.target.value })}>
                  <option value="">— Chọn bàn —</option>
                  {lk.tables.rows.filter((t) => !t.deleted).map((t) => <option key={t.id} value={t.id}>{t.tableCode} ({t.capacity} chỗ){t.status !== 'AVAILABLE' ? ' – đang có khách/đặt' : ''}</option>)}
                </select></label>
              <label className="adm-field"><span className="adm-label">Khách hàng</span>
                <select className="adm-input" value={info.customerId} onChange={(e) => setInfo({ ...info, customerId: e.target.value })}>
                  <option value="">Khách vãng lai</option>
                  {lk.customers.opts.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select></label>
              <label className="adm-field"><span className="adm-label">Nhân viên phụ trách</span>
                <select className="adm-input" value={info.employeeId} onChange={(e) => setInfo({ ...info, employeeId: e.target.value })}>
                  <option value="">— Không chọn —</option>
                  {lk.employees.opts.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select></label>
            </div>

            <h3>Món đã chọn</h3>
            {cart.length === 0 && <p className="adm-muted">Bấm vào món bên trái để thêm.</p>}
            {cart.map((l) => (
              <div className="adm-cart-line" key={l.id}>
                <div><b>{l.name}</b><div className="adm-muted">{vnd(l.price)}</div></div>
                <div className="adm-qty">
                  <button type="button" onClick={() => change(l.id, -1)}>−</button><span>{l.qty}</span><button type="button" onClick={() => change(l.id, 1)}>＋</button>
                </div>
                <input className="adm-input" style={{ gridColumn: '1 / -1' }} placeholder="Ghi chú (ít cay, không hành…)" value={l.note} onChange={(e) => note(l.id, e.target.value)} />
              </div>
            ))}
            <div className="adm-sum"><div className="adm-grand"><span>Tạm tính</span><span>{vnd(total)}</span></div></div>
            {err && <div className="adm-alert">{err}</div>}
            <div className="adm-form-f">
              <button type="button" className="adm-btn ghost" onClick={() => router.push('/admin/orders')}>Hủy</button>
              <button type="button" className="adm-btn" disabled={saving} onClick={submit}>{saving ? 'Đang tạo…' : 'Tạo đơn'}</button>
            </div>
          </aside>
        </div>
      )}
    </>
  );
}
