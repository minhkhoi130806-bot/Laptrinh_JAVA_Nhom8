'use client';

import { useEffect, useMemo, useState } from 'react';
import PageHeader from '@/components/admin/PageHeader';

function csvDownload(filename, rows) {
  if (!rows.length) return;
  const columns = [...new Set(rows.flatMap((row) => Object.keys(row)))];
  const cell = (value) => `"${String(value ?? '').replace(/"/g, '""')}"`;
  const content = '\uFEFF' + [columns.map(cell).join(','), ...rows.map((row) => columns.map((key) => cell(row[key])).join(','))].join('\r\n');
  const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8;' }));
  const a = document.createElement('a'); a.href = url; a.download = filename; a.click(); URL.revokeObjectURL(url);
}

export default function LocalManagerPage({ config }) {
  const [items, setItems] = useState([]);
  const [ready, setReady] = useState(false);
  const [form, setForm] = useState({});
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(config.storageKey) || '[]');
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setItems(saved);
    } catch {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setItems([]);
    }
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setReady(true);
  }, [config.storageKey]);

  useEffect(() => {
    if (ready) localStorage.setItem(config.storageKey, JSON.stringify(items));
  }, [items, ready, config.storageKey]);

  const filtered = useMemo(() => items.filter((item) => {
    const matchesSearch = Object.values(item).some((value) => String(value ?? '').toLowerCase().includes(search.toLowerCase()));
    const matchesFilter = filter === 'ALL' || (filter === 'OPEN' ? !item.done : item.done);
    return matchesSearch && matchesFilter;
  }), [items, search, filter]);

  const submit = (event) => {
    event.preventDefault();
    const missing = config.fields.find((field) => field.required && !String(form[field.name] ?? '').trim());
    if (missing) { setNotice(`Vui lòng nhập ${missing.label.toLowerCase()}.`); return; }
    setItems((prev) => [{ id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`, createdAt: new Date().toISOString(), done: false, ...form }, ...prev]);
    setForm({}); setNotice('Đã lưu vào trình duyệt hiện tại.');
  };

  const update = (id, patch) => setItems((prev) => prev.map((item) => item.id === id ? { ...item, ...patch } : item));
  const remove = (id) => { if (window.confirm('Bạn có chắc muốn xóa mục này?')) setItems((prev) => prev.filter((item) => item.id !== id)); };
  const doneCount = items.filter((item) => item.done).length;

  return <>
    <PageHeader icon={config.icon} title={config.title} subtitle={config.subtitle}>
      <button type="button" className="adm-btn ghost sm" onClick={() => csvDownload(`${config.fileName}.csv`, filtered)}>⬇ Xuất CSV</button>
    </PageHeader>
    <div className="adm-stats">
      <div className="adm-stat"><span className="adm-muted">Tổng mục</span><b>{items.length}</b></div>
      <div className="adm-stat"><span className="adm-muted">Đang mở</span><b>{items.length - doneCount}</b></div>
      <div className="adm-stat"><span className="adm-muted">Đã hoàn tất</span><b>{doneCount}</b></div>
      <div className="adm-stat"><span className="adm-muted">Phạm vi lưu</span><b style={{ fontSize: 14 }}>Trình duyệt này</b></div>
    </div>
    <div className="adm-card adm-section" style={{ marginTop: 16 }}>
      <h3 style={{ marginTop: 0 }}>＋ {config.formTitle || 'Thêm mục mới'}</h3>
      <form onSubmit={submit} className="adm-form-grid">
        {config.fields.map((field) => <label key={field.name} className="adm-field">
          <span className="adm-label">{field.label}{field.required ? ' *' : ''}</span>
          {field.type === 'select' ? <select value={form[field.name] || ''} required={field.required} onChange={(e) => setForm((prev) => ({ ...prev, [field.name]: e.target.value }))}>
            <option value="">-- Chọn --</option>{field.options.map((option) => <option key={option.value || option} value={option.value || option}>{option.label || option}</option>)}
          </select> : field.type === 'textarea' ? <textarea rows={3} required={field.required} placeholder={field.placeholder || ''} value={form[field.name] || ''} onChange={(e) => setForm((prev) => ({ ...prev, [field.name]: e.target.value }))} /> : <input type={field.type || 'text'} required={field.required} placeholder={field.placeholder || ''} value={form[field.name] || ''} onChange={(e) => setForm((prev) => ({ ...prev, [field.name]: e.target.value }))} />}
        </label>)}
        <div style={{ display: 'flex', alignItems: 'end', gap: 8 }}><button className="adm-btn" type="submit">Lưu mục</button><button className="adm-btn ghost" type="button" onClick={() => { setForm({}); setNotice(''); }}>Làm mới</button></div>
      </form>
      {notice && <p className="adm-muted" role="status" style={{ marginBottom: 0 }}>{notice}</p>}
      <p className="adm-muted" style={{ fontSize: 12, marginBottom: 0 }}>Lưu ý: module này lưu dữ liệu trong localStorage của trình duyệt, chưa đồng bộ với máy chủ hoặc tài khoản khác.</p>
    </div>
    <div className="adm-card adm-section" style={{ marginTop: 16 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
        <h3 style={{ margin: 0 }}>{config.listTitle || 'Danh sách'}</h3>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          <input aria-label="Tìm kiếm" placeholder="Tìm trong danh sách…" value={search} onChange={(e) => setSearch(e.target.value)} />
          <select aria-label="Lọc trạng thái" value={filter} onChange={(e) => setFilter(e.target.value)}><option value="ALL">Tất cả</option><option value="OPEN">Đang mở</option><option value="DONE">Hoàn tất</option></select>
        </div>
      </div>
      {filtered.length === 0 ? <div className="adm-empty">Chưa có dữ liệu phù hợp. Hãy thêm mục mới ở biểu mẫu phía trên.</div> : <div className="adm-table-wrap"><table className="adm-table"><thead><tr>{config.fields.slice(0, 4).map((field) => <th key={field.name}>{field.label}</th>)}<th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
        {filtered.map((item) => <tr key={item.id}>{config.fields.slice(0, 4).map((field) => <td key={field.name}>{String(item[field.name] ?? '—')}</td>)}<td><span className={`adm-badge ${item.done ? 'green' : 'amber'}`}>{item.done ? 'Hoàn tất' : 'Đang mở'}</span></td><td><div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}><button className="adm-btn ghost sm" onClick={() => update(item.id, { done: !item.done })}>{item.done ? 'Mở lại' : config.doneLabel || 'Hoàn tất'}</button><button className="adm-btn ghost sm" style={{ color: '#dc2626' }} onClick={() => remove(item.id)}>Xóa</button></div></td></tr>)}
      </tbody></table></div>}
    </div>
  </>;
}
