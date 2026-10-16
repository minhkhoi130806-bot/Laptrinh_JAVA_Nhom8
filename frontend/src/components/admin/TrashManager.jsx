'use client';
import { useState } from 'react';
import { api, dt } from '@/lib/api';
import { useApi } from '@/lib/hooks';
import { TRASH_TYPES } from '@/lib/admin/resources';
import DataTable from './DataTable';
import PageHeader from './PageHeader';
import StateBox from './StateBox';
import { useConfirm, useToast } from './Feedback';

const SUB = {
  menu: (r) => `${r.categoryName} · ${Number(r.price).toLocaleString('vi-VN')} ₫`,
  customers: (r) => `${r.phone}${r.email ? ' · ' + r.email : ''}`,
  employees: (r) => `${r.username} · ${r.phone}`,
  tables: (r) => `${r.capacity} chỗ · ${r.zone}`,
};

function TrashList({ c }) {
  const toast = useToast();
  const confirm = useConfirm();
  const { data, error, loading, reload } = useApi(`${c.endpoint}?scope=TRASH`);

  const restore = async (r) => {
    try { await api(`${c.endpoint}/${r.id}/restore`, { method: 'POST' }); toast(`Đã khôi phục "${c.titleOf(r)}"`); reload(); }
    catch (e) { toast(e.message, 'err'); }
  };
  const purge = async (r) => {
    const ok = await confirm({
      title: 'Xóa vĩnh viễn', okText: 'Xóa vĩnh viễn', danger: true,
      message: `Xóa vĩnh viễn "${c.titleOf(r)}"? Không thể khôi phục. Dữ liệu cũ (đơn hàng, đặt bàn…) đang tham chiếu tới mục này có thể không còn hiển thị được tên.`,
    });
    if (!ok) return;
    try { await api(`${c.endpoint}/${r.id}/permanent`, { method: 'DELETE' }); toast('Đã xóa vĩnh viễn'); reload(); }
    catch (e) { toast(e.message, 'err'); }
  };

  return (
    <>
      <StateBox loading={loading} error={error} onRetry={reload} />
      {data && (
        <DataTable rows={data} empty={`Thùng rác trống — không có ${c.singular} nào bị xóa.`}
          columns={[
            { key: 'name', label: c.title, render: (r) => <div><div className="adm-cell-main">{c.titleOf(r)}</div><div className="adm-cell-sub">{SUB[c.key]?.(r)}</div></div> },
            { key: 'deletedAt', label: 'Xóa lúc', render: (r) => dt(r.deletedAt) },
            {
              key: 'act', label: '', render: (r) => (
                <div className="adm-row-actions">
                  <button className="adm-btn ok sm" onClick={() => restore(r)}>↩ Khôi phục</button>
                  <button className="adm-btn danger sm" onClick={() => purge(r)}>Xóa vĩnh viễn</button>
                </div>
              ),
            },
          ]} />
      )}
    </>
  );
}

export default function TrashManager({ initialType }) {
  const [type, setType] = useState(TRASH_TYPES.some((t) => t.key === initialType) ? initialType : TRASH_TYPES[0].key);
  const current = TRASH_TYPES.find((t) => t.key === type);
  return (
    <>
      <PageHeader icon="🗑️" title="Thùng rác" subtitle="Các mục đã xóa vẫn giữ lại để lịch sử đơn hàng không bị mất — khôi phục hoặc xóa vĩnh viễn tại đây." />
      <div className="adm-tabs">
        {TRASH_TYPES.map((t) => <button key={t.key} className={t.key === type ? 'on' : ''} onClick={() => setType(t.key)}>{t.icon} {t.title}</button>)}
      </div>
      <TrashList key={type} c={current} />
    </>
  );
}
