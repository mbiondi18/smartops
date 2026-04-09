'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getTasks, createTask, updateTask, deleteTask, analyseTask, Task, Priority, Status } from '@/lib/api';

const PRIORITY_COLORS: Record<Priority, string> = {
  LOW: 'bg-green-100 text-green-700',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  HIGH: 'bg-red-100 text-red-700',
};

const STATUS_COLORS: Record<Status, string> = {
  PENDING: 'bg-gray-100 text-gray-600',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  DONE: 'bg-emerald-100 text-emerald-700',
  ARCHIVED: 'bg-gray-100 text-gray-400',
};

export default function Dashboard() {
  const router = useRouter();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [user, setUser] = useState<{ name: string; email: string } | null>(null);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [analysing, setAnalysing] = useState<number | null>(null);

  // create form state
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newPriority, setNewPriority] = useState<Priority>('MEDIUM');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');
    if (!token) { router.replace('/login'); return; }
    if (storedUser) setUser(JSON.parse(storedUser));
    loadTasks();
  }, []);

  async function loadTasks() {
    try {
      const res = await getTasks();
      setTasks(res.data);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setCreating(true);
    try {
      await createTask(newTitle, newDesc, newPriority);
      setNewTitle(''); setNewDesc(''); setNewPriority('MEDIUM');
      setShowCreate(false);
      await loadTasks();
    } finally {
      setCreating(false);
    }
  }

  async function handleStatusChange(task: Task, status: Status) {
    await updateTask(task.id, { status });
    await loadTasks();
    if (selectedTask?.id === task.id) setSelectedTask({ ...selectedTask, status });
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this task?')) return;
    await deleteTask(id);
    setSelectedTask(null);
    await loadTasks();
  }

  async function handleAnalyse(task: Task) {
    setAnalysing(task.id);
    try {
      const res = await analyseTask(task.id);
      await loadTasks();
      const updated = await getTasks();
      const refreshed = updated.data.find(t => t.id === task.id);
      if (refreshed) setSelectedTask(refreshed);
    } finally {
      setAnalysing(null);
    }
  }

  function handleLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    router.push('/login');
  }

  if (loading) return <div className="min-h-screen flex items-center justify-center text-gray-400">Loading...</div>;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Navbar */}
      <nav className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
        <h1 className="text-xl font-bold text-blue-600">SmartOps Hub</h1>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">Hello, <strong>{user?.name}</strong></span>
          <button onClick={handleLogout} className="text-sm text-gray-500 hover:text-gray-800">Logout</button>
        </div>
      </nav>

      <div className="max-w-6xl mx-auto px-6 py-8 flex gap-6">
        {/* Task list */}
        <div className="flex-1">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold">My Tasks <span className="text-gray-400 font-normal text-base">({tasks.length})</span></h2>
            <button
              onClick={() => setShowCreate(!showCreate)}
              className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition"
            >
              + New Task
            </button>
          </div>

          {/* Create form */}
          {showCreate && (
            <form onSubmit={handleCreate} className="bg-white rounded-xl border border-gray-200 p-5 mb-4 space-y-3">
              <input
                type="text"
                placeholder="Task title"
                required
                value={newTitle}
                onChange={e => setNewTitle(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <textarea
                placeholder="Description (optional)"
                value={newDesc}
                onChange={e => setNewDesc(e.target.value)}
                rows={2}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              />
              <div className="flex gap-2">
                <select
                  value={newPriority}
                  onChange={e => setNewPriority(e.target.value as Priority)}
                  className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="LOW">Low priority</option>
                  <option value="MEDIUM">Medium priority</option>
                  <option value="HIGH">High priority</option>
                </select>
                <button
                  type="submit"
                  disabled={creating}
                  className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-sm font-medium px-4 py-2 rounded-lg transition"
                >
                  {creating ? 'Creating...' : 'Create'}
                </button>
                <button type="button" onClick={() => setShowCreate(false)} className="text-sm text-gray-500 hover:text-gray-800 px-2">
                  Cancel
                </button>
              </div>
            </form>
          )}

          {/* Tasks */}
          {tasks.length === 0 ? (
            <div className="text-center py-16 text-gray-400">
              <p className="text-lg">No tasks yet</p>
              <p className="text-sm">Click &quot;New Task&quot; to get started</p>
            </div>
          ) : (
            <div className="space-y-2">
              {tasks.map(task => (
                <div
                  key={task.id}
                  onClick={() => setSelectedTask(task)}
                  className={`bg-white rounded-xl border px-5 py-4 cursor-pointer hover:border-blue-300 transition ${selectedTask?.id === task.id ? 'border-blue-400 ring-1 ring-blue-200' : 'border-gray-200'}`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <p className={`font-medium truncate ${task.status === 'DONE' ? 'line-through text-gray-400' : ''}`}>{task.title}</p>
                      {task.description && <p className="text-sm text-gray-400 truncate mt-0.5">{task.description}</p>}
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${PRIORITY_COLORS[task.priority]}`}>{task.priority}</span>
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_COLORS[task.status]}`}>{task.status.replace('_', ' ')}</span>
                      {task.aiAnalysed && <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-purple-100 text-purple-700">AI ✓</span>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Task detail panel */}
        {selectedTask && (
          <div className="w-80 shrink-0">
            <div className="bg-white rounded-xl border border-gray-200 p-5 sticky top-8">
              <div className="flex items-start justify-between mb-4">
                <h3 className="font-semibold text-lg leading-tight">{selectedTask.title}</h3>
                <button onClick={() => setSelectedTask(null)} className="text-gray-400 hover:text-gray-600 ml-2">✕</button>
              </div>

              {selectedTask.description && (
                <p className="text-sm text-gray-600 mb-4">{selectedTask.description}</p>
              )}

              <div className="flex gap-2 mb-4 flex-wrap">
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${PRIORITY_COLORS[selectedTask.priority]}`}>{selectedTask.priority}</span>
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_COLORS[selectedTask.status]}`}>{selectedTask.status.replace('_', ' ')}</span>
                {selectedTask.category && (
                  <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700">{selectedTask.category}</span>
                )}
              </div>

              {/* Status change */}
              <div className="mb-4">
                <label className="text-xs font-medium text-gray-500 block mb-1">Change status</label>
                <select
                  value={selectedTask.status}
                  onChange={e => handleStatusChange(selectedTask, e.target.value as Status)}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="PENDING">Pending</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="DONE">Done</option>
                  <option value="ARCHIVED">Archived</option>
                </select>
              </div>

              {/* AI Analysis */}
              {selectedTask.aiAnalysed && selectedTask.aiSummary ? (
                <div className="bg-purple-50 border border-purple-100 rounded-lg p-3 mb-4">
                  <p className="text-xs font-semibold text-purple-700 mb-1">AI Analysis</p>
                  <p className="text-sm text-gray-700">{selectedTask.aiSummary}</p>
                </div>
              ) : (
                <button
                  onClick={() => handleAnalyse(selectedTask)}
                  disabled={analysing === selectedTask.id}
                  className="w-full bg-purple-600 hover:bg-purple-700 disabled:bg-purple-400 text-white text-sm font-medium py-2 rounded-lg transition mb-4"
                >
                  {analysing === selectedTask.id ? 'Analysing...' : '✦ Analyse with AI'}
                </button>
              )}

              <p className="text-xs text-gray-400 mb-4">Created {new Date(selectedTask.createdAt).toLocaleDateString()}</p>

              <button
                onClick={() => handleDelete(selectedTask.id)}
                className="w-full text-sm text-red-500 hover:text-red-700 hover:bg-red-50 py-2 rounded-lg transition"
              >
                Delete task
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
