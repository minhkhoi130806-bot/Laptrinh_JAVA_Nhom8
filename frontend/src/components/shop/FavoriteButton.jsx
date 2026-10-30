'use client';
import { useEffect, useState } from 'react';

const KEY = 'hanseoul:favorites';
export function readFavorites() {
  if (typeof window === 'undefined') return [];
  try { return JSON.parse(localStorage.getItem(KEY) || '[]'); } catch { return []; }
}
export function saveFavorites(rows) {
  if (typeof window !== 'undefined') {
    localStorage.setItem(KEY, JSON.stringify(rows));
    window.dispatchEvent(new Event('favorites:changed'));
  }
}
export default function FavoriteButton({ item, className = '' }) {
  const [active, setActive] = useState(false);
  useEffect(() => {
    const refresh = () => setActive(readFavorites().some((row) => String(row.id) === String(item.id)));
    refresh();
    window.addEventListener('favorites:changed', refresh);
    window.addEventListener('storage', refresh);
    return () => { window.removeEventListener('favorites:changed', refresh); window.removeEventListener('storage', refresh); };
  }, [item.id]);
  const toggle = (event) => {
    event.preventDefault(); event.stopPropagation();
    const current = readFavorites();
    const exists = current.some((row) => String(row.id) === String(item.id));
    saveFavorites(exists ? current.filter((row) => String(row.id) !== String(item.id)) : [...current, { id: item.id, nameVi: item.nameVi, nameKo: item.nameKo, price: item.price, imageUrl: item.imageUrl, description: item.description, spiceLevel: item.spiceLevel }]);
    setActive(!exists);
  };
  return <button type="button" className={`shop-favorite-button ${active ? 'is-favorite' : ''} ${className}`} onClick={toggle} aria-label={active ? 'Bỏ khỏi món yêu thích' : 'Thêm vào món yêu thích'} title={active ? 'Bỏ yêu thích' : 'Yêu thích'}>{active ? '♥' : '♡'}</button>;
}
