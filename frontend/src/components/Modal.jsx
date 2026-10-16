'use client';
export default function Modal({ title, onClose, children }) {
  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal-h"><h3>{title}</h3><button className="x" onClick={onClose}>✕</button></div>
        {children}
      </div>
    </div>
  );
}
