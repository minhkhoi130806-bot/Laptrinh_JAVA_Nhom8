"use client";
import ResourceList from "@/components/admin/ResourceList";
import { reservations } from "@/lib/admin/resources";

export default function Page() {
  return <ResourceList config={reservations} />;
}
