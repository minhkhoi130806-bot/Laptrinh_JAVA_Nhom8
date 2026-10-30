'use client';

const CART_KEY = 'hanseoul:cart:v1';
export function readCart() {
  if (typeof window === 'undefined') return [];
  try { const value = JSON.parse(localStorage.getItem(CART_KEY) || '[]'); return Array.isArray(value) ? value : []; }
  catch { return []; }
}
function publish(items) {
  localStorage.setItem(CART_KEY, JSON.stringify(items));
  window.dispatchEvent(new CustomEvent('cart:changed', { detail: items }));
}
export function addToCart(item, quantity = 1, note = '') {
  const rows = readCart();
  const id = Number(item.id);
  const found = rows.find((row) => Number(row.id) === id && (row.note || '') === note);
  if (found) found.quantity = Math.min(99, Number(found.quantity || 1) + quantity);
  else rows.push({ id, name: item.nameVi || item.name || 'Món ăn', imageUrl: item.imageUrl || '', price: Number(item.price || 0), quantity: Math.min(99, Math.max(1, quantity)), note });
  publish(rows);
  return rows;
}
export function updateCartItem(id, quantity, note) {
  const rows = readCart().map((row) => Number(row.id) === Number(id) ? { ...row, quantity: Math.max(1, Math.min(99, Number(quantity) || 1)), ...(note === undefined ? {} : { note }) } : row);
  publish(rows); return rows;
}
export function removeCartItem(id, note) {
  const rows = readCart().filter((row) => !(Number(row.id) === Number(id) && (note === undefined || (row.note || '') === note)));
  publish(rows); return rows;
}
export function clearCart() { publish([]); }
export function cartCount(rows = readCart()) { return rows.reduce((sum, row) => sum + Number(row.quantity || 0), 0); }
export function cartTotal(rows = readCart()) { return rows.reduce((sum, row) => sum + Number(row.price || 0) * Number(row.quantity || 0), 0); }
