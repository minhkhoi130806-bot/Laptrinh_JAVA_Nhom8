import { vnd } from '@/lib/api';
const spiceLabels = { NONE: 'Không cay', MILD: 'Cay nhẹ', MEDIUM: 'Cay vừa', HOT: 'Cay nhiều', EXTRA_HOT: 'Rất cay' };
export default function MenuCard({ item, compact = false }) {
  const image = item.imageUrl ? (item.imageUrl.startsWith('http') ? item.imageUrl : item.imageUrl.startsWith('/') ? item.imageUrl : `/${item.imageUrl}`) : '';
  return (
    <article className={`shop-menu-card ${compact ? 'compact' : ''}`}>
      <div className="shop-menu-image">
        {image ? <img src={image} alt={item.nameVi} loading="lazy" /> : <div className="shop-menu-placeholder" aria-label="Ảnh món ăn">🍲</div>}
        {item.spiceLevel && item.spiceLevel !== 'NONE' && <span className="shop-spice">🌶 {spiceLabels[item.spiceLevel] || item.spiceLevel}</span>}
      </div>
      <div className="shop-menu-card-body">
        <div className="shop-menu-card-heading"><h3>{item.nameVi}</h3><span className="shop-menu-price">{vnd(item.price)}</span></div>
        {item.nameKo && <p className="shop-menu-korean">{item.nameKo}</p>}
        <p className="shop-menu-description">{item.description || 'Được chế biến tươi ngon từ những nguyên liệu chọn lọc.'}</p>
      </div>
    </article>
  );
}
