'use client';
import { useParams } from 'next/navigation';
import ReservationForm from '@/components/admin/PhucReservationForm';

export default function Page() {
  const { id } = useParams();
  return <ReservationForm id={id} />;
}
