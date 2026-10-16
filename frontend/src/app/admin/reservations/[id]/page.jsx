'use client';
import { useParams } from 'next/navigation';
import ReservationForm from '@/components/admin/ReservationForm';

export default function Page() {
  const { id } = useParams();
  return <ReservationForm id={id} />;
}
