'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import MenuCard from '@/components/shop/MenuCard';
import { api } from '@/lib/api';

export default function HomePage() {
  const [featured, setFeatured] = useState([]);
  const [menuError, setMenuError] = useState(false);
  useEffect(() => {
    let alive = true;
    api('/menu-items').then((rows) => alive && setFeatured((rows || []).filter((item) => item.available && !item.deleted).slice(0, 3))).catch(() => alive && setMenuError(true));
    return () => { alive = false; };
  }, []);
  return <ShopLayout>
    <section className="shop-hero"><div className="shop-hero-art" aria-hidden="true"><div className="shop-hero-orbit orbit-one"/><div className="shop-hero-orbit orbit-two"/><div className="shop-hero-dish"><span>한</span><i>SEOUL<br/>TABLE</i></div><span className="shop-floating-tag tag-top">KOREAN COMFORT FOOD</span><span className="shop-floating-tag tag-bottom">정성 · 맛 · 함께</span></div><div className="shop-container shop-hero-content"><span className="shop-eyebrow"><i/> VỊ HÀN QUỐC · TÌNH THÂN QUEN</span><h1>Một chút Seoul,<br/>ngay trên <em>bàn ăn.</em></h1><p>Khám phá hương vị Hàn Quốc đậm đà, nguyên liệu tươi ngon và những bữa ăn đáng nhớ tại Han Seoul.</p><div className="shop-hero-actions"><Link className="shop-button" href="/thuc-don">Khám phá thực đơn <span>↗</span></Link><Link className="shop-text-link" href="/dat-ban">Đặt bàn trước <span>→</span></Link></div><div className="shop-hero-stats"><div><b>100%</b><span>Hương vị tận tâm</span></div><i/><div><b>매일</b><span>Tươi ngon mỗi ngày</span></div><i/><div><b>한식</b><span>Ẩm thực Hàn Quốc</span></div></div></div><div className="shop-hero-index">01 <span/> 03</div></section>
    <section className="shop-intro shop-container"><div className="shop-intro-mark">“</div><div><span className="shop-eyebrow">CÂU CHUYỆN CỦA CHÚNG TÔI</span><h2>Không chỉ là món ăn.<br/><em>Đó là cảm giác được sẻ chia.</em></h2></div><p>Han Seoul mang tinh thần của những bữa cơm Hàn Quốc đến gần hơn với bạn — mộc mạc, ấm áp và luôn đầy ắp hương vị.</p></section>
    <section className="shop-section shop-featured"><div className="shop-container"><div className="shop-section-heading"><div><span className="shop-eyebrow">TỪ BẾP ĐẾN BÀN</span><h2>Được yêu thích tại Han Seoul</h2><p>Một vài lựa chọn nổi bật để bắt đầu bữa ăn của bạn.</p></div><Link className="shop-outline-link" href="/thuc-don">Xem toàn bộ thực đơn <span>↗</span></Link></div>{menuError && <div className="shop-menu-notice">Thực đơn sẽ hiển thị khi kết nối được với máy chủ nhà hàng.</div>}{featured.length > 0 ? <div className="shop-menu-grid shop-featured-grid">{featured.map((item) => <MenuCard key={item.id} item={item} compact />)}</div> : !menuError && <div className="shop-featured-loading">Đang chuẩn bị những món ngon...</div>}</div></section>
    <section className="shop-experience"><div className="shop-container shop-experience-inner"><div className="shop-experience-symbol">한<br/><small>SEOUL</small></div><div><span className="shop-eyebrow">MỘT BỮA ĂN, NHIỀU KỶ NIỆM</span><h2>Gặp nhau ở Han Seoul nhé?</h2><p>Chọn món yêu thích, rủ người thân và để chúng tôi chuẩn bị một chiếc bàn dành riêng cho bạn.</p><Link className="shop-button shop-button-light" href="/dat-ban">Đặt bàn ngay <span>↗</span></Link></div><div className="shop-experience-side">맛있게<br/><em>Enjoy your meal.</em></div></div></section>
  </ShopLayout>;
}
