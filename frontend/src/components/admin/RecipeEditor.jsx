'use client';
import { useState } from 'react';
import { api } from '@/lib/api';
import { useApi } from '@/lib/hooks';
import { useToast } from './Feedback';

// Công thức món: mỗi dòng = 1 nguyên liệu + định lượng cho 1 phần (dùng để trừ kho sau này)
function Lines({ menuId, initial, ingredients }) {
  const toast = useToast();
  const [lines, setLines] = useState(initial);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState('');
  const unitOf = (id) => ingredients.find((i) => String(i.id) === String(id))?.unit ?? '';
  const upd = (i, patch) => setLines((ls) => ls.map((l, k) => (k === i ? { ...l, ...patch } : l)));

  const save = async () => {
    setSaving(true); setErr('');
    try {
      const body = lines.filter((l) => l.ingredientId !== '').map((l) => ({ ingredientId: Number(l.ingredientId), quantityNeeded: Number(l.quantityNeeded) }));
      await api(`/menu-items/${menuId}/recipe`, { method: 'PUT', body });
      toast('Đã lưu công thức');
    } catch (e) { setErr(e.message); } finally { setSaving(false); }
  };

  return (
    <>
      {lines.length === 0 && <p className="adm-muted" style={{ marginBottom: 10 }}>Món này chưa có công thức.</p>}
      {lines.map((l, i) => (
        <div className="adm-inline-form" key={i}>
          <select className="adm-input" value={l.ingredientId} onChange={(e) => upd(i, { ingredientId: e.target.value })}>
            <option value="">— Chọn nguyên liệu —</option>
            {ingredients.map((g) => <option key={g.id} value={g.id}>{g.name} ({g.unit})</option>)}
          </select>
          <div style={{ position: 'relative' }}>
            <input className="adm-input" type="number" min="0" step="0.001" placeholder="Định lượng" value={l.quantityNeeded}
              onChange={(e) => upd(i, { quantityNeeded: e.target.value })} />
          </div>
          <button type="button" className="adm-btn danger sm" onClick={() => setLines((ls) => ls.filter((_, k) => k !== i))} aria-label="Xóa dòng">✕</button>
          {l.ingredientId !== '' && <span className="adm-muted" style={{ gridColumn: '1 / -1', marginTop: -4 }}>Đơn vị: {unitOf(l.ingredientId)}</span>}
        </div>
      ))}
      {err && <div className="adm-alert">{err}</div>}
      <div className="adm-tools" style={{ marginTop: 10 }}>
        <button type="button" className="adm-btn ghost sm" onClick={() => setLines((ls) => [...ls, { ingredientId: '', quantityNeeded: '' }])}>＋ Thêm nguyên liệu</button>
        <button type="button" className="adm-btn sm" disabled={saving} onClick={save}>{saving ? 'Đang lưu…' : 'Lưu công thức'}</button>
      </div>
    </>
  );
}

export default function RecipeEditor({ menuId }) {
  const rec = useApi(`/menu-items/${menuId}/recipe`);
  const ing = useApi('/ingredients');
  const ready = rec.data && ing.data;
  return (
    <section className="adm-card adm-section">
      <h3>🥘 Công thức (nguyên liệu cho 1 phần)</h3>
      {(rec.error || ing.error) && <div className="adm-alert">{rec.error || ing.error}</div>}
      {!ready && !rec.error && !ing.error && <div className="adm-muted">Đang tải…</div>}
      {ready && (
        <Lines menuId={menuId} ingredients={ing.data}
          initial={rec.data.map((r) => ({ ingredientId: r.ingredientId, quantityNeeded: String(r.quantityNeeded) }))} />
      )}
    </section>
  );
}
