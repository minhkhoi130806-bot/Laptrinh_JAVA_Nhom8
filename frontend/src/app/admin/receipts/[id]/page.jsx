'use client';
import { useParams } from 'next/navigation';
import ReceiptView from '@/components/admin/ReceiptView';

export default function Page() {
  const { id } = useParams();
  return <ReceiptView id={id} />;
}
