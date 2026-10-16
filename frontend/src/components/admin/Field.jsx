'use client';
import ImageUpload from './ImageUpload';

/**
 * 1 ô nhập của form, dựa theo cấu hình field trong lib/admin/resources.jsx
 *  field.type : text | number | email | tel | password | textarea | select | checkbox | datetime-local | image
 */
export default function Field({ field, value, onChange, options = [], disabled, isNew }) {
  const f = field;
  const required = f.required || (f.requiredOnCreate && isNew);
  const set = (v) => onChange(f.name, v);

  let control;
  if (f.type === 'image') {
    control = <ImageUpload value={value} onChange={set} folder={f.folder} disabled={disabled} />;
  } else if (f.type === 'checkbox') {
    control = (
      <label className="adm-check">
        <span className="adm-switch">
          <input type="checkbox" checked={Boolean(value)} disabled={disabled} onChange={(e) => set(e.target.checked)} />
          <span />
        </span>
        <span>{f.checkLabel || 'Bật'}</span>
      </label>
    );
  } else if (f.type === 'textarea') {
    control = <textarea className="adm-input" rows={3} value={value} disabled={disabled} required={required}
      placeholder={f.placeholder} onChange={(e) => set(e.target.value)} />;
  } else if (f.options || f.lookup) {
    control = (
      <select className="adm-input" value={value} disabled={disabled} required={required} onChange={(e) => set(e.target.value)}>
        <option value="">— Chọn —</option>
        {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    );
  } else {
    control = (
      <input className="adm-input" type={f.type || 'text'} value={value} disabled={disabled} required={required}
        min={f.min} step={f.step} placeholder={f.placeholder} autoComplete={f.type === 'password' ? 'new-password' : undefined}
        onChange={(e) => set(e.target.value)} />
    );
  }

  return (
    <div className={`adm-field ${f.full || f.type === 'image' || f.type === 'textarea' ? 'full' : ''}`}>
      <span className="adm-label">{f.label}{required && <span className="adm-req"> *</span>}</span>
      {control}
      {f.hint && <span className="adm-hint">{f.hint}</span>}
    </div>
  );
}
