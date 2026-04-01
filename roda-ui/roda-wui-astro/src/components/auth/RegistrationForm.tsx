/**
 * RegistrationForm — self-registration form.
 * Replaces GWT Register.java.
 * POST /api/v2/members/users/register
 */
import { useState } from "react";

export default function RegistrationForm() {
  const [form, setForm] = useState({ name: "", fullname: "", email: "", password: "", confirm: "" });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    if (form.password !== form.confirm) {
      setError("Passwords do not match.");
      return;
    }
    setLoading(true);
    try {
      const res = await fetch("/api/v2/members/users/register", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: form.name, fullname: form.fullname, email: form.email, password: form.password }),
      });
      if (!res.ok) {
        const msg = await res.text().catch(() => res.statusText);
        setError(msg || `Registration failed (${res.status})`);
        return;
      }
      setSuccess(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <div className="text-center space-y-3">
        <p className="text-4xl">✅</p>
        <h2 className="text-lg font-semibold text-gray-800">Registration submitted</h2>
        <p className="text-sm text-gray-500">Your account request has been submitted. You will receive an email when it is approved.</p>
        <a href="/login" className="text-sm text-blue-600 hover:underline">Back to login</a>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Username *</label>
        <input required value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Full name</label>
        <input value={form.fullname} onChange={(e) => setForm((p) => ({ ...p, fullname: e.target.value }))}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Email *</label>
        <input required type="email" value={form.email} onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Password *</label>
        <input required type="password" value={form.password} onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
      </div>
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Confirm password *</label>
        <input required type="password" value={form.confirm} onChange={(e) => setForm((p) => ({ ...p, confirm: e.target.value }))}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <button type="submit" disabled={loading}
        className="w-full py-2 bg-blue-600 text-white text-sm rounded-lg font-medium hover:bg-blue-700 disabled:opacity-40 transition-colors">
        {loading ? "Registering…" : "Register"}
      </button>
      <p className="text-center text-xs text-gray-500">
        Already have an account? <a href="/login" className="text-blue-600 hover:underline">Sign in</a>
      </p>
    </form>
  );
}
