'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import { api, vnd } from '@/lib/api';

// MoMo chuyển khách về đây kèm ?orderId=<mã giao dịch MoMo>&resultCode=...
// Các tham số trên URL KHÔNG được tin: trang chỉ lấy mã giao dịch rồi hỏi backend, backend truy vấn trực tiếp MoMo.
const MAX_TRIES = 8;
const messages = {
  SUCCESS: ['Thanh toán thành công', 'MoMo đã xác nhận giao dịch. Nhà hàng sẽ chuẩn bị đơn của bạn.'],
  FAILED: ['Thanh toán chưa thành công', 'Giao dịch bị hủy hoặc thất bại, bạn chưa bị trừ tiền. Có thể thử lại từ trang theo dõi đơn hàng.'],
  EXPIRED: ['Giao dịch đã hết hạn', 'Bạn có thể tạo lại giao dịch từ trang theo dõi đơn hàng.'],
  PENDING: ['Đang chờ MoMo xác nhận', 'Hệ thống chưa nhận được kết quả cuối cùng. Vui lòng bấm kiểm tra lại sau ít phút, đừng thanh toán lần nữa.'],
};

export default function MomoResultPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [momoOrderId, setMomoOrderId] = useState('');
  const [phone, setPhone] = useState('');

  async function check(id, attempt = 1) {
    setLoading(true); setError('');
    try {
      const res = await api(`/payments/momo/status?orderId=${encodeURIComponent(id)}`);
      setData(res);
      if (res.status === 'PENDING' && attempt < MAX_TRIES) { setTimeout(() => check(id, attempt + 1), 3000); return; }
    } catch (e) { setError(e.message || 'Không kiểm tra được trạng thái thanh toán.'); }
    setLoading(false);
  }

  useEffect(() => {
    const id = new URLSearchParams(window.location.search).get('orderId') || '';
    setMomoOrderId(id);
    try { setPhone(localStorage.getItem('hanseoul:lastPhone') || ''); } catch {}
    if (!id) { setError('Thiếu mã giao dịch.'); setLoading(false); return; }
    check(id);
  }, []);

  const [title, desc] = messages[data?.status] || ['Đang kiểm tra thanh toán', 'Vui lòng đợi trong giây lát...'];
  const trackHref = data?.orderId ? `/don-hang?orderId=${data.orderId}${phone ? `&phone=${encodeURIComponent(phone)}` : ''}` : '/don-hang';
  return <ShopLayout><section className="shop-page-hero"><div className="shop-container"><span className="shop-eyebrow">MOMO · HAN SEOUL</span><h1>Kết quả <em>thanh toán</em></h1><div className="shop-breadcrumb"><Link href="/">Trang chủ</Link><span>/</span><span>Thanh toán MoMo</span></div></div></section>
  <section className="shop-section shop-container"><div className="order-detail-card">
    <div className="order-detail-top"><div>{data?.orderId && <span className="shop-eyebrow">MÃ ĐƠN #{data.orderId}</span>}<h2>{loading && !data ? 'Đang kiểm tra thanh toán' : title}</h2><p>{loading && !data ? 'Vui lòng đợi trong giây lát...' : desc}</p></div>{data && <span className={`order-status-badge status-${data.status === 'SUCCESS' ? 'paid' : data.status === 'PENDING' ? 'open' : 'cancelled'}`}>{data.status}</span>}</div>
    {data && <div className="order-meta-grid"><div><small>Số tiền</small><b>{vnd(data.amount)}</b></div><div><small>Mã giao dịch</small><b>{data.momoOrderId}</b></div></div>}
    {error && <p className="checkout-error" role="alert">{error}</p>}
    <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 16 }}>
      {data?.status === 'PENDING' && !loading && <button className="shop-outline-link" onClick={() => check(momoOrderId)}>↻ Kiểm tra lại</button>}
      <Link className="shop-button" href={trackHref}>Xem đơn hàng ↗</Link>
    </div>
  </div></section></ShopLayout>;
}
