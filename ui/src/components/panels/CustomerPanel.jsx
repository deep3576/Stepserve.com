import { useState } from 'react';

export default function CustomerPanel({ api }) {
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState('');

  const loadBookings = async () => {
    setError('');
    try {
      const { data } = await api.get('/customer/bookings');
      setBookings(data);
    } catch {
      setError('Unable to load customer bookings. Add a customer token first.');
    }
  };

  return (
    <section className="panel">
      <h3>Customer view</h3>
      <button onClick={loadBookings}>Load My Bookings</button>
      {error ? <p className="error">{error}</p> : null}
      <ul className="list">
        {bookings.map((booking) => (
          <li key={booking.id}>{booking.service_title} · {booking.status} · ${booking.total_price}</li>
        ))}
      </ul>
    </section>
  );
}
