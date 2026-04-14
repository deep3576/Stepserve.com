import { useState } from 'react';

export default function HandymanPanel({ api }) {
  const [file, setFile] = useState(null);
  const [uploads, setUploads] = useState([]);
  const [message, setMessage] = useState('');

  const loadUploads = async () => {
    try {
      const { data } = await api.get('/providers/uploads');
      setUploads(data);
    } catch {
      setMessage('Could not fetch uploads.');
    }
  };

  const submitUpload = async (e) => {
    e.preventDefault();
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    try {
      await api.post('/providers/uploads', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setMessage('Upload successful.');
      await loadUploads();
    } catch {
      setMessage('Upload failed. Add provider token and profile first.');
    }
  };

  return (
    <section className="panel">
      <h3>Handyman upload panel</h3>
      <form className="grid-form" onSubmit={submitUpload}>
        <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        <button type="submit">Upload Asset</button>
      </form>
      {message ? <p className="hint">{message}</p> : null}
      <button onClick={loadUploads}>Refresh Uploads</button>
      <ul className="list">
        {uploads.map((u) => (
          <li key={u.id}>{u.file_name} · {u.file_size} bytes</li>
        ))}
      </ul>
    </section>
  );
}
