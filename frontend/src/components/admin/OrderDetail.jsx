'use client';
import { useState } from 'react';
import Link from 'next/link';
import { api, dt, toInputDateTime, vnd } from '@/lib/api';
import { useApi, useLookups } from '@/lib/hooks';
import { LK, nameOf } from '@/lib/admin/resources';
import { PAYMENT_METHODS, paymentLabel } from '@/lib/constants';
import Badge from './Badge';
import Modal from './Modal';
import PageHeader from './PageHeader';
import StateBox from './StateBox';
import Thumb from './Thumb';
import { useConfirm, useToast } from './Feedback';

const DEFS = { customers: LK.customers, tables: LK.tables, employees: LK.employees, menu: LK.menu };

// Cập nhật trạng thái bàn kèm theo đơn (không bắt buộc, lỗi thì bỏ qua)
// Trả bàn: backend tự chọn Trống hoặc Đã đặt (nếu sắp có khách đã xác nhận)
const releaseTable = (tableId) => (tableId == null ? null : api(`/tables/${tableId}/release`, { method: 'POST' }).catch(() => null));

function PayDialog({ order, subtotal, onClose, onDone }) {
  const toast = useToast();
  const [discount, setDiscount] = useState('0');
  const [method, setMethod] = useState('CASH');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const disc = Math.min(Math.max(Number(discount) || 0, 0), subtotal);
  const total = subtotal - disc;

  const pay = async (e) => {
    e.preventDefault(); setBusy(true); setErr('');
    try {
      await api('/invoices', { method: 'POST', body: { orderId: order.id, subtotal, discount: disc, total, paymentMethod: method, paidAt: toInputDateTime() + ':00' } });
      await api(`/orders/${order.id}/status`, { method: 'PATCH', body: { status: 'PAID' } });
      await releaseTable(order.tableId);
      toast('Đã thanh toán và tạo hóa đơn');
      onDone();
    } catch (ex) { setErr(ex.message); setBusy(false); }
  };

  return (
    <Modal title={`Thanh toán đơn #${order.id}`} onClose={onClose}>
      <form onSubmit={pay} className="adm-form-grid" style={{ gridTemplateColumns: '1fr' }}>
        <div className="adm-sum" style={{ marginTop: 0 }}><div><span>Tạm tính</span><b>{vnd(subtotal)}</b></div></div>
        <label className="adm-field"><span className="adm-label">Giảm giá (₫)</span>
          <input className="adm-input" type="number" min="0" max={subtotal} step="1000" value={discount} onChange={(e) => setDiscount(e.target.value)} /></label>
        <label className="adm-field"><span className="adm-label">Phương thức thanh toán</span>
          <select className="adm-input" value={method} onChange={(e) => setMethod(e.target.value)}>
            {PAYMENT_METHODS.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
          </select></label>
        <div className="adm-sum"><div className="adm-grand"><span>Khách phải trả</span><span>{vnd(total)}</span></div></div>
        {err && <div className="adm-alert">{err}</div>}
        <div className="adm-form-f">
          <button type="button" className="adm-btn ghost" onClick={onClose}>Hủy</button>
          <button className="adm-btn ok" disabled={busy}>{busy ? 'Đang xử lý…' : 'Xác nhận thanh toán'}</button>
        </div>
      </form>
    </Modal>
  );
}

function EditInfo({ order, lk, onClose, onDone }) {
  const toast = useToast();
  const [f, setF] = useState({ customerId: order.customerId ?? '', tableId: order.tableId ?? '', employeeId: order.employeeId ?? '' });
  const [err, setErr] = useState('');
  const save = async (e) => {
    e.preventDefault();
    try {
      await api(`/orders/${order.id}`, {
        method: 'PUT',
        body: {
          customerId: f.customerId === '' ? null : Number(f.customerId), tableId: f.tableId === '' ? null : Number(f.tableId),
          employeeId: f.employeeId === '' ? null : Number(f.employeeId), createdAt: order.createdAt, status: order.status,
        },
      });
      toast('Đã cập nhật đơn hàng'); onDone();
    } catch (ex) { setErr(ex.message); }
  };
  const sel = (key, label, opts, empty) => (
    <label className="adm-field"><span className="adm-label">{label}</span>
      <select className="adm-input" value={f[key]} onChange={(e) => setF({ ...f, [key]: e.target.value })}>
        <option value="">{empty}</option>{opts.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select></label>
  );
  return (
    <Modal title={`Sửa thông tin đơn #${order.id}`} onClose={onClose}>
      <form onSubmit={save} className="adm-form-grid" style={{ gridTemplateColumns: '1fr' }}>
        {sel('tableId', 'Bàn', lk.tables.opts, '— Chọn bàn —')}
        {sel('customerId', 'Khách hàng', lk.customers.opts, 'Khách vãng lai')}
        {sel('employeeId', 'Nhân viên phụ trách', lk.employees.opts, '— Không chọn —')}
        {err && <div className="adm-alert">{err}</div>}
        <div className="adm-form-f"><button type="button" className="adm-btn ghost" onClick={onClose}>Hủy</button><button className="adm-btn">Lưu</button></div>
      </form>
    </Modal>
  );
}

// Trang CHI TIẾT ĐƠN HÀNG: thông tin đơn + các món trong đơn + hóa đơn
export default function OrderDetail({ id }) {
  const toast = useToast();
  const confirm = useConfirm();
  const order = useApi(`/orders/${id}`);
  const items = useApi(`/order-items?orderId=${id}`);
  const inv = useApi(`/invoices?orderId=${id}`);
  const { lk, ready, error: lkError } = useLookups(DEFS);
  const [dlg, setDlg] = useState(null); // 'pay' | 'edit'

  const reloadAll = () => { order.reload(); items.reload(); inv.reload(); };
  const o = order.data;
  const lines = items.data || [];
  const subtotal = lines.reduce((s, l) => s + l.quantity * Number(l.unitPrice), 0);
  const invoice = inv.data?.[0];
  const open = o && ['OPEN', 'SERVING'].includes(o.status);

  const setStatus = async (status, msg) => {
    try {
      await api(`/orders/${id}/status`, { method: 'PATCH', body: { status } });
      if (status === 'CANCELLED') await releaseTable(o.tableId);
      toast(msg); reloadAll();
    } catch (e) { toast(e.message, 'err'); }
  };
  const cancel = async () => {
    if (await confirm({ title: 'Hủy đơn hàng', message: `Hủy đơn #${id}? Bàn sẽ được trả lại.`, okText: 'Hủy đơn', danger: true })) setStatus('CANCELLED', 'Đã hủy đơn');
  };

  const err = order.error || items.error || lkError;
  const loading = order.loading || items.loading || !ready;

  return (
    <>
      <PageHeader icon="🧾" title={`Đơn hàng #${id}`} back={{ href: '/admin/orders', label: 'Đơn hàng' }} subtitle={o ? `Tạo lúc ${dt(o.createdAt)}` : undefined}>
        {o && <Badge value={o.status} />}
        {o?.status === 'OPEN' && <button className="adm-btn info" onClick={() => setStatus('SERVING', 'Đã chuyển sang đang phục vụ')}>Bắt đầu phục vụ</button>}
        {open && <button className="adm-btn ok" disabled={lines.length === 0} onClick={() => setDlg('pay')}>💳 Thanh toán</button>}
        {open && <button className="adm-btn danger" onClick={cancel}>Hủy đơn</button>}
      </PageHeader>

      <StateBox loading={loading && !err} error={err} onRetry={reloadAll} />

      {o && ready && !err && (
        <div className="adm-detail">
          <section className="adm-card adm-section">
            <h3>Các món trong đơn <span className="adm-muted">{lines.length} món</span></h3>
            {lines.length === 0 ? <div className="adm-empty">Đơn chưa có món nào.</div> : (
              <div className="adm-table-wrap" style={{ boxShadow: 'none' }}>
                <table className="adm-table">
                  <thead><tr><th>Món</th><th className="adm-right">SL</th><th className="adm-right">Đơn giá</th><th className="adm-right">Thành tiền</th></tr></thead>
                  <tbody>
                    {lines.map((l) => {
                      const m = lk.menu.rows.find((x) => x.id === l.menuItemId);
                      return (
                        <tr key={l.menuItemId}>
                          <td><div className="adm-cell-flex"><Thumb url={m?.imageUrl} />
                            <div><div className="adm-cell-main">{m?.nameVi ?? `Món #${l.menuItemId} (đã xóa)`}</div>{l.note && <div className="adm-cell-sub">📝 {l.note}</div>}</div></div></td>
                          <td className="adm-num">{l.quantity}</td>
                          <td className="adm-num">{vnd(l.unitPrice)}</td>
                          <td className="adm-num"><b>{vnd(l.quantity * Number(l.unitPrice))}</b></td>
                        </tr>
                      );
                    })}
                  </tbody>
                  <tfoot><tr><td colSpan={3}>Tổng cộng</td><td className="adm-num">{vnd(subtotal)}</td></tr></tfoot>
                </table>
              </div>
            )}
          </section>

          <div>
            <section className="adm-card adm-section">
              <h3>Thông tin <button className="adm-btn ghost sm" onClick={() => setDlg('edit')}>Sửa</button></h3>
              <dl className="adm-kv">
                <dt>Khách hàng</dt>
                <dd>{o.customerId ? <Link className="adm-link" href={`/admin/customers/${o.customerId}`}>{nameOf(lk, 'customers', o.customerId)}</Link> : 'Khách vãng lai'}</dd>
                <dt>Bàn</dt><dd>{nameOf(lk, 'tables', o.tableId)}</dd>
                <dt>Nhân viên</dt><dd>{nameOf(lk, 'employees', o.employeeId)}</dd>
                <dt>Trạng thái</dt><dd><Badge value={o.status} /></dd>
              </dl>
            </section>
            <section className="adm-card adm-section">
              <h3>Hóa đơn</h3>
              {invoice ? (
                <>
                  <dl className="adm-kv">
                    <dt>Mã HĐ</dt><dd>#{invoice.id}</dd><dt>Thanh toán</dt><dd>{paymentLabel(invoice.paymentMethod)}</dd>
                    <dt>Giảm giá</dt><dd>{vnd(invoice.discount)}</dd><dt>Tổng cộng</dt><dd><b>{vnd(invoice.total)}</b></dd><dt>Thời gian</dt><dd>{dt(invoice.paidAt)}</dd>
                  </dl>
                  <Link href={`/admin/invoices/${invoice.id}`} className="adm-btn ghost sm" style={{ marginTop: 12 }}>Xem / in hóa đơn</Link>
                </>
              ) : <p className="adm-muted">Chưa có hóa đơn. Hóa đơn được tạo khi bấm “Thanh toán”.</p>}
            </section>
          </div>
        </div>
      )}

      {dlg === 'pay' && <PayDialog order={o} subtotal={subtotal} onClose={() => setDlg(null)} onDone={() => { setDlg(null); reloadAll(); }} />}
      {dlg === 'edit' && <EditInfo order={o} lk={lk} onClose={() => setDlg(null)} onDone={() => { setDlg(null); reloadAll(); }} />}
    </>
  );
}
