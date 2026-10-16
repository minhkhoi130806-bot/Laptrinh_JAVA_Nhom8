'use client';
import { dt, num, vnd } from '@/lib/api';
import { useApi } from '@/lib/hooks';
import PageHeader from './PageHeader';
import StateBox from './StateBox';

export default function ReceiptView({ id }) {
  const { data: r, error, loading, reload } = useApi(`/receipts/${id}`);
  return (
    <>
      <PageHeader icon="📥" title={`Phiếu nhập #${id}`} back={{ href: '/admin/receipts', label: 'Phiếu nhập kho' }} />
      <StateBox loading={loading} error={error} onRetry={reload} />
      {r && (
        <div className="adm-detail">
          <section className="adm-card adm-section">
            <h3>Nguyên liệu đã nhập</h3>
            <div className="adm-table-wrap" style={{ boxShadow: 'none' }}>
              <table className="adm-table">
                <thead><tr><th>Nguyên liệu</th><th className="adm-right">Số lượng</th><th className="adm-right">Đơn giá</th><th className="adm-right">Thành tiền</th></tr></thead>
                <tbody>
                  {r.details.map((d) => (
                    <tr key={d.ingredientId}>
                      <td className="adm-cell-main">{d.ingredientName}</td><td className="adm-num">{num(d.quantity)} {d.unit}</td>
                      <td className="adm-num">{vnd(d.unitPrice)}</td><td className="adm-num"><b>{vnd(d.lineTotal)}</b></td>
                    </tr>
                  ))}
                </tbody>
                <tfoot><tr><td colSpan={3}>Tổng tiền</td><td className="adm-num">{vnd(r.totalAmount)}</td></tr></tfoot>
              </table>
            </div>
          </section>
          <section className="adm-card adm-section">
            <h3>Thông tin phiếu</h3>
            <dl className="adm-kv">
              <dt>Nhà cung cấp</dt><dd>{r.supplierName}</dd><dt>Người lập</dt><dd>{r.employeeName ?? '—'}</dd>
              <dt>Ngày nhập</dt><dd>{dt(r.receiptDate)}</dd><dt>Ghi chú</dt><dd>{r.note ?? '—'}</dd>
            </dl>
          </section>
        </div>
      )}
    </>
  );
}
