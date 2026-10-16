'use client';
import { useCallback, useEffect, useState } from 'react';
import { api } from './api';

// Tải dữ liệu từ 1 endpoint:  const { data, error, loading, reload } = useApi('/orders')
// Truyền path = null để tạm không tải.
export function useApi(path) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(Boolean(path));

  const load = useCallback(async () => {
    if (!path) return;
    try {
      setLoading(true);
      setData(await api(path));
      setError('');
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [path]);

  useEffect(() => { load(); }, [load]);
  return { data, error, loading, reload: load, setData };
}

// Tải nhiều danh sách tra cứu cùng lúc (để đổ vào <select> và hiển thị tên thay cho id).
//   defs = { customers: { endpoint: '/customers?scope=ALL', label: (c) => c.fullName } }
//   -> lk.customers = { rows, opts: [{value,label}], map: { [id]: label } }
// defs phải là hằng số (khai báo ngoài component) để không tải lại liên tục.
export function useLookups(defs) {
  const keys = defs ? Object.keys(defs) : [];
  const [lk, setLk] = useState({});
  const [ready, setReady] = useState(keys.length === 0);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (!defs || Object.keys(defs).length === 0) return;
    try {
      const results = await Promise.all(Object.values(defs).map((d) => api(d.endpoint)));
      const out = {};
      Object.keys(defs).forEach((k, i) => {
        const rows = results[i];
        out[k] = {
          rows,
          // chỉ cho chọn các bản ghi chưa vào thùng rác
          opts: rows.filter((r) => !r.deleted).map((r) => ({ value: r.id, label: defs[k].label(r) })),
          map: Object.fromEntries(rows.map((r) => [r.id, defs[k].label(r)])),
        };
      });
      setLk(out);
      setError('');
      setReady(true);
    } catch (e) {
      setError(e.message);
    }
  }, [defs]);

  useEffect(() => { load(); }, [load]);
  return { lk, ready, error, reload: load };
}
