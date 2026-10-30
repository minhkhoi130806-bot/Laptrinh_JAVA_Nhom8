'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import { api, vnd } from '@/lib/api';

// VNPay chuyển khách về đây kèm ?vnp_TxnRef=...&vnp_ResponseCode=...&vnp_Amount=...
// Các tham số trên URL KHÔNG được tin trực tiếp: trang gửi toàn bộ params lên backend
// để backend xác thực chữ ký rồi trả kết quả tin cậy.
const MAX_TRIES = 8;
const messages = {
  SUCCESS: ['Thanh toán thành công', 'VNPay đã xác nhận giao dịch. Nhà hàng sẽ chuẩn bị đơn của bạn.'],
  FAILED: ['Thanh toán chưa thành công', 'Giao dịch bị hủy hoặc thất bại, bạn chưa bị trừ tiền. Có thể thử lại từ trang theo dõi đơn hàng.'],
  EXPIRED: ['Giao dịch đã hết hạn', 'Bạn có thể tạo lại giao dịch từ trang theo dõi đơn hàng.'],
  PENDING: ['Đang chờ VNPay xác nhận', 'Hệ thống chưa nhận được kết quả cuối cùng. Vui lòng bấm kiểm tra lại sau ít phút, đừng thanh toán lần nữa.'],
};

export default function VnpayResultPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [vnpTxnRef, setVnpTxnRef] = useState('');
  const [phone, setPhone] = useState('');

  async function checkReturn(searchParams) {
    setLoading(true); setError('');
    try {
      // Gửi toàn bộ query params lên backend để xác thực chữ ký
      const queryString = searchParams.toString();
      const res = await api(`/payments/vnpay/return?${queryString}`);
      setData(res);
    } catch (e) {
      setError(e.message || 'Không kiểm tra được trạng thái thanh toán.');
    }
    setLoading(false);
  }

  async function checkStatus(txnRef, attempt = 1) {
    setLoading(true); setError('');
    try {
      const res = await api(`/payments/vnpay/status?txnRef=${encodeURIComponent(txnRef)}`);
      setData(res);
      if (res.status === 'PENDING' && attempt < MAX_TRIES) {
        setTimeout(() => checkStatus(txnRef, attempt + 1), 3000);
        return;
      }
    } catch (e) { setError(e.message || 'Không kiểm tra được trạng thái thanh toán.'); }
    setLoading(false);
  }

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const txnRef = params.get('vnp_TxnRef') || '';
    setVnpTxnRef(txnRef);
    try { setPhone(localStorage.getItem('hanseoul:lastPhone') || ''); } catch {}
    if (!txnRef) { setError('Thiếu mã giao dịch.'); setLoading(false); return; }

    // Nếu có vnp_SecureHash => đây là return URL từ VNPay, gửi lên backend xác thực
    if (params.get('vnp_SecureHash')) {
      checkReturn(params);
    } else {
      // Nếu không có chữ ký => người dùng truy cập lại trang, chỉ hỏi trạng thái
      checkStatus(txnRef);
    }
  }, []);

  const [title, desc] = messages[data?.status] || ['Đang kiểm tra thanh toán', 'Vui lòng đợi trong giây lát...'];
  const trackHref = data?.orderId ? `/don-hang?orderId=${data.orderId}${phone ? `&phone=${encodeURIComponent(phone)}` : ''}` : '/don-hang';
  return <ShopLayout><section className="shop-page-hero"><div className="shop-container"><span className="shop-eyebrow">VNPAY · HAN SEOUL</span><h1>Kết quả <em>thanh toán</em></h1><div className="shop-breadcrumb"><Link href="/">Trang chủ</Link><span>/</span><span>Thanh toán VNPay</span></div></div></section>
  <section className="shop-section shop-container"><div className="order-detail-card">
    <div className="order-detail-top"><div>{data?.orderId && <span className="shop-eyebrow">MÃ ĐƠN #{data.orderId}</span>}<h2>{loading && !data ? 'Đang kiểm tra thanh toán' : title}</h2><p>{loading && !data ? 'Vui lòng đợi trong giây lát...' : desc}</p></div>{data && <span className={`order-status-badge status-${data.status === 'SUCCESS' ? 'paid' : data.status === 'PENDING' ? 'open' : 'cancelled'}`}>{data.status}</span>}</div>
    {data && <div className="order-meta-grid"><div><small>Số tiền</small><b>{vnd(data.amount)}</b></div><div><small>Mã giao dịch</small><b>{data.vnpTxnRef}</b></div>{data.transactionNo && <div><small>Mã GD VNPay</small><b>{data.transactionNo}</b></div>}</div>}
    {error && <p className="checkout-error" role="alert">{error}</p>}
    <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 16 }}>
      {data?.status === 'PENDING' && !loading && <button className="shop-outline-link" onClick={() => checkStatus(vnpTxnRef)}>↻ Kiểm tra lại</button>}
      <Link className="shop-button" href={trackHref}>Xem đơn hàng ↗</Link>
    </div>
  </div></section></ShopLayout>;
}
