'use client';
import { useParams } from 'next/navigation';
import ResourceForm from '@/components/admin/PhucResourceForm';
import CustomerOrders from '@/components/admin/PhucCustomerOrders';
import { customers } from '@/lib/admin/resources';

// Chi tiết khách hàng: sửa thông tin + xem các đơn khách đã mua
export default function Page() {
  const { id } = useParams();
  return <ResourceForm config={customers} id={id} extra={(c) => <CustomerOrders customerId={c.id} />} />;
}
