import axios from 'axios';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://34.76.76.49';

const api = axios.create({ baseURL: API_URL });

// attach token to every request automatically
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// redirect to login if token is expired or invalid
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 403 && typeof window !== 'undefined') {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default api;

// --- types ---

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export type Status = 'PENDING' | 'IN_PROGRESS' | 'DONE' | 'ARCHIVED';
export type Category = 'WORK' | 'PERSONAL' | 'URGENT' | 'OTHER';

export interface Task {
  id: number;
  title: string;
  description: string;
  priority: Priority;
  status: Status;
  category: Category | null;
  aiSummary: string | null;
  aiAnalysed: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  name: string;
}

// --- auth ---

export const register = (name: string, email: string, password: string) =>
  api.post<AuthResponse>('/api/auth/register', { name, email, password });

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/api/auth/login', { email, password });

// --- tasks ---

export const getTasks = () => api.get<Task[]>('/api/tasks');

export const createTask = (title: string, description: string, priority: Priority) =>
  api.post<Task>('/api/tasks', { title, description, priority });

export const updateTask = (id: number, data: Partial<{ title: string; description: string; priority: Priority; status: Status; category: Category }>) =>
  api.put<Task>(`/api/tasks/${id}`, data);

export const deleteTask = (id: number) => api.delete(`/api/tasks/${id}`);

export const analyseTask = (id: number) =>
  api.post<{ taskId: number; summary: string; suggestedPriority: Priority; suggestedCategory: Category }>(`/api/tasks/${id}/analyse`);
