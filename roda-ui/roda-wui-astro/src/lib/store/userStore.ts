import { create } from "zustand";

export interface UserState {
  user: AstroUser | null;
  setUser: (user: AstroUser | null) => void;
  logout: () => void;
}

export interface AstroUser {
  id: string;
  name: string;
  fullname: string;
  email: string;
  roles: string[];
  groups: string[];
  isActive: boolean;
  isGuest: boolean;
}

export const useUserStore = create<UserState>((set) => ({
  user: null,
  setUser: (user) => set({ user }),
  logout: () => set({ user: null }),
}));
