'use client';
import { useEffect } from 'react';

export default function Modal({ title, onClose, children, wide = false }) {
  // Nhấn Esc để đóng
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="adm-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`adm-modal${wide ? ' wide' : ''}`} role="dialog" aria-modal="true">
        <div className="adm-modal-h">
          <h3>{title}</h3>
          <button type="button" className="adm-x" onClick={onClose} aria-label="Đóng">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}
