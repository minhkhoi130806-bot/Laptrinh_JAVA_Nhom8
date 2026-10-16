'use client';
import { useParams } from 'next/navigation';
import InvoiceView from '@/components/admin/InvoiceView';

export default function Page() {
  const { id } = useParams();
  return <InvoiceView id={id} />;
}
