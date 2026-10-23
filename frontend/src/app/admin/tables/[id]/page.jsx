'use client';
import { useParams } from 'next/navigation';
import ResourceForm from '@/components/admin/PhucResourceForm';
import TableReservations from '@/components/admin/PhucTableReservations';
import { tables } from '@/lib/admin/resources';

// Chi tiết bàn: sửa thông tin + xem các lượt đặt của bàn
export default function Page() {
  const { id } = useParams();
  return <ResourceForm config={tables} id={id} extra={(t) => <TableReservations tableId={t.id} />} />;
}
