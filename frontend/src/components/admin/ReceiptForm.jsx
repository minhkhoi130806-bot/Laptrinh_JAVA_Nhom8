'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, toInputDateTime, vnd } from '@/lib/api';
import { useLookups } from '@/lib/hooks';
import { LK } from '@/lib/admin/resources';
import PageHeader from './PageHeader';
import StateBox from './StateBox';
import { useToast } from './Feedback';

const DEFS = { suppliers: LK.suppliers, employees: LK.employees, ingredients: LK.ingredients };
const emptyLine = () => ({ ingredientId: '', quantity: '', unitPrice: '' });

// Lập phiếu nhập kho: lưu phiếu xong backend tự CỘNG số lượng vào tồn kho
export default function ReceiptForm() {
  const router = useRouter();
  const toast = useToast();
  const { lk, ready, error, reload } = useLookups(DEFS);
  const [head, setHead] = useState({ supplierId: '', employeeId: '', receiptDate: toInputDateTime(), note: '' });
  const [lines, setLines] = useState([emptyLine()]);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');

  const upd = (i, patch) => setLines((ls) => ls.map((l, k) => (k === i ? { ...l, ...patch } : l)));
  const total = lines.reduce((s, l) => s + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0), 0);
  const unitOf = (id) => lk.ingredients.rows.find((x) => String(x.id) === String(id))?.unit ?? '';
  const lowStockList = (lk.ingredients?.rows || []).filter((i) => i.lowStock);

  const submit = async (e) => {
    e.preventDefault(); setErr('');
    const details = lines.filter((l) => l.ingredientId !== '');
    if (details.length === 0) return setErr('Hãy chọn ít nhất 1 nguyên liệu');
    if (details.some((l) => !l.quantity || Number(l.quantity) <= 0)) {
      return setErr('Số lượng nhập của tất cả các dòng phải lớn hơn 0');
    }
    if (details.some((l) => l.unitPrice === '' || Number(l.unitPrice) < 0)) {
      return setErr('Đơn giá nhập của các dòng không được để trống hoặc âm');
    }
    setSaving(true);
    try {
      const saved = await api('/receipts', {
        method: 'POST',
        body: {
          supplierId: Number(head.supplierId), employeeId: head.employeeId ? Number(head.employeeId) : null,
          receiptDate: head.receiptDate || null, note: head.note || null,
          details: details.map((l) => ({ ingredientId: Number(l.ingredientId), quantity: Number(l.quantity), unitPrice: Number(l.unitPrice) })),
        },
      });
      toast('Đã lập phiếu nhập và cộng tồn kho');
      router.push(`/admin/receipts/${saved.id}`);
    } catch (ex) { setErr(ex.message); setSaving(false); }
  };

  return (
    <>
      <PageHeader icon="📥" title="Lập phiếu nhập" back={{ href: '/admin/receipts', label: 'Phiếu nhập kho' }} />
      <StateBox loading={!ready && !error} error={error} onRetry={reload} />
      {ready && (
        <form onSubmit={submit} className="adm-card adm-form-card adm-section">
          <div className="adm-form-grid">
            <label className="adm-field">
              <span className="adm-label">Nhà cung cấp <span className="adm-req">*</span></span>
              <select className="adm-input" required value={head.supplierId} onChange={(e) => setHead({ ...head, supplierId: e.target.value })}>
                <option value="">— Chọn —</option>
                {lk.suppliers.opts.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </label>

            <label className="adm-field">
              <span className="adm-label">Người lập phiếu (Nhân viên)</span>
              <select className="adm-input" value={head.employeeId} onChange={(e) => setHead({ ...head, employeeId: e.target.value })}>
                <option value="">— Không chọn —</option>
                {lk.employees.opts.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </label>

            <label className="adm-field">
              <span className="adm-label">Ngày nhập</span>
              <input className="adm-input" type="datetime-local" value={head.receiptDate} onChange={(e) => setHead({ ...head, receiptDate: e.target.value })} />
            </label>

            <label className="adm-field">
              <span className="adm-label">Ghi chú</span>
              <input
                className="adm-input"
                placeholder="Ghi chú thêm hoặc số hóa đơn..."
                value={head.note}
                onChange={(e) => setHead({ ...head, note: e.target.value })}
              />
            </label>
          </div>

          {lowStockList.length > 0 && (
            <div className="adm-alert danger" style={{ margin: '16px 0 8px' }}>
              <div style={{ fontWeight: 600, marginBottom: 6 }}>
                ⚠️ Có {lowStockList.length} nguyên liệu dưới mức tồn tối thiểu cần nhập:
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {lowStockList.map((ing) => {
                  const inLines = lines.some((l) => String(l.ingredientId) === String(ing.id));
                  const shortfall = Math.max(0, (Number(ing.minStock) || 0) - (Number(ing.stockQty) || 0));
                  return (
                    <span key={ing.id} style={{ display: 'inline-flex', alignItems: 'center', background: '#fff', padding: '4px 8px', borderRadius: 6, fontSize: '0.85rem', border: '1px solid #fca5a5' }}>
                      <span><strong>{ing.name}</strong> (còn {ing.stockQty} / tối thiểu {ing.minStock} {ing.unit})</span>
                      {!inLines && (
                        <button type="button" className="adm-btn sm ghost" style={{ marginLeft: 6, padding: '2px 6px', height: 'auto', fontSize: '0.75rem' }}
                          onClick={() => {
                            setLines((ls) => {
                              if (ls.length === 1 && !ls[0].ingredientId) {
                                return [{ ingredientId: String(ing.id), quantity: shortfall > 0 ? String(shortfall) : '', unitPrice: '' }];
                              }
                              return [...ls, { ingredientId: String(ing.id), quantity: shortfall > 0 ? String(shortfall) : '', unitPrice: '' }];
                            });
                          }}>
                          ＋ Thêm vào phiếu
                        </button>
                      )}
                    </span>
                  );
                })}
              </div>
            </div>
          )}

          <h3 style={{ margin: '22px 0 10px' }}>Nguyên liệu nhập</h3>
          {lines.map((l, i) => (
            <div className="adm-inline-form recv" key={i}>
              <select className="adm-input" value={l.ingredientId} onChange={(e) => upd(i, { ingredientId: e.target.value })}>
                <option value="">— Chọn nguyên liệu —</option>
                {lk.ingredients.opts
                  .filter((o) => String(o.value) === String(l.ingredientId) || !lines.some((x) => String(x.ingredientId) === String(o.value)))
                  .map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <input className="adm-input" type="number" min="0" step="0.001" placeholder={`SL${unitOf(l.ingredientId) ? ` (${unitOf(l.ingredientId)})` : ''}`}
                required={l.ingredientId !== ''} value={l.quantity} onChange={(e) => upd(i, { quantity: e.target.value })} />
              <input className="adm-input" type="number" min="0" step="100" placeholder="Đơn giá (₫)"
                required={l.ingredientId !== ''} value={l.unitPrice} onChange={(e) => upd(i, { unitPrice: e.target.value })} />
              <button type="button" className="adm-btn danger sm" disabled={lines.length === 1} onClick={() => setLines((ls) => ls.filter((_, k) => k !== i))} aria-label="Xóa dòng">✕</button>
            </div>
          ))}
          <button type="button" className="adm-btn ghost sm" onClick={() => setLines((ls) => [...ls, emptyLine()])}>＋ Thêm dòng</button>

          <div className="adm-sum"><div className="adm-grand"><span>Tổng tiền phiếu</span><span>{vnd(total)}</span></div></div>

          {err && <div className="adm-alert">{err}</div>}
          <div className="adm-form-f">
            <button type="button" className="adm-btn ghost" onClick={() => router.push('/admin/receipts')}>Hủy</button>
            <button className="adm-btn" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu phiếu nhập'}</button>
          </div>
        </form>
      )}
    </>
  );
}
