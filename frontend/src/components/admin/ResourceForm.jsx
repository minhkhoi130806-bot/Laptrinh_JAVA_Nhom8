'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, toInputDateTime } from '@/lib/api';
import { useApi, useLookups } from '@/lib/hooks';
import PageHeader from './PageHeader';
import StateBox from './StateBox';
import Field from './Field';
import { useConfirm, useToast } from './Feedback';

const initialValues = (fields) =>
  Object.fromEntries(fields.map((f) => [f.name, f.default === 'now' ? toInputDateTime() : f.default ?? (f.type === 'checkbox' ? false : '')]));

const fromRecord = (fields, r) =>
  Object.fromEntries(fields.map((f) => {
    const v = r[f.name];
    if (f.type === 'checkbox') return [f.name, Boolean(v)];
    if (f.type === 'datetime-local') return [f.name, v ? String(v).slice(0, 16) : ''];
    return [f.name, v ?? ''];
  }));

function toBody(fields, form, isNew) {
  const body = {};
  fields.forEach((f) => {
    if ((f.hideOnCreate && isNew) || (f.hideOnEdit && !isNew)) return;
    const v = form[f.name];
    if (f.type === 'checkbox') body[f.name] = Boolean(v);
    else if (v === '' || v == null) body[f.name] = null;
    else if (f.type === 'number' || f.lookup || f.cast === 'number') body[f.name] = Number(v);
    else body[f.name] = typeof v === 'string' && f.type !== 'password' ? v.trim() : v;
  });
  return body;
}

// Phần form thật sự; khởi tạo giá trị 1 lần (key = id bản ghi nên đổi trang là reset)
function FormBody({ c, record, lk, isNew, onSaved }) {
  const router = useRouter();
  const toast = useToast();
  const [form, setForm] = useState(() => (isNew ? initialValues(c.fields) : fromRecord(c.fields, record)));
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');
  const locked = !isNew && c.locked?.(record);

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true); setErr('');
    try {
      const saved = await api(isNew ? c.endpoint : `${c.endpoint}/${record.id}`, {
        method: isNew ? 'POST' : 'PUT',
        body: toBody(c.fields, form, isNew),
      });
      toast(isNew ? `Đã thêm ${c.singular}` : 'Đã lưu thay đổi');
      if (isNew) router.push(c.afterCreate ? c.afterCreate(saved) : c.base);
      else onSaved();
    } catch (ex) { setErr(ex.message); } finally { setSaving(false); }
  };

  const visible = c.fields.filter((f) => !((f.hideOnCreate && isNew) || (f.hideOnEdit && !isNew)));

  return (
    <form onSubmit={submit} className="adm-card adm-form-card adm-section">
      {locked && <div className="adm-alert warn">{c.lockedMessage || 'Bản ghi này đã khóa, không thể sửa.'}</div>}
      <div className="adm-form-grid">
        {visible.map((f) => (
          <Field key={f.name} field={f} isNew={isNew} value={form[f.name]}
            disabled={locked || (f.lockOnEdit && !isNew)}
            options={f.options || lk[f.lookup]?.opts || []}
            onChange={(name, v) => setForm((s) => ({ ...s, [name]: v }))} />
        ))}
      </div>
      {err && <div className="adm-alert">{err}</div>}
      <div className="adm-form-f">
        <button type="button" className="adm-btn ghost" onClick={() => router.push(c.base)}>
          {locked ? 'Quay lại' : 'Hủy'}
        </button>
        {!locked && <button className="adm-btn" disabled={saving}>{saving ? 'Đang lưu…' : isNew ? `Thêm ${c.singular}` : 'Lưu thay đổi'}</button>}
      </div>
    </form>
  );
}

/**
 * Trang TẠO MỚI (admin/<tên>/create/page.jsx)  -> <ResourceForm config={...} />
 * Trang CHI TIẾT + SỬA (admin/<tên>/[id]/page.jsx) -> <ResourceForm config={...} id={id} extra={(record, reload) => ...} />
 *   extra : vùng hiển thị thêm bên dưới form (công thức món, đơn hàng của khách...)
 */
export default function ResourceForm({ config: c, id, extra }) {
  const isNew = id == null;
  const router = useRouter();
  const toast = useToast();
  const confirm = useConfirm();
  const { data: record, error, loading, reload } = useApi(isNew ? null : `${c.endpoint}/${id}`);
  const { lk, ready, error: lkError } = useLookups(c.lookups);

  const remove = async () => {
    const name = c.titleOf(record);
    const ok = await confirm(c.softDelete
      ? { title: 'Chuyển vào thùng rác', message: `Chuyển "${name}" vào thùng rác?`, okText: 'Chuyển vào thùng rác', danger: true }
      : { title: `Xóa ${c.singular}`, message: `Xóa "${name}"? Thao tác này không thể hoàn tác.`, okText: 'Xóa', danger: true });
    if (!ok) return;
    try {
      await api(`${c.endpoint}/${id}`, { method: 'DELETE' });
      toast(c.softDelete ? 'Đã chuyển vào thùng rác' : `Đã xóa ${c.singular}`);
      router.push(c.base);
    } catch (e) { toast(e.message, 'err'); }
  };

  const ready2 = ready && (isNew || record);
  const title = isNew ? `Thêm ${c.singular}` : record ? c.titleOf(record) : c.title;

  return (
    <>
      <PageHeader icon={c.icon} title={title} back={{ href: c.base, label: c.title }}
        subtitle={!isNew && record ? `Mã #${record.id}${record.deleted ? ' · đang ở thùng rác' : ''}` : undefined}>
        {!isNew && record && c.detailActions?.(record, { reload, toast })}
        {!isNew && record && c.canDelete !== false && !record.deleted && (
          <button className="adm-btn danger" onClick={remove}>🗑️ {c.softDelete ? 'Chuyển vào thùng rác' : 'Xóa'}</button>
        )}
      </PageHeader>

      <StateBox loading={loading || (!ready && !lkError)} error={error || lkError} onRetry={reload} />

      {ready2 && !error && (
        <>
          <FormBody key={record?.id ?? 'new'} c={c} record={record} lk={lk} isNew={isNew} onSaved={reload} />
          {!isNew && extra?.(record, reload)}
        </>
      )}
    </>
  );
}
