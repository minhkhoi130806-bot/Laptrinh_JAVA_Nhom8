'use client';
import Link from 'next/link';
import { dt, num, vnd } from '@/lib/api';
import { useApi } from '@/lib/hooks';
import Badge from './Badge';
import PageHeader from './PageHeader';
import StateBox from './StateBox';

export default function Dashboard() {
  const tables = useApi('/tables');
  const reservations = useApi('/reservations');
  const orders = useApi('/orders');
  const invoices = useApi('/invoices');
  const customers = useApi('/customers');
  const menu = useApi('/menu-items');
  const ingredients = useApi('/ingredients');
  const all = [tables, reservations, orders, invoices, customers, menu, ingredients];
  const error = all.find((x) => x.error)?.error;
  const loading = all.some((x) => x.loading && !x.data);

  const body = !error && !loading && (() => {
    const T = tables.data, count = (s) => T.filter((t) => t.status === s).length;
    const revenue = invoices.data.reduce((s, i) => s + Number(i.total || 0), 0);
    const openOrders = orders.data.filter((o) => ['OPEN', 'SERVING'].includes(o.status));
    const low = ingredients.data.filter((i) => i.lowStock);
    const upcoming = reservations.data.filter((r) => ['PENDING', 'CONFIRMED'].includes(r.status))
      .sort((a, b) => a.reservedAt.localeCompare(b.reservedAt)).slice(0, 6);
    const recent = [...orders.data].sort((a, b) => b.id - a.id).slice(0, 6);
    const cname = (id) => customers.data.find((c) => c.id === id)?.fullName ?? `Khách #${id}`;
    const tcode = (id) => T.find((t) => t.id === id)?.tableCode ?? id;
    const stats = [
      ['Doanh thu', vnd(revenue), '/admin/invoices'], ['Đơn đang mở', openOrders.length, '/admin/orders'],
      ['Tổng đơn hàng', orders.data.length, '/admin/orders'], ['Khách hàng', customers.data.length, '/admin/customers'],
      ['Món đang bán', menu.data.filter((m) => m.available).length, '/admin/menu'], ['Nguyên liệu sắp hết', low.length, '/admin/ingredients'],
    ];
    return (
      <>
        <div className="adm-stats">
          {stats.map(([l, v, href]) => <Link href={href} key={l} className="adm-stat"><span className="adm-muted">{l}</span><b>{v}</b></Link>)}
        </div>
        <div className="adm-two">
          <section className="adm-panel">
            <h3>Tình trạng bàn</h3>
            <div>
              {[['AVAILABLE', 'green'], ['RESERVED', 'amber'], ['OCCUPIED', 'red']].map(([s, c]) => (
                <div key={s} className="adm-bar-row"><Badge value={s} />
                  <div className="adm-bar"><i className={c} style={{ width: `${T.length ? (count(s) / T.length) * 100 : 0}%` }} /></div><b>{count(s)}</b></div>
              ))}
            </div>
            <Link href="/admin/tables" className="adm-link">Mở sơ đồ bàn →</Link>
          </section>
          <section className="adm-panel">
            <h3>Đơn hàng gần đây</h3>
            <div>
              {recent.length === 0 && <p className="adm-muted">Chưa có đơn hàng.</p>}
              {recent.map((o) => (
                <Link key={o.id} href={`/admin/orders/${o.id}`} className="adm-li">
                  <div><b>Đơn #{o.id}</b><div className="adm-muted">Bàn {tcode(o.tableId)} · {o.customerId ? cname(o.customerId) : 'Khách vãng lai'} · {vnd(o.total)}</div></div>
                  <Badge value={o.status} />
                </Link>
              ))}
            </div>
            <Link href="/admin/orders" className="adm-link">Tất cả đơn hàng →</Link>
          </section>
        </div>
        <div className="adm-two">
          <section className="adm-panel">
            <h3>Đặt bàn sắp tới</h3>
            <div>
              {upcoming.length === 0 && <p className="adm-muted">Không có lượt đặt nào đang chờ.</p>}
              {upcoming.map((r) => (
                <Link key={r.id} href={`/admin/reservations/${r.id}`} className="adm-li">
                  <div><b>{cname(r.customerId)}</b><div className="adm-muted">{dt(r.reservedAt)} · bàn {tcode(r.tableId)} · {r.guestCount} khách</div></div>
                  <Badge value={r.status} />
                </Link>
              ))}
            </div>
            <Link href="/admin/reservations" className="adm-link">Quản lý đặt bàn →</Link>
          </section>
          <section className="adm-panel">
            <h3>Nguyên liệu sắp hết</h3>
            <div>
              {low.length === 0 && <p className="adm-muted">Tồn kho đều ổn 👍</p>}
              {low.slice(0, 6).map((i) => (
                <Link key={i.id} href={`/admin/ingredients/${i.id}`} className="adm-li">
                  <b>{i.name}</b><span className="adm-muted">còn {num(i.stockQty)} / tối thiểu {num(i.minStock)} {i.unit}</span>
                </Link>
              ))}
            </div>
            <Link href="/admin/receipts/create" className="adm-link">Lập phiếu nhập →</Link>
          </section>
        </div>
      </>
    );
  })();

  return (
    <>
      <PageHeader icon="📊" title="Tổng quan" subtitle="Tình hình nhà hàng hôm nay" />
      <StateBox loading={loading} error={error} onRetry={() => all.forEach((x) => x.reload())} />
      {body}
    </>
  );
}
