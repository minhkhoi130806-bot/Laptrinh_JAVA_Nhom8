'use client';
import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import PageHeader from './PageHeader';
import DataTable from './DataTable';
import StateBox from './StateBox';
import { useConfirm, useToast } from './Feedback';

/**
 * Trang DANH SÁCH dùng chung (admin/<tên>/page.jsx), cấu hình trong lib/admin/resources.jsx
 * Tính năng: tìm kiếm, bộ lọc, bảng hoặc thẻ, bấm vào dòng để mở trang chi tiết,
 * nút Thêm / Sửa / Xóa (xóa mềm -> thùng rác nếu config.softDelete).
 */
export default function ResourceList({ config: c }) {
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const [data, setData] = useState(null);
  const [lk, setLk] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lkError, setLkError] = useState('');

  // Tải dữ liệu trực tiếp trong trang, không thông qua custom hook.
  const reload = async () => {
    try {
      setLoading(true);
      setError('');
      setLkError('');

      const lookups = c.lookups || {};
      const results = await Promise.all([
        api(c.endpoint),
        ...Object.values(lookups).map((item) => api(item.endpoint)),
      ]);
      setData(results[0]);

      const lookupData = {};
      Object.keys(lookups).forEach((key, index) => {
        const rows = results[index + 1];
        const options = rows
          .filter((row) => !row.deleted)
          .map((row) => ({ value: row.id, label: lookups[key].label(row) }));
        lookupData[key] = {
          rows,
          opts: options,
          map: Object.fromEntries(rows.map((row) => [row.id, lookups[key].label(row)])),
        };
      });
      setLk(lookupData);
    } catch (err) {
      setError(err.message || 'Không thể tải dữ liệu.');
      setLkError(err.message || 'Không thể tải dữ liệu tra cứu.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload();
    // Cấu hình được khai báo cố định trong lib/admin/resources.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [c.endpoint]);
  const [q, setQ] = useState('');
  const [fv, setFv] = useState({});

  const canCreate = c.canCreate !== false;
  const canDelete = c.canDelete !== false;
  const filters = c.filters || [];
  const href = (r) => `${c.base}/${r.id}`;

  const rows = useMemo(() => {
    const needle = q.trim().toLowerCase();
    const list = (data || [])
      .filter((r) => filters.every((f) => !fv[f.name] || (f.test ? f.test(r, fv[f.name]) : String(r[f.name]) === fv[f.name])))
      .filter((r) => !needle
        || JSON.stringify(Object.values(r)).toLowerCase().includes(needle)
        || String(c.searchText?.(r, lk) ?? '').toLowerCase().includes(needle));
    return [...list].sort(c.sort || ((a, b) => b.id - a.id));
  }, [data, q, fv, lk]); // eslint-disable-line react-hooks/exhaustive-deps

  const remove = async (r) => {
    const name = c.titleOf ? c.titleOf(r) : `#${r.id}`;
    const ok = await confirm(c.softDelete
      ? { title: 'Chuyển vào thùng rác', message: `Chuyển "${name}" vào thùng rác? Bạn có thể khôi phục lại bất cứ lúc nào.`, okText: 'Chuyển vào thùng rác', danger: true }
      : { title: `Xóa ${c.singular}`, message: `Xóa "${name}"? Thao tác này không thể hoàn tác.`, okText: 'Xóa', danger: true });
    if (!ok) return;
    try {
      await api(`${c.endpoint}/${r.id}`, { method: 'DELETE' });
      toast(c.softDelete ? 'Đã chuyển vào thùng rác' : `Đã xóa ${c.singular}`);
      reload();
    } catch (e) { toast(e.message, 'err'); }
  };

  const ctx = { lk, reload, toast, confirm };
  const buttons = (r) => (
    <div className="adm-row-actions">
      {c.rowActions?.(r, ctx)}
      <Link href={href(r)} className="adm-btn ghost sm">{c.openLabel || 'Sửa'}</Link>
      {canDelete && <button className="adm-btn danger sm" onClick={() => remove(r)}>Xóa</button>}
    </div>
  );

  const columns = c.cards ? [] : [
    ...c.columns,
    { key: '__actions', label: '', render: (r) => buttons(r) },
  ];

  const state = loading || Boolean(error || lkError);

  return (
    <>
      <PageHeader title={c.title} icon={c.icon} subtitle={data ? `${rows.length} / ${data.length} bản ghi` : ' '}>
        {filters.map((f) => (
          <select key={f.name} className="adm-input" value={fv[f.name] || ''} onChange={(e) => setFv({ ...fv, [f.name]: e.target.value })}>
            <option value="">{f.label}</option>
            {(f.options || lk[f.lookup]?.opts || []).map((o) => <option key={o.value} value={String(o.value)}>{o.label}</option>)}
          </select>
        ))}
        <input className="adm-input" placeholder="Tìm kiếm…" value={q} onChange={(e) => setQ(e.target.value)} />
        {c.trash && <Link href={`/admin/trash?type=${c.key}`} className="adm-btn ghost">🗑️ Thùng rác</Link>}
        {canCreate && <Link href={`${c.base}/create`} className="adm-btn">＋ {c.createLabel || `Thêm ${c.singular}`}</Link>}
      </PageHeader>

      <StateBox loading={loading} error={error || lkError} onRetry={reload} />

      {!state && data && c.summary?.(data)}

      {!state && data && (c.cards ? (
        rows.length === 0 ? <div className="adm-empty">Chưa có dữ liệu{q && ' khớp tìm kiếm'}.</div> : (
          <div className="adm-grid">
            {rows.map((r) => (
              <div key={r.id} className="adm-card-item" onClick={() => router.push(href(r))}>
                {c.cards(r, ctx)}
                <div onClick={(e) => e.stopPropagation()}>{buttons(r)}</div>
              </div>
            ))}
          </div>
        )
      ) : (
        <DataTable columns={columns} rows={rows} ctx={ctx} rowHref={href}
          empty={`Chưa có ${c.singular} nào${q ? ' khớp tìm kiếm' : ''}.`} />
      ))}
    </>
  );
}
