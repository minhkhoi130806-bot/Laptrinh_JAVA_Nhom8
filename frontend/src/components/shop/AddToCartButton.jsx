'use client';
import { useState } from 'react';
import { addToCart } from '@/lib/cart';
export default function AddToCartButton({ item, className = '', label = 'Thêm vào giỏ' }) {
  const [added, setAdded] = useState(false);
  function add() {
    addToCart(item);
    setAdded(true);
    window.setTimeout(() => setAdded(false), 1400);
  }
  return <button type="button" className={className || 'shop-add-cart'} onClick={add}>{added ? '✓ Đã thêm' : label}</button>;
}
