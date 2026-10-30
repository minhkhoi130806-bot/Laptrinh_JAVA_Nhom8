'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import AddToCartButton from '@/components/shop/AddToCartButton';
import FavoriteButton from '@/components/shop/FavoriteButton';
import MenuCard from '@/components/shop/MenuCard';
import { api, vnd } from '@/lib/api';
export default function DishDetailPage() {
 const { id } = useParams(); const [item,setItem]=useState(null); const [related,setRelated]=useState([]); const [loading,setLoading]=useState(true); const [error,setError]=useState('');
 useEffect(()=>{let alive=true; Promise.all([api(`/menu-items/${id}`),api('/menu-items')]).then(([one,all])=>{if(alive){setItem(one);setRelated((all||[]).filter(x=>x.available&&!x.deleted&&String(x.id)!==String(id)).slice(0,3));}}).catch(e=>alive&&setError(e.message||'Không tải được món ăn.')).finally(()=>alive&&setLoading(false));return()=>{alive=false}},[id]);
 const image=item?.imageUrl ? (item.imageUrl.startsWith('http')||item.imageUrl.startsWith('/')?item.imageUrl:`/${item.imageUrl}`):'';
 return <ShopLayout><section className="shop-page-hero"><div className="shop-container"><div className="shop-breadcrumb"><Link href="/">Trang chủ</Link><span>/</span><Link href="/thuc-don">Thực đơn</Link><span>/</span><span>Chi tiết món</span></div></div></section>{loading?<div className="shop-state"><span className="shop-spinner"/>Đang tải thông tin món...</div>:error?<div className="shop-state shop-state-error">{error}<p><Link href="/thuc-don">Quay lại thực đơn</Link></p></div>:item&&<><section className="shop-section shop-container dish-detail-layout"><div className="dish-detail-image">{image?<img src={image} alt={item.nameVi}/>:<div className="shop-menu-placeholder">🍲</div>}<FavoriteButton item={item}/></div><div className="dish-detail-content"><span className="shop-eyebrow">HAN SEOUL · MENU</span><h1>{item.nameVi}</h1>{item.nameKo&&<p className="shop-menu-korean">{item.nameKo}</p>}<strong className="dish-detail-price">{vnd(item.price)}</strong><p>{item.description||'Món ăn được chế biến tươi ngon theo phong vị Hàn Quốc.'}</p><div className="dish-detail-tags">{item.spiceLevel&&<span>🌶 Độ cay: {item.spiceLevel}</span>}{item.available?<span>● Đang phục vụ</span>:<span>Hết món</span>}</div><AddToCartButton item={item} className="shop-button dish-add-button" label="Thêm vào giỏ hàng ↗"/><p className="dish-detail-note">Ghi chú riêng và số lượng có thể điều chỉnh trong giỏ hàng.</p></div></section><section className="shop-section shop-container"><div className="shop-section-heading"><div><span className="shop-eyebrow">CÓ THỂ BẠN SẼ THÍCH</span><h2>Khám phá thêm món ngon</h2></div><Link className="shop-outline-link" href="/thuc-don">Xem thực đơn ↗</Link></div><div className="shop-menu-grid">{related.map(row=><MenuCard key={row.id} item={row} compact/>)}</div></section></>}</ShopLayout>;
}
