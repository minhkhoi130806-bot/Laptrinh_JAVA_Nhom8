'use client';
import { useParams } from 'next/navigation';
import ResourceForm from '@/components/admin/ResourceForm';
import RecipeEditor from '@/components/admin/RecipeEditor';
import { menu } from '@/lib/admin/resources';

export default function Page() {
  const { id } = useParams();
  return <ResourceForm config={menu} id={id} extra={(m) => <RecipeEditor menuId={m.id} />} />;
}
