// Gọi API Spring Boot qua proxy /api (xem next.config.mjs). Dùng chung cho admin và shop.
// Tự động gắn kèm Authorization: Bearer <token> từ localStorage (Nhiệm vụ Ngô Minh Khôi - Tuần 3)
export async function api(path, { method = 'GET', body, signal } = {}) {
  const isForm = typeof FormData !== 'undefined' && body instanceof FormData;
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;

  const headers = {};
  if (body && !isForm) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch('/api' + path, {
    method,
    headers: Object.keys(headers).length > 0 ? headers : undefined,
    body: body ? (isForm ? body : JSON.stringify(body)) : undefined,
    signal,
  });

  if (res.status === 401 && !path.startsWith('/auth/login') && typeof window !== 'undefined') {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    window.dispatchEvent(new Event('auth:expired'));
  }

  if (!res.ok) {
    let msg = `Lỗi ${res.status}`;
    try { msg = (await res.json()).message || msg; } catch {}
    throw new Error(msg);
  }
  return res.status === 204 ? null : res.json();
}

// Upload 1 ảnh lên backend, trả về URL dạng "/uploads/menu/xxxx.jpg" để lưu vào cột image_url
// folder: menu | categories | employees | misc
export async function uploadImage(file, folder = 'misc') {
  const fd = new FormData();
  fd.append('file', file);
  fd.append('folder', folder);
  const { url } = await api('/uploads', { method: 'POST', body: fd });
  return url;
}

// ----- Định dạng hiển thị -----
export const vnd = (n) => (n == null || n === '' ? '—' : Number(n).toLocaleString('vi-VN') + ' ₫');
export const num = (n) => (n == null || n === '' ? '—' : Number(n).toLocaleString('vi-VN', { maximumFractionDigits: 3 }));
export const dt = (s) => (s ? new Date(s).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : '—');

// Giá trị cho <input type="datetime-local"> từ chuỗi ISO / Date
export const toInputDateTime = (v) => {
  const d = v ? new Date(v) : new Date();
  const pad = (x) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};
