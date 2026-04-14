import { CalendarCheck2, CreditCard, Search, ShieldCheck, Star, UserRound } from 'lucide-react';

const FEATURES = [
  { icon: Search, title: 'Smart service discovery', text: 'Search vetted providers by category and region.' },
  { icon: CalendarCheck2, title: 'Simple bookings', text: 'Pick slots and confirm quickly with transparent pricing.' },
  { icon: CreditCard, title: 'Stripe-ready payments', text: 'Built for secure CAD transactions and scalable checkout.' },
  { icon: ShieldCheck, title: 'Role-based security', text: 'Protected APIs for customer, provider, and admin flows.' },
  { icon: UserRound, title: 'Provider profiles', text: 'Professional profiles, availability, and service descriptions.' },
  { icon: Star, title: 'Trust via ratings', text: 'Post-service reviews to maintain marketplace quality.' }
];

export default function FeatureGrid() {
  return (
    <section className="feature-grid">
      {FEATURES.map(({ icon: Icon, title, text }) => (
        <article key={title} className="feature-card">
          <Icon size={20} />
          <h3>{title}</h3>
          <p>{text}</p>
        </article>
      ))}
    </section>
  );
}
