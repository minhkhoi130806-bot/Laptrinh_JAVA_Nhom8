'use client';
import Link from 'next/link';
import { dt, vnd } from '@/lib/api';
import { useApi, useLookups } from '@/lib/hooks';
import { LK, nameOf } from '@/lib/admin/resources';
import { paymentLabel } from '@/lib/constants';
import PageHeader from './PageHeader';
import StateBox from './StateBox';

const DEFS = { customers: LK.customers, tables: LK.tables, menu: LK.menu };

// Hóa đơn chỉ xem + in (không sửa/xóa để sổ sách luôn đúng)
export default function InvoiceView({ id }) {
  const inv = useApi(`/invoices/${id}`);
  const orderId = inv.data?.orderId;
  const order = useApi(orderId ? `/orders/${orderId}` : null);
  const items = useApi(orderId ? `/order-items?orderId=${orderId}` : null);
  const { lk, ready } = useLookups(DEFS);
  const i = inv.data;

  return (
    <>
      <PageHeader icon="💳" title={`Hóa đơn #${id}`} back={{ href: '/admin/invoices', label: 'Hóa đơn' }}>
        {i && <button className="adm-btn" onClick={() => window.print()}>🖨️ In hóa đơn</button>}
      </PageHeader>
      <StateBox loading={inv.loading} error={inv.error} onRetry={inv.reload} />
      {i && ready && (
        <div className="adm-card adm-invoice">
          <div className="adm-invoice-h">
            <div><h2>🍜 Nhà hàng Nhóm 8</h2><p className="adm-muted">HÓA ĐƠN THANH TOÁN</p></div>
            <div className="adm-right"><b>#{i.id}</b><p className="adm-muted">{dt(i.paidAt)}</p></div>
          </div>
          <dl className="adm-kv" style={{ marginBottom: 16 }}>
            <dt>Đơn hàng</dt><dd><Link className="adm-link" href={`/admin/orders/${i.orderId}`}>Đơn #{i.orderId}</Link></dd>
            <dt>Bàn</dt><dd>{nameOf(lk, 'tables', order.data?.tableId)}</dd>
            <dt>Khách hàng</dt><dd>{order.data?.customerId ? nameOf(lk, 'customers', order.data.customerId) : 'Khách vãng lai'}</dd>
            <dt>Thanh toán</dt><dd>{paymentLabel(i.paymentMethod)}</dd>
          </dl>
          <table className="adm-table">
            <thead><tr><th>Món</th><th className="adm-right">SL</th><th className="adm-right">Đơn giá</th><th className="adm-right">Thành tiền</th></tr></thead>
            <tbody>
              {(items.data || []).map((l) => (
                <tr key={l.menuItemId}>
                  <td>{lk.menu.map[l.menuItemId] ?? `Món #${l.menuItemId}`}</td><td className="adm-num">{l.quantity}</td>
                  <td className="adm-num">{vnd(l.unitPrice)}</td><td className="adm-num">{vnd(l.quantity * Number(l.unitPrice))}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="adm-sum">
            <div><span>Tạm tính</span><span>{vnd(i.subtotal)}</span></div>
            <div><span>Giảm giá</span><span>− {vnd(i.discount)}</span></div>
            <div className="adm-grand"><span>Tổng cộng</span><span>{vnd(i.total)}</span></div>
          </div>
          <p className="adm-muted adm-right" style={{ marginTop: 18 }}>Cảm ơn quý khách – hẹn gặp lại!</p>
        </div>
      )}
    </>
  );
}
