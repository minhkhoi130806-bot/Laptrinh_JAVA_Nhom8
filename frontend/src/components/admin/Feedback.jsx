'use client';
import { createContext, useCallback, useContext, useState } from 'react';
import Modal from './Modal';

// Toast (thông báo nhỏ) + hộp thoại xác nhận dùng chung cho mọi trang admin
//   const toast = useToast();      toast('Đã lưu');  toast('Lỗi...', 'err');
//   const confirm = useConfirm();  if (await confirm({ title, message, danger: true })) { ... }
const Ctx = createContext(null);

export function FeedbackProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const [dlg, setDlg] = useState(null);

  const toast = useCallback((message, type = 'ok') => {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, message, type }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3800);
  }, []);

  const confirm = useCallback((opts) => new Promise((resolve) => setDlg({ ...opts, resolve })), []);
  const close = (value) => { dlg?.resolve(value); setDlg(null); };

  return (
    <Ctx.Provider value={{ toast, confirm }}>
      {children}
      <div className="adm-toasts" aria-live="polite">
        {toasts.map((t) => <div key={t.id} className={`adm-toast ${t.type === 'err' ? 'err' : ''}`}>{t.message}</div>)}
      </div>
      {dlg && (
        <Modal title={dlg.title || 'Xác nhận'} onClose={() => close(false)}>
          <p style={{ marginBottom: 18 }}>{dlg.message}</p>
          <div className="adm-form-f" style={{ marginTop: 0, borderTop: 0, paddingTop: 0 }}>
            <button className="adm-btn ghost" onClick={() => close(false)}>Hủy</button>
            <button className={`adm-btn ${dlg.danger ? 'danger' : ''}`} onClick={() => close(true)} autoFocus>
              {dlg.okText || 'Đồng ý'}
            </button>
          </div>
        </Modal>
      )}
    </Ctx.Provider>
  );
}

export const useToast = () => useContext(Ctx).toast;
export const useConfirm = () => useContext(Ctx).confirm;
