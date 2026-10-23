import Link from 'next/link';
export default function ShopFooter() {
  return (
    <footer className="shop-footer">
      <div className="shop-footer-inner">
        <div><Link className="shop-brand shop-brand-light" href="/"><span className="shop-brand-mark">한</span><span><b>HAN SEOUL</b><small>KOREAN DINING</small></span></Link><p>Hương vị Hàn Quốc, trải nghiệm ấm áp như ở nhà.</p></div>
        <div><b>Khám phá</b><Link href="/thuc-don">Thực đơn</Link><Link href="/dat-ban">Đặt bàn</Link><Link href="/admin">Khu quản lý</Link></div>
        <div><b>Giờ phục vụ</b><span>Thứ 2 – Chủ nhật</span><span>10:00 – 22:00</span><span>Hotline: 0900 000 888</span></div>
      </div>
      <div className="shop-footer-bottom">© {new Date().getFullYear()} Han Seoul Korean Dining. Một dự án nhà hàng của Nhóm 8.</div>
    </footer>
  );
}
