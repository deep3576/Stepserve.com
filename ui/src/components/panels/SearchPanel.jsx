import { useState } from 'react';

export default function SearchPanel({ api }) {
  const [filters, setFilters] = useState({ query: '', location: '', min_price: '', max_price: '' });
  const [results, setResults] = useState([]);
  const [error, setError] = useState('');

  const runSearch = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const { data } = await api.get('/search/services', {
        params: {
          query: filters.query || undefined,
          location: filters.location || undefined,
          min_price: filters.min_price || undefined,
          max_price: filters.max_price || undefined
        }
      });
      setResults(data);
    } catch {
      setError('Unable to fetch search results. Check backend connectivity.');
    }
  };

  return (
    <section className="panel">
      <h3>Search options</h3>
      <form className="grid-form" onSubmit={runSearch}>
        <input placeholder="Keyword" value={filters.query} onChange={(e) => setFilters({ ...filters, query: e.target.value })} />
        <input placeholder="Location" value={filters.location} onChange={(e) => setFilters({ ...filters, location: e.target.value })} />
        <input placeholder="Min price" value={filters.min_price} onChange={(e) => setFilters({ ...filters, min_price: e.target.value })} />
        <input placeholder="Max price" value={filters.max_price} onChange={(e) => setFilters({ ...filters, max_price: e.target.value })} />
        <button type="submit">Search Services</button>
      </form>
      {error ? <p className="error">{error}</p> : null}
      <ul className="list">
        {results.map((item) => (
          <li key={item.id}>{item.title} · {item.provider_name} · ${item.price}</li>
        ))}
      </ul>
    </section>
  );
}
