'use client';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';
import Modal from './Modal';

/**
 * Trang CRUD dùng chung, cấu hình bằng props:
 *  endpoint  : '/customers'
 *  columns   : [{ key, label, render?(row, maps) }]
 *  fields    : [{ name, label, type, options?, lookup?, required?, lockOnEdit?, hideOnEdit? }]
 *  lookups   : { customers: { endpoint:'/customers', label: r => r.fullName } }
 *  idOf      : row => khóa (mặc định row.id; order-items dùng "orderId/menuItemId")
 *  actions   : (row, reload) => JSX  (nút thao tác thêm, vd: đổi trạng thái)
 *  cards     : (row, ctx) => JSX     (hiển thị dạng thẻ thay vì bảng)
 */
export default function Crud({ title, icon, endpoint, columns, fields, lookups = {}, idOf = (r) => r.id, actions, cards, filters }) {
  const [rows, setRows] = useState([]);
  const [lk, setLk] = useState({});
  const [q, setQ] = useState('');
  const [state, setState] = useState('loading');
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(null); // null | {} (mới) | row
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);
  const [formErr, setFormErr] = useState('');

  const load = async () => {
    try {
      setState('loading');
      const [data, ...ls] = await Promise.all([
        api(endpoint),
        ...Object.values(lookups).map((l) => api(l.endpoint)),
      ]);
      const out = {};
      Object.keys(lookups).forEach((k, i) => {
        const opts = ls[i].map((r) => ({ value: r.id, label: lookups[k].label(r) }));
        out[k] = { opts, map: Object.fromEntries(opts.map((o) => [o.value, o.label])) };
      });
      setRows(data); setLk(out); setState('ok'); setError('');
    } catch (e) { setError(e.message); setState('error'); }
  };
  useEffect(() => { load(); // eslint-disable-line react-hooks/exhaustive-deps
  }, [endpoint]);

  const maps = useMemo(() => Object.fromEntries(Object.entries(lk).map(([k, v]) => [k, v.map])), [lk]);
  const shown = rows.filter((r) => !q || JSON.stringify(Object.values(r)).toLowerCase().includes(q.toLowerCase())
    || columns.some((c) => c.render && String(c.search?.(r, maps) ?? '').toLowerCase().includes(q.toLowerCase())));

  const isNew = editing && !editing.__row;
  const openNew = () => { setForm(Object.fromEntries(fields.map((f) => [f.name, f.default ?? '']))); setFormErr(''); setEditing({}); };
  const openEdit = (row) => {
    setForm(Object.fromEntries(fields.map((f) => [f.name, f.type === 'datetime-local' ? (row[f.name] || '').slice(0, 16) : row[f.name] ?? ''])));
    setFormErr(''); setEditing({ __row: row });
  };

  const save = async (e) => {
    e.preventDefault(); setSaving(true); setFormErr('');
    const body = {};
    fields.forEach((f) => {
      const v = form[f.name];
      body[f.name] = v === '' || v == null ? null : f.type === 'number' || f.cast === 'number' ? Number(v) : v;
    });
    try {
      await api(isNew ? endpoint : `${endpoint}/${idOf(editing.__row)}`, { method: isNew ? 'POST' : 'PUT', body });
      setEditing(null); load();
    } catch (err) { setFormErr(err.message); } finally { setSaving(false); }
  };

  const remove = async (row) => {
    if (!confirm('Bạn chắc chắn muốn xóa mục này?')) return;
    try { await api(`${endpoint}/${idOf(row)}`, { method: 'DELETE' }); load(); } catch (e) { alert(e.message); }
  };

  const ctx = { maps, reload: load, edit: openEdit, remove };
  const rowButtons = (r) => (
    <div className="row-actions">
      {actions?.(r, load)}
      <button className="btn ghost sm" onClick={() => openEdit(r)}>Sửa</button>
      <button className="btn danger sm" onClick={() => remove(r)}>Xóa</button>
    </div>
  );

  return (
    <>
      <header className="page-h">
        <div><h1>{icon} {title}</h1><p className="muted">{rows.length} bản ghi</p></div>
        <div className="tools">
          {filters}
          <input className="input" placeholder="Tìm kiếm…" value={q} onChange={(e) => setQ(e.target.value)} />
          <button className="btn" onClick={openNew}>＋ Thêm mới</button>
        </div>
      </header>

      {state === 'error' && (
        <div className="alert">Không kết nối được backend: {error}. Hãy chạy Spring Boot ở cổng 8080 và kiểm tra MySQL.
          <button className="btn ghost sm" onClick={load}>Thử lại</button></div>
      )}
      {state === 'loading' && <div className="muted pad">Đang tải…</div>}
      {state === 'ok' && shown.length === 0 && <div className="empty">Chưa có dữ liệu{q && ' khớp tìm kiếm'}.</div>}

      {state === 'ok' && shown.length > 0 && (cards ? (
        <div className="grid">{shown.map((r) => <div key={idOf(r)} className="card-item">{cards(r, ctx)}{rowButtons(r)}</div>)}</div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead><tr>{columns.map((c) => <th key={c.key}>{c.label}</th>)}<th /></tr></thead>
            <tbody>
              {shown.map((r) => (
                <tr key={idOf(r)}>
                  {columns.map((c) => <td key={c.key}>{c.render ? c.render(r, maps) : r[c.key] ?? '—'}</td>)}
                  <td>{rowButtons(r)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}

      {editing && (
        <Modal title={isNew ? `Thêm ${title.toLowerCase()}` : `Sửa ${title.toLowerCase()}`} onClose={() => setEditing(null)}>
          <form onSubmit={save} className="form">
            {fields.filter((f) => !(f.hideOnEdit && !isNew)).map((f) => (
              <label key={f.name}>
                <span>{f.label}{f.required && ' *'}</span>
                {f.options || f.lookup ? (
                  <select className="input" required={f.required} disabled={f.lockOnEdit && !isNew}
                    value={form[f.name]} onChange={(e) => setForm({ ...form, [f.name]: e.target.value })}>
                    <option value="">— Chọn —</option>
                    {(f.options || lk[f.lookup]?.opts || []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                ) : (
                  <input className="input" type={f.type || 'text'} required={f.required} step={f.step} min={f.min}
                    disabled={f.lockOnEdit && !isNew} value={form[f.name]}
                    onChange={(e) => setForm({ ...form, [f.name]: e.target.value })} />
                )}
              </label>
            ))}
            {formErr && <div className="alert">{formErr}</div>}
            <div className="form-f">
              <button type="button" className="btn ghost" onClick={() => setEditing(null)}>Hủy</button>
              <button className="btn" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu'}</button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
