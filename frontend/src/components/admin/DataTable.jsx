'use client';
import { useRouter } from 'next/navigation';

/**
 * Bảng dữ liệu dùng chung.
 *  columns : [{ key, label, render?(row, ctx), className?, align? }]
 *  rows    : mảng dữ liệu
 *  rowHref : (row) => '/admin/orders/5'  -> bấm vào dòng sẽ chuyển trang
 *  ctx     : dữ liệu phụ truyền cho render (lookup, reload, toast...)
 *  footer  : <tr>...</tr> hiển thị ở cuối bảng
 */
export default function DataTable({ columns, rows, rowHref, ctx = {}, empty = 'Chưa có dữ liệu.', footer, rowKey = (r) => r.id }) {
  const router = useRouter();

  if (!rows.length) return <div className="adm-empty">{empty}</div>;

  const go = (e, row) => {
    if (!rowHref) return;
    // Không điều hướng khi bấm vào nút / liên kết / ô nhập nằm trong dòng
    if (e.target.closest('a, button, input, select, textarea, label')) return;
    router.push(rowHref(row));
  };

  return (
    <div className="adm-table-wrap">
      <table className="adm-table">
        <thead>
          <tr>{columns.map((c) => <th key={c.key} className={c.align === 'right' ? 'adm-right' : ''}>{c.label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={rowKey(r)} className={rowHref ? 'adm-click' : ''} onClick={(e) => go(e, r)}>
              {columns.map((c) => (
                <td key={c.key} className={`${c.className || ''} ${c.align === 'right' ? 'adm-right' : ''}`}>
                  {c.render ? c.render(r, ctx) : r[c.key] ?? '—'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
        {footer && <tfoot>{footer}</tfoot>}
      </table>
    </div>
  );
}
