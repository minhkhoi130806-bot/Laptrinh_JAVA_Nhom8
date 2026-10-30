'use client';
import { useState } from 'react';
import { addToCart } from '@/lib/cart';
import Icon from '@/components/ui/Icon';
export default function AddToCartButton({ item, className = '', label = 'Thêm vào giỏ' }) {
  const [added, setAdded] = useState(false);
  function add() {
    addToCart(item);
    setAdded(true);
    window.setTimeout(() => setAdded(false), 1400);
  }
  return <button type="button" className={className || 'shop-add-cart'} onClick={add}><Icon name={added ? 'check' : 'plus'} size={16}/><span>{added ? 'Đã thêm' : label}</span></button>;
}
