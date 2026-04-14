export default function NavTabs({ active, onChange }) {
  const tabs = [
    { key: 'search', label: 'Search' },
    { key: 'customer', label: 'Customer View' },
    { key: 'handyman', label: 'Handyman Panel' },
    { key: 'admin', label: 'Admin Panel' }
  ];

  return (
    <div className="tabs">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          className={active === tab.key ? 'tab active' : 'tab'}
          onClick={() => onChange(tab.key)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
