import Link from 'next/link';
export default function ShopFooter() {
  return <footer className="shop-footer"><div className="shop-footer-inner">
    <div><Link className="shop-brand shop-brand-light" href="/"><span className="shop-brand-mark">한</span><span><b>HAN SEOUL</b><small>KOREAN DINING</small></span></Link><p>Hương vị Hàn Quốc, trải nghiệm ấm áp như ở nhà.</p><div className="shop-footer-social"><a href="https://www.facebook.com/" target="_blank" rel="noreferrer">Facebook ↗</a><a href="https://www.instagram.com/" target="_blank" rel="noreferrer">Instagram ↗</a></div></div>
    <div><b>Khám phá</b><Link href="/thuc-don">Thực đơn</Link><Link href="/khuyen-mai">Ưu đãi</Link><Link href="/yeu-thich">Món yêu thích</Link><Link href="/dat-tiec">Đặt tiệc nhóm</Link><Link href="/gioi-thieu">Câu chuyện Han Seoul</Link></div>
    <div><b>Hỗ trợ khách hàng</b><Link href="/dat-ban">Đặt bàn</Link><Link href="/tra-cuu-dat-ban">Tra cứu đặt bàn</Link><Link href="/cau-hoi">Câu hỏi thường gặp</Link><Link href="/danh-gia">Đánh giá trải nghiệm</Link><Link href="/lien-he">Liên hệ</Link></div>
    <div><b>Giờ phục vụ</b><span>Thứ 2 – Chủ nhật</span><span>10:00 – 22:00</span><a href="tel:0900000888">Hotline: 0900 000 888</a><a href="mailto:hello@hanseoul.vn">hello@hanseoul.vn</a></div>
  </div><div className="shop-footer-bottom"><span>© {new Date().getFullYear()} Han Seoul Korean Dining. Một dự án nhà hàng của Nhóm 8.</span><span><Link href="/chinh-sach">Chính sách & quyền riêng tư</Link></span></div></footer>;
}
