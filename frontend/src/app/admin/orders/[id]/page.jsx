'use client';
import { useParams } from 'next/navigation';
import OrderDetail from '@/components/admin/OrderDetail';

// Bấm vào 1 đơn trong danh sách -> trang chi tiết đơn hàng (các món, hóa đơn...)
export default function Page() {
  const { id } = useParams();
  return <OrderDetail id={id} />;
}
