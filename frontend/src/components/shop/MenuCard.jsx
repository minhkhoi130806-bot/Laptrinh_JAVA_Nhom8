import Link from 'next/link';
import { vnd } from '@/lib/api';
import FavoriteButton from './FavoriteButton';
import AddToCartButton from './AddToCartButton';
const spiceLabels = { NONE: 'Không cay', MILD: 'Cay nhẹ', MEDIUM: 'Cay vừa', HOT: 'Cay nhiều', EXTRA_HOT: 'Rất cay' };
export default function MenuCard({ item, compact = false }) {
  const image = item.imageUrl ? (item.imageUrl.startsWith('http') ? item.imageUrl : item.imageUrl.startsWith('/') ? item.imageUrl : `/${item.imageUrl}`) : '';
  return (
    <article className={`shop-menu-card ${compact ? 'compact' : ''}`}>
      <div className="shop-menu-image">
        <Link href={`/mon-an/${item.id}`} aria-label={`Xem ${item.nameVi}`}>
          {image ? <img src={image} alt={item.nameVi} loading="lazy" /> : <div className="shop-menu-placeholder" aria-label="Ảnh món ăn">🍲</div>}
        </Link>
        <FavoriteButton item={item} />
        {item.spiceLevel && item.spiceLevel !== 'NONE' && <span className="shop-spice">🌶 {spiceLabels[item.spiceLevel] || item.spiceLevel}</span>}
      </div>
      <div className="shop-menu-card-body">
        <div className="shop-menu-card-heading"><h3><Link href={`/mon-an/${item.id}`}>{item.nameVi}</Link></h3><span className="shop-menu-price">{vnd(item.price)}</span></div>
        {item.nameKo && <p className="shop-menu-korean">{item.nameKo}</p>}
        <p className="shop-menu-description">{item.description || 'Được chế biến tươi ngon từ những nguyên liệu chọn lọc.'}</p>
        <div className="shop-card-actions"><Link className="shop-card-detail-link" href={`/mon-an/${item.id}`}>Xem chi tiết <span>↗</span></Link><AddToCartButton item={item} /></div>
      </div>
    </article>
  );
}
