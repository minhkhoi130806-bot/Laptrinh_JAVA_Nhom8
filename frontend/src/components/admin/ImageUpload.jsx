'use client';
import { useRef, useState } from 'react';
import { uploadImage } from '@/lib/api';

const MAX_MB = 5;

/**
 * Chọn / kéo thả ảnh -> upload lên backend (POST /api/uploads) -> trả URL qua onChange.
 *  value    : URL ảnh hiện tại ("/uploads/menu/xxx.jpg") hoặc ''
 *  onChange : (url) => void   (url = '' khi gỡ ảnh)
 *  folder   : menu | categories | employees | misc
 */
export default function ImageUpload({ value, onChange, folder = 'misc', disabled = false }) {
  const input = useRef(null);
  const [busy, setBusy] = useState(false);
  const [drag, setDrag] = useState(false);
  const [err, setErr] = useState('');

  const handle = async (file) => {
    if (!file || disabled) return;
    if (!file.type.startsWith('image/')) return setErr('Vui lòng chọn file ảnh (JPG, PNG, GIF, WEBP)');
    if (file.size > MAX_MB * 1024 * 1024) return setErr(`Ảnh tối đa ${MAX_MB}MB`);
    setBusy(true); setErr('');
    try {
      onChange(await uploadImage(file, folder));
    } catch (e) {
      setErr(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="adm-upload">
      <div
        className={`adm-upload-box ${drag ? 'drag' : ''}`}
        onClick={() => !disabled && input.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDrag(true); }}
        onDragLeave={() => setDrag(false)}
        onDrop={(e) => { e.preventDefault(); setDrag(false); handle(e.dataTransfer.files?.[0]); }}
      >
        {value
          // eslint-disable-next-line @next/next/no-img-element
          ? <img src={value} alt="Ảnh xem trước" />
          : <span>📷<br />Bấm hoặc kéo ảnh vào đây</span>}
        {busy && <div className="adm-spin">Đang tải lên…</div>}
      </div>
      <div className="adm-upload-actions">
        <input ref={input} type="file" accept="image/jpeg,image/png,image/gif,image/webp" hidden
          onChange={(e) => { handle(e.target.files?.[0]); e.target.value = ''; }} />
        <button type="button" className="adm-btn ghost sm" disabled={disabled || busy} onClick={() => input.current?.click()}>
          {value ? 'Đổi ảnh' : 'Chọn ảnh'}
        </button>
        {value && <button type="button" className="adm-btn danger sm" disabled={disabled || busy} onClick={() => onChange('')}>Gỡ ảnh</button>}
        <span className="adm-hint adm-muted">JPG / PNG / GIF / WEBP, tối đa {MAX_MB}MB</span>
        {err && <span style={{ color: 'var(--red)', fontSize: '.85rem' }}>{err}</span>}
      </div>
    </div>
  );
}
