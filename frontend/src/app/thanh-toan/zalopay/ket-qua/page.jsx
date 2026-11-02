'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import { api, vnd } from '@/lib/api';

// ZaloPay chuyển khách về đây kèm ?appid=...&apptransid=<mã giao dịch>&pmcid=...&status=1...
// Các tham số trên URL KHÔNG được tin: trang chỉ lấy apptransid rồi hỏi backend,
// backend truy vấn trực tiếp ZaloPay server qua API v2.
const MAX_TRIES = 8;
const messages = {
  SUCCESS: ['Thanh toán thành công', 'ZaloPay đã xác nhận giao dịch. Nhà hàng sẽ chuẩn bị đơn của bạn.'],
  FAILED: ['Thanh toán chưa thành công', 'Giao dịch bị hủy hoặc thất bại, bạn chưa bị trừ tiền. Có thể thử lại từ trang theo dõi đơn hàng.'],
  EXPIRED: ['Giao dịch đã hết hạn', 'Bạn có thể tạo lại giao dịch từ trang theo dõi đơn hàng.'],
  PENDING: ['Đang chờ ZaloPay xác nhận', 'Hệ thống đang kiểm tra kết quả thanh toán. Vui lòng bấm kiểm tra lại sau ít phút, đừng thanh toán lần nữa.'],
};

export default function ZalopayResultPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [appTransId, setAppTransId] = useState('');
  const [phone, setPhone] = useState('');

  async function check(id, attempt = 1) {
    setLoading(true); setError('');
    try {
      const res = await api(`/payments/zalopay/status?appTransId=${encodeURIComponent(id)}`);
      setData(res);
      if (res.status === 'PENDING' && attempt < MAX_TRIES) {
        setTimeout(() => check(id, attempt + 1), 3000);
        return;
      }
    } catch (e) {
      setError(e.message || 'Không kiểm tra được trạng thái thanh toán ZaloPay.');
    }
    setLoading(false);
  }

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('apptransid') || params.get('app_trans_id') || '';
    setAppTransId(id);
    try { setPhone(localStorage.getItem('hanseoul:lastPhone') || ''); } catch {}
    if (!id) {
      setError('Thiếu mã giao dịch ZaloPay.');
      setLoading(false);
      return;
    }
    check(id);
  }, []);

  const [title, desc] = messages[data?.status] || ['Đang kiểm tra thanh toán', 'Vui lòng đợi trong giây lát...'];
  const trackHref = data?.orderId ? `/don-hang?orderId=${data.orderId}${phone ? `&phone=${encodeURIComponent(phone)}` : ''}` : '/don-hang';

  return <ShopLayout><section className="shop-page-hero"><div className="shop-container"><span className="shop-eyebrow">ZALOPAY · HAN SEOUL</span><h1>Kết quả <em>thanh toán</em></h1><div className="shop-breadcrumb"><Link href="/">Trang chủ</Link><span>/</span><span>Thanh toán ZaloPay</span></div></div></section>
  <section className="shop-section shop-container"><div className="order-detail-card">
    <div className="order-detail-top"><div>{data?.orderId && <span className="shop-eyebrow">MÃ ĐƠN #{data.orderId}</span>}<h2>{loading && !data ? 'Đang kiểm tra thanh toán' : title}</h2><p>{loading && !data ? 'Vui lòng đợi trong giây lát...' : desc}</p></div>{data && <span className={`order-status-badge status-${data.status === 'SUCCESS' ? 'paid' : data.status === 'PENDING' ? 'open' : 'cancelled'}`}>{data.status}</span>}</div>
    {data && <div className="order-meta-grid"><div><small>Số tiền</small><b>{vnd(data.amount)}</b></div><div><small>Mã giao dịch</small><b>{data.appTransId}</b></div>{data.zpTransId && <div><small>Mã GD ZaloPay</small><b>{data.zpTransId}</b></div>}</div>}
    {error && <p className="checkout-error" role="alert">{error}</p>}
    <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 16 }}>
      {data?.status === 'PENDING' && !loading && <button className="shop-outline-link" onClick={() => check(appTransId)}>↻ Kiểm tra lại</button>}
      <Link className="shop-button" href={trackHref}>Xem đơn hàng ↗</Link>
    </div>
  </div></section></ShopLayout>;
}
