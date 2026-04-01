/**
 * PasswordRecoveryForm — request a password reset email.
 * Replaces GWT RecoverLogin.java.
 * POST /api/v2/members/users/password_reset_token
 */
import { useState } from "react";

export default function PasswordRecoveryForm() {
  const [username, setUsername] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await fetch("/api/v2/members/users/password_reset_token", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username }),
      });
      if (!res.ok && res.status !== 204) {
        const msg = await res.text().catch(() => res.statusText);
        setError(msg || `Request failed (${res.status})`);
        return;
      }
      setSuccess(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Request failed");
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <div className="text-center space-y-3">
        <p className="text-4xl">📧</p>
        <h2 className="text-lg font-semibold text-gray-800">Check your email</h2>
        <p className="text-sm text-gray-500">If an account with that username exists, a password reset link has been sent.</p>
        <a href="/login" className="text-sm text-blue-600 hover:underline">Back to login</a>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
        <input
          required
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Enter your username"
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <button type="submit" disabled={loading}
        className="w-full py-2 bg-blue-600 text-white text-sm rounded-lg font-medium hover:bg-blue-700 disabled:opacity-40 transition-colors">
        {loading ? "Sending…" : "Send reset link"}
      </button>
      <p className="text-center text-xs text-gray-500">
        <a href="/login" className="text-blue-600 hover:underline">Back to login</a>
      </p>
    </form>
  );
}
