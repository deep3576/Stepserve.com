import { useState } from 'react';

export default function AdminPanel({ api }) {
  const [overview, setOverview] = useState(null);
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');

  const loadOverview = async () => {
    setError('');
    try {
      const [{ data: ov }, { data: us }] = await Promise.all([
        api.get('/admin/overview'),
        api.get('/admin/users')
      ]);
      setOverview(ov);
      setUsers(us);
    } catch {
      setError('Admin endpoints failed. Use admin token.');
    }
  };

  return (
    <section className="panel">
      <h3>Admin panel</h3>
      <button onClick={loadOverview}>Load Admin Data</button>
      {error ? <p className="error">{error}</p> : null}
      {overview ? (
        <div className="stats">
          <span>Users: {overview.users_count}</span>
          <span>Services: {overview.services_count}</span>
          <span>Bookings: {overview.bookings_count}</span>
          <span>Paid: ${overview.paid_total}</span>
        </div>
      ) : null}
      <ul className="list">
        {users.map((u) => (
          <li key={u.id}>{u.email} · {u.role} · {u.is_active ? 'active' : 'disabled'}</li>
        ))}
      </ul>
    </section>
  );
}
