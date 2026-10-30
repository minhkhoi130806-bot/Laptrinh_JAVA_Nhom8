'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import MenuCard from '@/components/shop/MenuCard';
import Icon from '@/components/ui/Icon';
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
    <section className="shop-hero shop-home-hero">
      <div className="shop-container shop-home-grid">
        <div className="shop-hero-content">
          <span className="shop-eyebrow"><i /> VỊ HÀN QUỐC · TÌNH THÂN QUEN</span>
          <h1>Một chút Seoul,<br />ngay trên <em>bàn ăn.</em></h1>
          <p>Khám phá hương vị Hàn Quốc đậm đà, nguyên liệu tươi ngon và những bữa ăn đáng nhớ tại Han Seoul.</p>
          <div className="shop-hero-actions">
            <Link className="shop-button" href="/thuc-don">Khám phá thực đơn <Icon name="arrowUpRight" size={18} /></Link>
            <Link className="shop-text-link" href="/dat-ban">Đặt bàn trước <Icon name="arrowRight" size={18} /></Link>
          </div>
          <div className="shop-hero-stats">
            <div><b>100%</b><span>Hương vị tận tâm</span></div><i />
            <div><b>매일</b><span>Tươi ngon mỗi ngày</span></div><i />
            <div><b>한식</b><span>Ẩm thực Hàn Quốc</span></div>
          </div>
        </div>
        <div className="shop-home-visual">
          <figure className="shop-home-photo">
            <img src="/images/han-seoul-table.webp" alt="Bàn ăn Hàn Quốc với cơm trộn bibimbap, kimchi và những món ăn kèm" width="1536" height="1024" fetchPriority="high" />
            <figcaption><span>정성으로 차린 한 상</span><p>Một bàn đầy hương vị.<br />Một bữa đầy yêu thương.</p></figcaption>
          </figure>
          <div className="shop-home-seal" aria-hidden="true"><b>한</b><small>SEOUL TABLE</small></div>
          <div className="shop-home-caption"><span>THE ART OF SHARING A MEAL</span><i /><span>맛 · 정성 · 함께</span></div>
        </div>
      </div>
    </section>
    <div className="shop-home-ribbon"><div className="shop-container">
      <span><Icon name="leaf" /> Nguyên liệu tươi mỗi ngày</span><i />
      <span><Icon name="bowl" /> Hương vị Hàn thân thuộc</span><i />
      <span><Icon name="heart" /> Tận tâm trong từng bữa ăn</span>
    </div></div>
    <section className="shop-intro shop-container shop-home-intro">
      <div className="shop-intro-mark" aria-hidden="true">한</div>
      <div><span className="shop-eyebrow">CÂU CHUYỆN CỦA CHÚNG TÔI</span><h2>Không chỉ là món ăn.<br /><em>Đó là cảm giác được sẻ chia.</em></h2></div>
      <p>Han Seoul mang tinh thần của những bữa cơm Hàn Quốc đến gần hơn với bạn — mộc mạc, ấm áp và luôn đầy ắp hương vị.</p>
    </section>
    <section className="shop-section shop-featured shop-home-featured"><div className="shop-container">
      <div className="shop-section-heading"><div><span className="shop-eyebrow">TỪ BẾP ĐẾN BÀN</span><h2>Được yêu thích tại Han Seoul</h2><p>Một vài lựa chọn nổi bật để bắt đầu bữa ăn của bạn.</p></div><Link className="shop-outline-link" href="/thuc-don">Xem toàn bộ thực đơn <Icon name="arrowUpRight" size={18} /></Link></div>
      {menuError && <div className="shop-menu-notice" role="status"><Icon name="bowl" size={28} /><p>Thực đơn sẽ hiển thị khi kết nối được với máy chủ nhà hàng.</p></div>}
      {featured.length > 0 ? <div className="shop-menu-grid shop-featured-grid">{featured.map((item) => <MenuCard key={item.id} item={item} compact />)}</div> : !menuError && <div className="shop-featured-loading" role="status"><span className="shop-spinner" />Đang chuẩn bị những món ngon...</div>}
    </div></section>
    <section className="shop-section shop-container shop-discovery">
      <div className="shop-section-heading"><div><span className="shop-eyebrow">HAN SEOUL EXPERIENCE</span><h2>Chuẩn bị cho lần ghé thăm tiếp theo</h2><p>Khám phá thêm tiện ích dành cho khách hàng.</p></div></div>
      <div className="shop-discovery-grid">
        <Link href="/khuyen-mai"><span><Icon name="ticket" /></span><b>Ưu đãi & trải nghiệm</b><small>Gợi ý cho bữa ăn cùng người thân</small><i><Icon name="arrowUpRight" /></i></Link>
        <Link href="/yeu-thich"><span><Icon name="heart" /></span><b>Món ăn yêu thích</b><small>Lưu lại những lựa chọn của bạn</small><i><Icon name="arrowUpRight" /></i></Link>
        <Link href="/tra-cuu-dat-ban"><span><Icon name="calendar" /></span><b>Tra cứu đặt bàn</b><small>Xem yêu cầu đã lưu trên thiết bị</small><i><Icon name="arrowUpRight" /></i></Link>
        <Link href="/cau-hoi"><span><Icon name="message" /></span><b>Trung tâm hỗ trợ</b><small>Giải đáp câu hỏi thường gặp</small><i><Icon name="arrowUpRight" /></i></Link>
      </div>
    </section>
    <section className="shop-experience"><div className="shop-container shop-experience-inner">
      <div className="shop-experience-symbol" aria-hidden="true">한<br /><small>SEOUL</small></div>
      <div><span className="shop-eyebrow">MỘT BỮA ĂN, NHIỀU KỶ NIỆM</span><h2>Gặp nhau ở Han Seoul nhé?</h2><p>Chọn món yêu thích, rủ người thân và để chúng tôi chuẩn bị một chiếc bàn dành riêng cho bạn.</p><Link className="shop-button shop-button-light" href="/dat-ban">Đặt bàn ngay <Icon name="arrowUpRight" size={18} /></Link></div>
      <div className="shop-experience-side">맛있게<br /><em>Enjoy your meal.</em></div>
    </div></section>
  </ShopLayout>;
}
