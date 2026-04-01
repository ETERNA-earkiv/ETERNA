/// <reference path="../.astro/types.d.ts" />

interface AstroUser {
  id: string;
  name: string;
  fullname: string;
  email: string;
  roles: string[];
  groups: string[];
  isActive: boolean;
  isGuest: boolean;
}

declare namespace App {
  interface Locals {
    user: AstroUser | null;
  }
}
